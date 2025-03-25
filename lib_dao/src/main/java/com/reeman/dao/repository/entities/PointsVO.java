package com.reeman.dao.repository.entities;

import java.io.Serializable;
import java.util.Objects;

public class PointsVO implements Serializable {
    private String point;

    private boolean isOpenWaitingTime;

    private int waitingTime;

    private String pointType;

    /**
     * 0:关闭顶升控制
     * 1:自动顶升抬起
     * 2:自动顶升放下
     * 3:手动顶升抬起
     * 4:手动顶升放下
     */
    private int liftAction;

    public PointsVO(String point, boolean isOpenWaitingTime, int waitingTime, String pointType, int liftAction) {
        this.point = point;
        this.isOpenWaitingTime = isOpenWaitingTime;
        this.waitingTime = waitingTime;
        this.pointType = pointType;
        this.liftAction = liftAction;
    }

    public PointsVO(PointsVO pointsVO) {
        this.point = pointsVO.point;
        this.isOpenWaitingTime = pointsVO.isOpenWaitingTime;
        this.waitingTime = pointsVO.waitingTime;
        this.pointType = pointsVO.pointType;
        this.liftAction = pointsVO.liftAction;
    }

    public PointsVO() {
    }

    public String getPoint() {
        return point;
    }

    public void setPoint(String point) {
        this.point = point;
    }

    public boolean isOpenWaitingTime() {
        return isOpenWaitingTime;
    }

    public void setOpenWaitingTime(boolean openWaitingTime) {
        isOpenWaitingTime = openWaitingTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public int getLiftAction() {
        return liftAction;
    }

    public void setLiftAction(int liftAction) {
        this.liftAction = liftAction;
    }
    public PointsVO getDefault(String name, String type) {
        return new PointsVO(name, true, 30, type, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointsVO pointsVO = (PointsVO) o;
        return isOpenWaitingTime == pointsVO.isOpenWaitingTime && waitingTime == pointsVO.waitingTime && liftAction == pointsVO.liftAction && point.equals(pointsVO.point) && pointType.equals(pointsVO.pointType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, isOpenWaitingTime, waitingTime, pointType, liftAction);
    }

    @Override
    public String toString() {
        return "PointsVO{" +
                "point='" + point + '\'' +
                ", isOpenWaitingTime=" + isOpenWaitingTime +
                ", waitingTime=" + waitingTime +
                ", pointType='" + pointType + '\'' +
                ", liftAction=" + liftAction +
                '}';
    }
}