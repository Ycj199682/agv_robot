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



    @Multipart
    @POST("api/robot/reportRobot")
    Call<ApiResponse> reportRobot(
            @Part("data") RequestBody data);




    @Multipart
    @POST("api/robot/reportPosition")
    Call<ApiResponse> updateAlipay(
            @Part("robot_no") RequestBody id,
            @Part("positions") RequestBody aliNumber);
}

