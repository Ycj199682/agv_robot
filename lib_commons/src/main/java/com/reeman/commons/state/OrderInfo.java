package com.reeman.commons.state;

public class OrderInfo {
    private static OrderInfo instance;

    public static OrderInfo getInstance(){
        if (instance == null) {
            synchronized (OrderInfo.class) {
                if (instance == null) {
                    instance = new OrderInfo();
                }
            }
        }
        return instance;
    }

    String orderNo;
    String payAccount;


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
}
