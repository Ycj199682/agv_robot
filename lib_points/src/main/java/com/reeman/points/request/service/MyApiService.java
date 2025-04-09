package com.reeman.points.request.service;


import com.reeman.points.model.request.ApiResponse;
import com.reeman.points.model.request.MapInfo;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

// 定义 API 接口
public interface MyApiService {
    @GET("users/{user}")
    Call<MapInfo> getMapInfo(@Path("user") String user);

    @Multipart
    @POST("api/robot/reportPosition")
    Call<ApiResponse> reportPosition(
            @Part("robot_no") RequestBody robotNo,
            @Part("positions") RequestBody positions);


    /**
     * 提交机器人信息
     * @param data
     * @return
     */
    @Multipart
    @POST("api/robot/reportRobot")
    Call<ApiResponse> reportRobot(
            @Part("data") RequestBody data);

    /**
     * 修改订单的收款码
     * @param order_no
     * @param pay_account
     * @return
     */
    @Multipart
    @POST("api/robot/setPayAccount")
    Call<ApiResponse> setPayAccount(
            @Part("order_no") RequestBody order_no,
            @Part("pay_account") RequestBody pay_account);

    /**
     * 修改订单状态
     * @param order_no
     * @param status
     * @return
     */
    @Multipart
    @POST("api/robot/orderFinish")
    Call<ApiResponse> orderFinish(
            @Part("order_no") RequestBody order_no,
            @Part("status") RequestBody status);
}

