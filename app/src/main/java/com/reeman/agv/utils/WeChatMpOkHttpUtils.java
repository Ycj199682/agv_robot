package com.reeman.agv.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众号接口工具类 (基于OkHttp)
 */
public class WeChatMpOkHttpUtils {

    private final OkHttpClient httpClient;
    private final String appId = "wx2d1a0c086e8e00c9";
    private final String appSecret = "6e013e7db0cb6b2fb9f584d6fe0e9da3";
    private final int timeout = 10;

    // 微信API地址
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TEMPLATE_MSG_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send";

    public WeChatMpOkHttpUtils() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }
    private static WeChatMpOkHttpUtils instance;

    public static WeChatMpOkHttpUtils getInstance(){
        if (instance == null) {
            synchronized (WeChatMpOkHttpUtils.class) {
                if (instance == null) {
                    instance = new WeChatMpOkHttpUtils();
                }
            }
        }
        return instance;
    }

    /**
     * 获取access_token
     */
    public String getAccessToken() throws IOException {
        HttpUrl url = HttpUrl.parse(ACCESS_TOKEN_URL).newBuilder()
                .addQueryParameter("grant_type", "client_credential")
                .addQueryParameter("appid", appId)
                .addQueryParameter("secret", appSecret)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }

            JSONObject json = JSON.parseObject(response.body().string());
            if (json.containsKey("access_token")) {
                return json.getString("access_token");
            } else {
                throw new IOException("Failed to get access_token: " + json);
            }
        }
    }

    /**
     * 发送模板消息
     * @param accessToken 接口调用凭证
     * @param openId 接收者openid
     * @param templateId 模板ID
     * @param data 模板数据 (示例: {"first":{"value":"您好"},"keyword1":{"value":"123"}})
     * @param url 跳转URL (可选)
     * @param miniProgram 跳转小程序 (可选)
     */
    public boolean sendTemplateMessage(
            String accessToken,
            String openId,
            String templateId,
            Map<String, Object> data,
            String url,
            Map<String, String> miniProgram
    ) throws IOException {
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("touser", openId);
        requestBody.put("template_id", templateId);
        requestBody.put("data", data);
        if (url != null) requestBody.put("url", url);
        if (miniProgram != null) requestBody.put("miniprogram", miniProgram);

        RequestBody body =
        RequestBody.create(MediaType.parse("text/plain"), JSON.toJSONString(requestBody));

        HttpUrl httpUrl = HttpUrl.parse(TEMPLATE_MSG_URL).newBuilder()
                .addQueryParameter("access_token", accessToken)
                .build();

        Request request = new Request.Builder()
                .url(httpUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code: " + response);
            }

            JSONObject json = JSON.parseObject(response.body().string());
            return json.getInteger("errcode") == 0;
        }
    }

    /**
     * 简化版发送模板消息 (不带跳转和小程序)
     */
    public boolean sendTemplateMessage(
            String accessToken,
            String openId,
            String templateId,
            Map<String, Object> data
    ) throws IOException {
        return sendTemplateMessage(accessToken, openId, templateId, data, null, null);
    }
}