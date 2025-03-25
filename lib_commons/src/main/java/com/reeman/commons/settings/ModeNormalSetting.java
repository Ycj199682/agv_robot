package com.reeman.commons.settings;

public class ModeNormalSetting extends BaseModeSetting {

    public boolean waitingCountDownTimerOpen;

    public int waitingTime;

    public int playArrivedTipTime;

    /**
     * 0:返回出品点
     * 1:返回充电桩
     * 2:原地停留
     */
    public int finishAction;

    /**
     * 手动顶升控制开关
     */
    public boolean manualLiftControlOpen;

    /**
     * 机器长度
     */
    public float length;

    /**
     * 机器宽度
     */
    public float width;

    /**
     * 机身尺寸
     */
    public float[] robotSize;

    /**
     * 激光宽度
     */
    public float lidarWidth;

    /**
     * 恢复模式
     */
    public boolean rotate;

    /**
     * 就近停靠
     */
    public boolean stopNearBy;

    public ModeNormalSetting(float speed, boolean startTaskCountDownSwitch, int startTaskCountDownTime, boolean waitingCountDownTimerOpen, int waitingTime, int playArrivedTipTime, int finishAction, boolean manualLiftControlOpen, float length, float width, float[] robotSize, float lidarWidth, boolean rotate, boolean stopNearBy) {
        super(speed, startTaskCountDownSwitch, startTaskCountDownTime);
        this.waitingCountDownTimerOpen = waitingCountDownTimerOpen;
        this.waitingTime = waitingTime;
        this.playArrivedTipTime = playArrivedTipTime;
        this.finishAction = finishAction;
        this.manualLiftControlOpen = manualLiftControlOpen;
        this.length = length;
        this.width = width;
        this.robotSize = robotSize;
        this.lidarWidth = lidarWidth;
        this.rotate = rotate;
        this.stopNearBy = stopNearBy;
    }

    public static ModeNormalSetting getDefault() {
        return new ModeNormalSetting(
                0.4f,
                false,
                5,
                true,
                30,
                20,
                0,
                false,
                0,
                0,
                new float[]{0, 0, 0, 0},
                0.3f,
                true,
                false
        );
    }

    @Override
    public String toString() {
        return "ModeNormalSetting{" +
                "waitingCountDownTimerOpen=" + waitingCountDownTimerOpen +
                ", waitingTime=" + waitingTime +
                ", finishAction=" + finishAction +
                ", manualLiftControlOpen=" + manualLiftControlOpen +
                ", length=" + length +
                ", width=" + width +
                ", lidarWidth=" + lidarWidth +
                ", rotate=" + rotate +
                ", stopNearBy=" + stopNearBy +
                ", speed=" + speed +
                ", startTaskCountDownSwitch=" + startTaskCountDownSwitch +
                ", startTaskCountDownTime=" + startTaskCountDownTime +
                '}';
    }
}
