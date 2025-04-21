package com.reeman.agv.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import timber.log.Timber;


/**
 * 微信AccessToken文件缓存工具类
 */
public class WeChatTokenCacheUtil {
    private final String cacheFilePath;
    private final int expires = 7200;// 微信token默认有效期7200秒(2小时)

    public WeChatTokenCacheUtil(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;
        File file = new File(cacheFilePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    /**
     * 保存token到缓存文件
     * @param accessToken token值
     */
    public void saveToken(String accessToken) {
        JSONObject tokenObj = new JSONObject();
        tokenObj.put("access_token", accessToken);
        tokenObj.put("expires_in", expires);
        tokenObj.put("timestamp", System.currentTimeMillis() / 1000); // 当前时间戳(秒)

        try {
            FileUtils.writeStringToFile(
                new File(cacheFilePath),
                tokenObj.toJSONString(),
                StandardCharsets.UTF_8
            );
            Timber.d("AccessToken已保存到文件: %s", cacheFilePath);

        } catch (IOException e) {
            Timber.e(e, "保存AccessToken到文件失败");
        }
    }
    
    /**
     * 从缓存文件读取token
     * @return 如果token有效返回token值，否则返回null
     */
    public String getToken() {
        try {
            if (!new File(cacheFilePath).exists()) {
                return null;
            }
            
            String content = FileUtils.readFileToString(
                new File(cacheFilePath),
                StandardCharsets.UTF_8
            );
            
            JSONObject tokenObj = JSON.parseObject(content);
            String token = tokenObj.getString("access_token");
            long timestamp = tokenObj.getLong("timestamp");
            int expiresIn = tokenObj.getIntValue("expires_in");
            
            // 检查token是否过期(提前5分钟过期)
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime - timestamp < expiresIn - 300) {
                Timber.d("从缓存文件读取有效AccessToken");
                return token;
            }

            Timber.d("AccessToken已过期");
            return null;
        } catch (IOException e) {
            Timber.e(e, "读取AccessToken缓存文件失败");
            return null;
        }
    }
    
    /**
     * 获取token的剩余有效时间(秒)
     * @return 剩余时间(秒)，如果文件不存在或已过期返回0
     */
    public int getTokenRemainingTime() {
        try {
            if (!new File(cacheFilePath).exists()) {
                return 0;
            }
            
            String content = FileUtils.readFileToString(
                new File(cacheFilePath),
                StandardCharsets.UTF_8
            );
            
            JSONObject tokenObj = JSON.parseObject(content);
            long timestamp = tokenObj.getLong("timestamp");
            int expiresIn = tokenObj.getIntValue("expires_in");
            
            long currentTime = System.currentTimeMillis() / 1000;
            int remaining = (int) (expiresIn - (currentTime - timestamp));
            return Math.max(remaining, 0);
        } catch (IOException e) {
            Timber.e(e,"计算token剩余时间失败");
            return 0;
        }
    }
    
    /**
     * 获取token的过期时间
     * @return LocalDateTime对象，表示token过期时间
     */
    public LocalDateTime getTokenExpireTime() {
        try {
            if (!new File(cacheFilePath).exists()) {
                return LocalDateTime.MIN;
            }
            
            String content = FileUtils.readFileToString(
                new File(cacheFilePath),
                StandardCharsets.UTF_8
            );
            
            JSONObject tokenObj = JSON.parseObject(content);
            long timestamp = tokenObj.getLong("timestamp");
            int expiresIn = tokenObj.getIntValue("expires_in");
            
            return LocalDateTime.ofEpochSecond(
                timestamp + expiresIn,
                0,
                ZoneOffset.ofHours(8)
            );
        } catch (IOException e) {
            Timber.e(e,"获取token过期时间失败");
            return LocalDateTime.MIN;
        }
    }
}