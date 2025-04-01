package com.reeman.points.request;

import android.content.Context;

import com.reeman.points.R;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyRetrofitClient {
    private static final String BASE_URL = "https://robot.ssmvv.com/"; // 外网地址 沈工测试地址
    private static Retrofit retrofit;

    // 获取不安全的 OkHttpClient（绕过 SSL 证书验证，仅用于测试）
    private static OkHttpClient getUnsafeOkHttpClient(Context context) {
        try {
            // 加载 Let's Encrypt 证书
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certInput = context.getResources().openRawResource(R.raw.isrgrootx1);
            Certificate ca;
            try {
                ca = cf.generateCertificate(certInput);
            } finally {
                certInput.close();
            }

            // 创建 KeyStore 并添加证书
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("letsencrypt", ca);

            // 创建 TrustManager 使用 KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            // 创建 SSLContext 并使用 TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Retrofit getInstance(Context context) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(getUnsafeOkHttpClient(context))
                    .addConverterFactory(GsonConverterFactory.create()) // 使用 Gson 解析 JSON
                    .build();
        }
        return retrofit;
    }
}
