package com.reeman.commons.settings;

public class ReturningSetting{

    public float gotoProductionPointSpeed;

    public float gotoChargingPileSpeed;

    public boolean startTaskCountDownSwitch;

    public int startTaskCountDownTime;
    public boolean stopNearBy;

    /**
     * 0: 单个出品点
     * 1: 多个出品点
     */
    public int productionPointSetting;

    /**
     * 默认充电桩
     */
    public String defaultProductionPoint;

    public ReturningSetting(float gotoProductionPointSpeed, float gotoChargingPileSpeed, boolean startTaskCountDownSwitch, int startTaskCountDownTime, boolean stopNearBy, int productionPointSetting, String defaultProductionPoint) {
        this.gotoProductionPointSpeed = gotoProductionPointSpeed;
        this.gotoChargingPileSpeed = gotoChargingPileSpeed;
        this.startTaskCountDownSwitch = startTaskCountDownSwitch;
        this.startTaskCountDownTime = startTaskCountDownTime;
        this.stopNearBy = stopNearBy;
        this.productionPointSetting = productionPointSetting;
        this.defaultProductionPoint = defaultProductionPoint;
    }

    public static ReturningSetting getDefault(){
        return new ReturningSetting(
                0.4f,
                0.4f,
                false,
                5,
                true,
                0,
                ""
        );
    }

    @Override
    public String toString() {
        return "ReturningSetting{" +
                "gotoProductionPointSpeed=" + gotoProductionPointSpeed +
                ", gotoChargingPileSpeed=" + gotoChargingPileSpeed +
                ", startTaskCountDownSwitch=" + startTaskCountDownSwitch +
                ", startTaskCountDownTime=" + startTaskCountDownTime +
                ", stopNearBy=" + stopNearBy +
                ", productionPointSetting=" + productionPointSetting +
                ", defaultProductionPoint='" + defaultProductionPoint + '\'' +
                '}';
    }
}
