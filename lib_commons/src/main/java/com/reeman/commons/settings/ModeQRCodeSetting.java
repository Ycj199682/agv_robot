package com.reeman.commons.settings;


import java.util.Arrays;

public class ModeQRCodeSetting extends BaseModeSetting {

    public float length;

    public float width;

    /**
     * 0:返回出品点
     * 1:返回充电桩
     * 2:原地停留
     */
    public int finishAction;

    /**
     * 机身尺寸
     */
    public float[] robotSize;

    //机器从二维码离开的方向和距离,导航默认值0.7
    public float orientationAndDistanceCalibration;

    //机器到达二维码后开始导航时的朝向,默认 false 倒退进入
    public boolean direction;

    //机器在二维码模式是否打开恢复模式,默认打开
    public boolean rotate;

    //二维码模式下抬起物品后修改激光宽度
    public float lidarWidth;

    //是否打开就近停靠
    public boolean stopNearby;
    //是否打开呼叫按键绑定
    public boolean callingBind;
    //是否打开顶升控制
    public boolean lift;

    public ModeQRCodeSetting(float speed, boolean startTaskCountDownSwitch,int finishAction, int startTaskCountDownTime, float length, float width, float[] robotSize, float orientationAndDistanceCalibration, boolean direction, boolean rotate, float lidarWidth, boolean stopNearby, boolean callingBind, boolean lift) {
        super(speed, startTaskCountDownSwitch, startTaskCountDownTime);
        this.length = length;
        this.width = width;
        this.finishAction = finishAction;
        this.robotSize = robotSize;
        this.orientationAndDistanceCalibration = orientationAndDistanceCalibration;
        this.direction = direction;
        this.rotate = rotate;
        this.lidarWidth = lidarWidth;
        this.stopNearby = stopNearby;
        this.callingBind = callingBind;
        this.lift = lift;
    }

    public static ModeQRCodeSetting getDefault(){
        return new ModeQRCodeSetting(
                0.4f,
                false,
                0,
                5,
                0,
                0,
                new float[]{0,0,0,0},
                0.7f,
                false,
                true,
                0.3f,
                false,
                false,
                true
        );
    }

    @Override
    public String toString() {
        return "ModeQRCodeSetting{" +
                "length=" + length +
                ", width=" + width +
                ", finishAction=" + finishAction +
                ", robotSize=" + Arrays.toString(robotSize) +
                ", orientationAndDistanceCalibration=" + orientationAndDistanceCalibration +
                ", direction=" + direction +
                ", rotate=" + rotate +
                ", lidarWidth=" + lidarWidth +
                ", stopNearby=" + stopNearby +
                ", callingBind=" + callingBind +
                ", lift=" + lift +
                ", speed=" + speed +
                ", startTaskCountDownSwitch=" + startTaskCountDownSwitch +
                ", startTaskCountDownTime=" + startTaskCountDownTime +
                '}';
    }
}
