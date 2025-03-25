package com.reeman.commons.settings;


public class DoorControlSetting {

    public boolean open;

    /**
     * 通讯方式:
     * 0:2.4GHz
     * 1:LoRa
     */
    public int communicationMethod;

    /**
     * 关门逻辑
     * 0:直接开始导航
     * 1:等待关门完成再开始导航
     */
    public int closeDoorAction;

    public int waitingTime;

    public DoorControlSetting(boolean open, int communicationMethod, int closeDoorAction, int waitingTime) {
        this.open = open;
        this.communicationMethod = communicationMethod;
        this.closeDoorAction = closeDoorAction;
        this.waitingTime = waitingTime;
    }

    public static DoorControlSetting getDefault() {
        return new DoorControlSetting(false, 0,1, 30);
    }

    @Override
    public String toString() {
        String closeDoorAction = "";
        if (this.closeDoorAction == 0) {
            closeDoorAction = "直接开始导航";
        } else if (this.closeDoorAction == 1) {
            closeDoorAction = "等待关门完成再开始导航";
        }
        String communicationMethod = "";
        if (this.communicationMethod == 0){
            communicationMethod = "2.4GHz";
        }else {
            communicationMethod = "LoRa";
        }
        return "门控设置{" +
                "打开=" + open +
                ", 通讯方式="+communicationMethod+
                ", 关门逻辑=" + closeDoorAction +
                ", 关门后停留时长=" +
                '}';
    }
}
