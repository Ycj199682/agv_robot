package com.reeman.agv.request;


import android.content.Context;

import com.reeman.agv.plugins.RetrofitClient;
import com.reeman.agv.request.service.RobotService;
import com.reeman.points.request.MyRetrofitClient;
import com.reeman.points.request.service.MyApiService;

public class ServiceFactory {
    private static RobotService robotService;
    private static MyApiService apiService;

    public static RobotService getRobotService() {
        if (robotService == null) {
            robotService = RetrofitClient.getClient().create(RobotService.class);
        }
        return robotService;
    }

    public static MyApiService getApiService(Context context) {
        if (apiService == null) {
            apiService = MyRetrofitClient.getInstance(context).create(MyApiService.class);
        }
        return apiService;
    }
}
