package com.reeman.agv.calling.button

import android.os.Build
import android.util.Log
import com.reeman.agv.calling.CallingInfo
import com.reeman.agv.calling.event.CallingButtonEvent
import com.reeman.agv.calling.event.QRCodeButtonEvent
import com.reeman.agv.calling.event.UnboundButtonEvent
import com.reeman.agv.calling.model.QRCodeModeTaskModel
import com.reeman.agv.calling.utils.CallingStateManager
import com.reeman.commons.event.CallingModelDisconnectedEvent
import com.reeman.commons.event.CallingModelReconnectSuccessEvent
import com.reeman.commons.event.CloseDoorFailedEvent
import com.reeman.commons.event.DoorClosedEvent
import com.reeman.commons.event.DoorNumSettingResultEvent
import com.reeman.commons.event.DoorOpenedEvent
import com.reeman.commons.event.OpenDoorFailedEvent
import com.reeman.commons.event.SetDoorNumFailedEvent
import com.reeman.commons.eventbus.EventBus
import com.reeman.commons.provider.SerialPortProvider
import com.reeman.commons.state.RobotInfo
import com.reeman.commons.utils.ByteUtil
import com.reeman.commons.utils.UsbEventCallback
import com.reeman.commons.utils.UsbFileObserver
import com.reeman.serialport.controller.SerialPortParser
import com.reeman.serialport.util.Parser
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object CallingHelper {
    private var parser: SerialPortParser? = null
    private var start = false
    private val mPattern = Pattern.compile("AA55|AA56")
    const val INIT = 0
    const val CLOSED = 1
    const val OPENED = 2
    const val WAITING_OPEN_RESULT = 3
    const val WAITING_CLOSE_RESULT = 4
    var doorState = INIT
    private var opposite = false
    private var isDoorControlTest = false
    private var currentOpeningDoor = ""
    private var currentClosingDoor = ""
    private var openDoorDisposable: Disposable? = null
    private var closeDoorDisposable: Disposable? = null
    private var usbFileObserver: UsbFileObserver? = null
    private var checkDeviceDisposable: Disposable? = null


    fun isStart() = start

    @Throws(Exception::class)
    fun start() {
        val path = SerialPortProvider.ofCallModule(Build.PRODUCT)
        val file = File(path)
        val files = file.listFiles()
        if (!file.exists() || files.isNullOrEmpty()) {
            throw FileNotFoundException()
        }
        val target =
            files.firstOrNull { it.name.startsWith("ttyUSB") } ?: throw FileNotFoundException()

        parser = SerialPortParser(
            File("/dev/${target.name}"),
            115200,
            object : SerialPortParser.OnDataResultListener {
                private val sb = StringBuilder()

                override fun onDataResult(bytes: ByteArray, len: Int) {

                    /**
                     * 门控事件
                     */
                    fun detailDoorControlEvent(data: String) {
                        if (data.length == 12) {
                            val subDoorNum: String = data.substring(4, 12)
                            var doorNum = ""
                            for (i in 0..3) {
                                doorNum += subDoorNum.substring(i * 2, i * 2 + 2).toInt(16)
                            }
                            if (data.startsWith("02", 2)) {
                                if (opposite) {
                                    Timber.w("控制盒收到关门指令: $doorNum")
                                    if (doorNum == currentClosingDoor) {
                                        cancelCloseDoorRetry()
                                        EventBus.sendEvent(DoorClosedEvent(doorNum.toInt()))
                                    }
                                } else {
                                    Timber.w("控制盒收到开门指令: $doorNum")
                                    if (doorNum == currentOpeningDoor) {
                                        cancelOpenDoorRetry()
                                        EventBus.sendEvent(DoorOpenedEvent(doorNum.toInt()))
                                    }
                                }
                            } else if (data.startsWith("03", 2)) {
                                Timber.w("控制盒开门成功: $doorNum")

                            } else if (data.startsWith("05", 2)) {
                                if (opposite) {
                                    Timber.w("控制盒收到开门指令: $doorNum")
                                    if (doorNum == currentOpeningDoor) {
                                        cancelOpenDoorRetry()
                                        EventBus.sendEvent(DoorOpenedEvent(doorNum.toInt()))
                                    }
                                } else {
                                    Timber.w("控制盒收到关门指令: $doorNum")
                                    if (doorNum == currentClosingDoor) {
                                        cancelCloseDoorRetry()
                                        EventBus.sendEvent(DoorClosedEvent(doorNum.toInt()))
                                    }
                                }
                            } else if (data.startsWith("06", 2)) {
                                Timber.w("控制盒关门成功")

                            } else if (data.startsWith("08", 2)) {
                                Timber.w("设置标签编号成功: $doorNum")
                                EventBus.sendEvent(DoorNumSettingResultEvent(doorNum.toInt()))
                            }
                        }
                    }

                    /**
                     * 呼叫事件
                     */
                    fun detailCallingEvent(key: String) {
                        if (RobotInfo.isElevatorMode) {
                            if (CallingInfo.callingButtonMapWithElevator.containsKey(key)) {
                                val pair = CallingInfo.callingButtonMapWithElevator[key]
                                Timber.w("收到呼叫,key: $key, taskPoint: $pair")
                                pair?.let { mPair ->
                                    CallingStateManager.setCallingButtonEvent(
                                        CallingButtonEvent(
                                            key,
                                            mPair.first,
                                            mPair.second
                                        )
                                    )
                                }
                            } else {
                                Timber.d("unbound key : $key")
                                CallingStateManager.setUnboundButtonEvent(
                                    UnboundButtonEvent(key)
                                )
                            }
                        } else {
                            if (CallingInfo.isQRCodeTaskUseCallingButton && CallingInfo.callingButtonWithQRCodeModelTaskMap.containsKey(
                                    key
                                )
                            ) {
                                val qrCodeModelTask =
                                    CallingInfo.callingButtonWithQRCodeModelTaskMap[key]
                                Timber.w("收到呼叫,key: $key, taskPoint: $qrCodeModelTask")
                                qrCodeModelTask?.let { mQRCodeModelTask ->
                                    CallingStateManager.setQRCodeButtonEvent(
                                        QRCodeButtonEvent(
                                            key,
                                            mQRCodeModelTask.map {
                                                Pair(
                                                    QRCodeModeTaskModel(
                                                        it.first.first,
                                                        it.first.second
                                                    ),
                                                    QRCodeModeTaskModel(
                                                        it.second.first,
                                                        it.second.second
                                                    )
                                                )
                                            })
                                    )
                                }
                            } else if (CallingInfo.callingButtonMap.containsKey(key)) {
                                val point = CallingInfo.callingButtonMap[key]
                                Timber.w("收到呼叫,key: $key, taskPoint: $point")
                                point?.let { mPoint ->
                                    CallingStateManager.setCallingButtonEvent(
                                        CallingButtonEvent(key, "", mPoint)
                                    )
                                }
                            } else {
                                Timber.d("unbound key : $key")
                                CallingStateManager.setUnboundButtonEvent(
                                    UnboundButtonEvent(key)
                                )
                            }
                        }

                    }
                    sb.append(ByteUtil.byteArr2HexString(bytes, len))
                    Log.w("receive", sb.toString())

                    while (sb.isNotEmpty()) {
                        if (sb.length < 4) return
                        val matcher = mPattern.matcher(sb)
                        if (matcher.find()) {
                            try {
                                val start = matcher.start()
                                val startIndex = start + 4
                                val header = sb.substring(start, startIndex)

                                if (startIndex + 2 >= sb.length) break

                                val dataSize = sb.substring(startIndex, startIndex + 2)
                                val intSize = dataSize.toInt(16)

                                val dataLastIndex = startIndex + intSize * 2 + 2

                                if (dataLastIndex + 2 > sb.length) break

                                val dataHexSum = sb.substring(startIndex, dataLastIndex)
                                val checkSum = sb.substring(dataLastIndex, dataLastIndex + 2)

                                if (checkSum == Parser.checkXor(dataHexSum)) {
                                    val data = sb.substring(startIndex + 2, dataLastIndex)
                                    Observable.just(Pair(header, data))
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe({
                                            when (it.first) {
                                                "AA55" -> detailCallingEvent(data.substring(2))

                                                else -> detailDoorControlEvent(data)
                                            }
                                        }
                                        ) { throwable ->
                                            Timber.d(
                                                throwable,
                                                "detail data failed"
                                            )
                                        }

                                    sb.delete(0, dataLastIndex + 2)
                                } else if (matcher.find()) {
                                    Timber.w("数据包校验不通过1 $sb")
                                    sb.delete(0, matcher.start())
                                } else {
                                    Timber.w("数据包校验不通过2 $sb")
                                    sb.clear()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Timber.w(e, "数据包校验不通过3 $sb")
                                sb.clear()
                            }
                        } else {
                            sb.clear()
                        }
                    }
                }
            })
        parser?.start()
        start = true
        usbFileObserver = UsbFileObserver("/dev/${target.name}", object : UsbEventCallback {
            override fun onUsbDeviceDetached(path: String?) {
                Timber.w("呼叫串口断开")
                EventBus.sendEvent(CallingModelDisconnectedEvent(0))
                stop()
                cancelOpenDoorRetry()
                cancelCloseDoorRetry()
                checkDevice()
            }
        })
        usbFileObserver?.startWatching()
    }

    private fun checkDevice() {
        checkDeviceDisposable = Observable.interval(2, 1, TimeUnit.SECONDS)
            .take(10)
            .map { count ->
                Pair(
                    count,
                    File(SerialPortProvider.ofCallModule(Build.PRODUCT)).exists()
                )
            }
            .observeOn(Schedulers.io())
            .subscribe({
                Log.w("---", "呼叫模块重连检测 : $it")
                if (checkDeviceDisposable?.isDisposed == true) return@subscribe
                if (it.second) {
                    checkDeviceDisposable?.dispose()
                    Timber.w("等待门控串口重连成功")
                    start()
                    EventBus.sendEvent(CallingModelReconnectSuccessEvent())
                    if (doorState == WAITING_OPEN_RESULT) {
                        if (isDoorControlTest) {
                            openDoor(currentOpeningDoor)
                        } else {
                            openDoor(currentOpeningDoor, opposite)
                        }
                    } else if (doorState == WAITING_CLOSE_RESULT) {
                        if (isDoorControlTest) {
                            closeDoor(currentClosingDoor)
                        } else {
                            closeDoor(currentClosingDoor, opposite)
                        }
                    }
                }else if(it.first == 9L){
                    EventBus.sendEvent(CallingModelDisconnectedEvent(1))
                }
            },
                {
                    if (checkDeviceDisposable?.isDisposed == true) return@subscribe
                    Timber.w(it, "呼叫模块重连失败")
                    EventBus.sendEvent(CallingModelDisconnectedEvent(1))
                }
            )
    }

    fun stop() {
        parser?.stop()
        start = false
        usbFileObserver?.stopWatching()
        usbFileObserver = null
        checkDeviceDisposable?.apply {
            if (!this.isDisposed) {
                this.dispose()
            }
        }
    }


    fun openDoor(number: String, opposite: Boolean) {
        if (number.isBlank()) return
        isDoorControlTest = false
        this.opposite = opposite
        this.currentOpeningDoor = number
        this.doorState = WAITING_OPEN_RESULT
        val doorNum = ByteUtil.strToDoorNum(number)
        val bytes = byteArrayOf(
            0x00,
            0x05,
            0x05,
            0xAA.toByte(),
            0x56,
            0x06,
            0x01,
            if (opposite) 0x04 else 0x01,
            doorNum[0],
            doorNum[1],
            doorNum[2],
            doorNum[3],
            0x00
        )

        openDoorDisposable = Observable.interval( 1, TimeUnit.SECONDS)
            .take(10)
            .map {
                if(it == 9L){
                    Timber.w("发送开门指令失败")
                    EventBus.sendEvent(OpenDoorFailedEvent())
                }else{
                    Timber.w("发送开门指令")
                    parser?.sendCommand(checksum(bytes))
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({},{
                Timber.w(it,"发送开门指令失败")
                EventBus.sendEvent(OpenDoorFailedEvent())
            })

    }


    fun openDoor(number: String):Boolean {
        if (number.isBlank()) return false
        isDoorControlTest = true
        this.currentOpeningDoor = number
        this.doorState = WAITING_OPEN_RESULT
        val doorNum = ByteUtil.strToDoorNum(number)
        val bytes = byteArrayOf(
            0x00,
            0x05,
            0x05,
            0xAA.toByte(),
            0x56,
            0x06,
            0x01,
            0x01,
            doorNum[0],
            doorNum[1],
            doorNum[2],
            doorNum[3],
            0x00
        )
        return try {
            parser!!.sendCommand(checksum(bytes))
            true
        } catch (e: Exception) {
            Timber.w(e, "发送开门指令失败")
            doorState = INIT
            EventBus.sendEvent(OpenDoorFailedEvent())
            false
        }
    }

    fun closeDoor(number: String, opposite: Boolean) {
        if (number.isBlank()) return
        isDoorControlTest = false
        this.opposite = opposite
        this.currentClosingDoor = number
        this.doorState = WAITING_CLOSE_RESULT
        val doorNum = ByteUtil.strToDoorNum(number)
        val bytes = byteArrayOf(
            0x00,
            0x05,
            0x05,
            0xAA.toByte(),
            0x56,
            0x06,
            0x01,
            if (opposite) 0x01 else 0x04,
            doorNum[0],
            doorNum[1],
            doorNum[2],
            doorNum[3],
            0x00
        )
        closeDoorDisposable = Observable.interval( 1, TimeUnit.SECONDS)
            .take(10)
            .map {
                if(it == 9L){
                    Timber.w("发送关门指令失败")
                    EventBus.sendEvent(CloseDoorFailedEvent())
                }else{
                    Timber.w("发送关门指令")
                    parser?.sendCommand(checksum(bytes))
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({},{
                Timber.w(it,"发送关门指令失败")
                EventBus.sendEvent(CloseDoorFailedEvent())
            })
    }

    fun closeDoor(number: String):Boolean {
        if (number.isBlank()) return false
        isDoorControlTest = true
        this.currentClosingDoor = number
        this.doorState = WAITING_CLOSE_RESULT
        val doorNum = ByteUtil.strToDoorNum(number)
        val bytes = byteArrayOf(
            0x00,
            0x05,
            0x05,
            0xAA.toByte(),
            0x56,
            0x06,
            0x01,
            0x04,
            doorNum[0],
            doorNum[1],
            doorNum[2],
            doorNum[3],
            0x00
        )
        return try {
            parser!!.sendCommand(checksum(bytes))
            true
        } catch (e: Exception) {
            Timber.w(e, "发送关门指令失败")
            doorState = INIT
            EventBus.sendEvent(CloseDoorFailedEvent())
            false
        }
    }

    fun setDoorNum(number: String) {
        if (number.isBlank()) return
        val doorNum = ByteUtil.strToDoorNum(number)
        val bytes = byteArrayOf(
            0x00,
            0x05,
            0x05,
            0xAA.toByte(),
            0x56,
            0x06,
            0x01,
            0x07,
            doorNum[0],
            doorNum[1],
            doorNum[2],
            doorNum[3],
            0x00
        )
        try {
            parser!!.sendCommand(checksum(bytes))
        } catch (e: Exception) {
            Timber.w(e, "发送设置门编号指令失败")
            EventBus.sendEvent(SetDoorNumFailedEvent())
        }
    }

    private fun checksum(msg: ByteArray): ByteArray {
        var mSum: Byte = 0
        for (i in 5 until msg.size - 1) {
            mSum = (mSum.toInt() xor msg[i].toInt()).toByte()
        }
        msg[msg.size - 1] = mSum
        return msg
    }

    fun cancelOpenDoorRetry() {
        openDoorDisposable?.apply {
            if (!this.isDisposed){
                this.dispose()
            }
        }
    }

    fun cancelCloseDoorRetry() {
        closeDoorDisposable?.apply {
            if (!this.isDisposed){
                this.dispose()
            }
        }
    }
}
