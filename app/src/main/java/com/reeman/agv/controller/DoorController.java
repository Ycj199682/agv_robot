package com.reeman.agv.controller;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


import com.aill.androidserialport.SerialPort;
import com.reeman.commons.exceptions.ReconnectUsbDeviceTimeoutException;
import com.reeman.commons.provider.SerialPortProvider;
import com.reeman.commons.utils.ByteUtil;
import com.reeman.commons.utils.UsbFileObserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Pair;
import timber.log.Timber;


public class DoorController {
    private static volatile DoorController instance;
    private boolean opposite = false;
    private volatile State currentState = State.CLOSED;
    private StringBuilder stringBuilder;
    private InputStream inputStream;
    private OutputStream outputStream;
    private volatile boolean stopped = false;
    private ExecutorService service;
    private String currentClosingDoor;

    private String currentOpeningDoor;

    private Disposable checkDeviceDisposable;

    private UsbFileObserver usbFileObserver;


    public enum State {
        CLOSED, //关闭状态
        WAITING_OPEN, //等待门开
        WAITING_CLOSE,//等待门关
        OPENED //开启状态
    }

    public boolean isOpposite() {
        return opposite;
    }

    public void setOpposite(boolean opposite) {
        this.opposite = opposite;
    }


    public boolean isOpened() {
        return currentState == State.OPENED;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public State getCurrentState() {
        return currentState;
    }

    private DoorController() {
    }

    public static DoorController createInstance() {
        if (instance == null) {
            synchronized (DoorController.class) {
                if (instance == null) {
                    instance = new DoorController();
                }
            }
        }
        return instance;
    }

    public static DoorController getInstance() {
        return instance;
    }

    public void init(OnAccessControlListener listener) throws Exception {
        stringBuilder = new StringBuilder();
        this.listener = listener;
        startSerialPort(SerialPortProvider.ofDoorControl(Build.PRODUCT));
    }

    private void startSerialPort(String port) throws Exception {
        File file = new File(port);
        if (!file.exists()) throw new FileNotFoundException();
        File targetFile = null;
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File tempFile : files) {
                    if (tempFile.getName().startsWith("ttyUSB")) {
                        targetFile = tempFile;
                        break;
                    }
                }
            }
        }
        if (targetFile == null) throw new FileNotFoundException();
        SerialPort serialPort = new SerialPort(new File("/dev/" + targetFile.getName()), 9600, 0);
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
        service = Executors.newSingleThreadExecutor(r -> new Thread(r, "thread-serial-port-helper"));
        byte[] buffer = new byte[1024];
        stopped = false;
        service.submit(() -> {
            while (!stopped) {
                if (inputStream == null) return;
                try {
                    int available = inputStream.available();
                    if (available <= 0) {
                        Thread.sleep(8);
                        continue;
                    }

                    int size = inputStream.read(buffer);
                    if (size > 0) {
                        if (listener == null) return;
                        String str = byte2HexString(buffer, size);

                        stringBuilder.append(str);

                        Timber.w("收到指令:" + str + " 当前指令: " + stringBuilder.toString() + " length: " + stringBuilder.length());

                        if (stringBuilder.length() < 4) continue;

                        if (!stringBuilder.toString().contains("574a")) {
                            stringBuilder.delete(0, stringBuilder.length());
                            continue;
                        } else if (!stringBuilder.toString().startsWith("574a")) {
                            stringBuilder.delete(0, stringBuilder.indexOf("574a"));
                        }

                        while (stringBuilder.length() >= 22) {
                            String response = stringBuilder.toString();
                            String subDoorNum = response.substring(10, 18);
                            String s = "";
                            for (int i = 0; i < 4; i++) {
                                s += Integer.parseInt(subDoorNum.substring(i * 2, i * 2 + 2), 16);
                            }
                            if (response.startsWith("574a090301") && stringBuilder.substring(18, 20).equals("02")) {
                                Timber.w("控制盒收到开门指令");
                            } else if (response.startsWith("574a090302") && stringBuilder.substring(18, 20).equals("02")) {
                                Timber.w("控制盒收到关门指令");
                            } else if (response.startsWith("574a090201") && stringBuilder.substring(18, 20).equals("01")) {
                                Timber.w("标签收到开门指令");
                                if (opposite) {
                                    if (Objects.equals(currentClosingDoor, s)) {
                                        listener.onCloseDoorSuccess("N-" + currentClosingDoor);
                                    }
                                } else {
                                    if (Objects.equals(currentOpeningDoor, s)) {
                                        listener.onOpenDoorSuccess();
                                    }
                                }
                            } else if (response.startsWith("574a090202") && stringBuilder.substring(18, 20).equals("01")) {
                                Timber.w("标签收到关门指令");
                                if (opposite) {
                                    if (Objects.equals(currentOpeningDoor, s)) {
                                        listener.onOpenDoorSuccess();
                                    }
                                } else {
                                    if (Objects.equals(currentClosingDoor, s)) {
                                        listener.onCloseDoorSuccess(currentClosingDoor);
                                    }
                                }
                            }
                            stringBuilder.delete(0, Math.min(22, stringBuilder.length()));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (stopped) break;
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (service != null) {
                service.shutdown();
            }
        });
        usbFileObserver = new UsbFileObserver("/dev/" + targetFile.getName(), path -> {
            Timber.w("门控串口断开");
            service.shutdown();
            stopped = true;
            usbFileObserver.stopWatching();
            usbFileObserver = null;
            checkDevice(SerialPortProvider.ofDoorControl(Build.PRODUCT));
        });
        usbFileObserver.startWatching();
    }

    private void checkDevice(String path) {
        checkDeviceDisposable = Observable.interval(2, 1, TimeUnit.SECONDS)
                .take(10)
                .map(count -> new Pair<>(count, new File(path).exists()))
                .observeOn(Schedulers.io())
                .subscribe(
                        result -> {
                            Log.w("---", "门控模块重连检测 : " + result);
                            if (checkDeviceDisposable.isDisposed()) return;
                            if (result.getSecond()) {
                                checkDeviceDisposable.dispose();
                                Timber.w("等待门控串口重连成功");
                                startSerialPort(SerialPortProvider.ofDoorControl(Build.PRODUCT));
                                if (currentState == State.WAITING_OPEN) {
                                    openDoor(currentOpeningDoor, opposite);
                                } else if (currentState == State.WAITING_CLOSE) {
                                    closeDoor(currentClosingDoor, opposite);
                                }
                            }else if (result.getFirst() == 9){
                                listener.onThrowable(true, new ReconnectUsbDeviceTimeoutException());
                            }
                        }, throwable -> {
                            if (checkDeviceDisposable.isDisposed()) return;
                            Timber.w(throwable, "等待门控重连失败");
                            listener.onThrowable(true, new ReconnectUsbDeviceTimeoutException());
                        });
    }

    private String byte2HexString(byte[] bytes, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            String s = Integer.toHexString(bytes[i]);
            if (s.length() == 1) {
                sb.append(0).append(s);
            } else {
                sb.append(s);
            }
        }
        return sb.toString();
    }

    public void unInit() {
        if (checkDeviceDisposable != null && !checkDeviceDisposable.isDisposed()) {
            checkDeviceDisposable.dispose();
        }
        if (usbFileObserver != null){
            usbFileObserver.stopWatching();
            usbFileObserver = null;
        }
        stopped = true;
        currentState = State.CLOSED;
        listener = null;
        instance = null;
        stringBuilder = null;

    }

    public boolean openDoor(String number, boolean opposite) {
        if (TextUtils.isEmpty(number)) return false;
        this.opposite = opposite;
        this.currentOpeningDoor = number;
        try {
            byte[] doorNum = ByteUtil.strToDoorNum(number);
            byte[] bytes;
            if (opposite) {
                bytes = new byte[]{0x57, 0x4a, 0x09, 0x01, 0x02, doorNum[0], doorNum[1], doorNum[2], doorNum[3], 0x00, 0x00};
            } else {
                bytes = new byte[]{0x57, 0x4a, 0x09, 0x01, 0x01, doorNum[0], doorNum[1], doorNum[2], doorNum[3], 0x00, 0x00};
            }
            currentState = State.WAITING_OPEN;
            String command = toHexString(sumCheck(bytes));
            this.outputStream.write(ByteUtil.hexStringToBytes(command));
            Timber.w("access 调用开门方法，门编号：" + number + " 门反方向：" + opposite + " 发送命令：" + command);
            return true;
        } catch (Exception e) {
            Timber.w("发送命令失败");
            if (checkDeviceDisposable != null && !checkDeviceDisposable.isDisposed()) return false;
            if (listener != null) listener.onThrowable(true, e);
            return false;
        }
    }

    public boolean closeDoor(String number, boolean opposite) {
        if (TextUtils.isEmpty(number)) return false;
        this.opposite = opposite;
        this.currentClosingDoor = number;
        try {
            byte[] bytes = ByteUtil.strToDoorNum(number);
            byte[] cmd;
            if (opposite) {
                cmd = new byte[]{0x57, 0x4a, 0x09, 0x01, 0x01, bytes[0], bytes[1], bytes[2], bytes[3], 0x00, 0x00};
            } else {
                cmd = new byte[]{0x57, 0x4a, 0x09, 0x01, 0x02, bytes[0], bytes[1], bytes[2], bytes[3], 0x00, 0x00};
            }
            currentState = State.WAITING_CLOSE;
            String command = toHexString(sumCheck(cmd));
            this.outputStream.write(ByteUtil.hexStringToBytes(command));
            Timber.w("access 调用关门方法，门编号：" + number + " 门方向：" + opposite + " 发送命令：" + command);
            return true;
        } catch (Exception e) {
            Timber.w(e, "发送命令失败");
            if (checkDeviceDisposable != null && !checkDeviceDisposable.isDisposed()) return false;
            if (listener != null) listener.onThrowable(false, e);
            return false;
        }
    }

    public String toHexString(byte[] byteArray) {
        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteArray.length; i++) {
            int value = byteArray[i] & 0xFF;
            if (value < 0x10) {
                hexString.append("0");
            }
            hexString.append(Integer.toHexString(value));
        }
        return hexString.toString().toLowerCase();
    }

    private byte[] sumCheck(byte[] msg) {
        long mSum = 0;
        for (int i = 2; i < msg.length - 1; i++) {
            long mNum = ((long) msg[i] >= 0) ? (long) msg[i] : ((long) msg[i] + 256);
            mSum += mNum;
        }
        msg[msg.length - 1] = (byte) (mSum >> (0) & 0xff);
        return msg;
    }

    private OnAccessControlListener listener;

    public interface OnAccessControlListener {

        void onOpenDoorSuccess();

        void onCloseDoorSuccess(String currentClosingDoor);

        void onThrowable(boolean isOpenDoor, Throwable throwable);
    }
}
