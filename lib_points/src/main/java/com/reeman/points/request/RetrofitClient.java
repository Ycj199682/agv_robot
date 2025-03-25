package com.reeman.points.request;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://robot.ssmvv.com/"; // 外网地址
    private static Retrofit retrofit;

    // 获取不安全的 OkHttpClient（绕过 SSL 证书验证，仅用于测试）
    private static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // 创建信任管理器，接受所有证书
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 初始化 SSL 上下文，并使用上述的信任管理器
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 创建 OkHttpClient，使用绕过 SSL 校验的配置
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true) // 忽略主机名验证
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Retrofit getInstance() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getUnsafeOkHttpClient()) // 测试环境绕过ssl验证
                    .addConverterFactory(GsonConverterFactory.create()) // 使用 Gson 解析 JSON
                    .build();
        }
        return retrofit;
    }
}
