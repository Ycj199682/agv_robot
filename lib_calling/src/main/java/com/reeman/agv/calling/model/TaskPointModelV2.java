package com.reeman.agv.calling.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class TaskPointModelV2 extends TaskPointModel{
    @SerializedName("order_no")
    public String orderNo;

    @SerializedName("pay_account")
    public String payAccount;

    public TaskPointModelV2(@Nullable String map, @NonNull String point) {
        super(map, point);
    }

    public TaskPointModelV2(@Nullable String map, @NonNull String point, String orderNo, String payAccount) {
        super(map, point);
        this.orderNo = orderNo;
        this.payAccount = payAccount;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getPayAccount() {
        return payAccount;
    }

    public void setPayAccount(String payAccount) {
        this.payAccount = payAccount;
    }

    @Override
    public String toString() {
        return "TaskPointModelV2{" +
                "map='" + getMap() + '\'' +
                ", point='" + getPoint() + '\'' +
                ", orderNo='" + getOrderNo() + '\'' +
                ", payAccount='" + getPayAccount() + '\'' +
                '}';
    }
}
