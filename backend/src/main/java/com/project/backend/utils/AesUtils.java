package com.project.backend.utils;

import cn.hutool.crypto.symmetric.AES;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * AES 加密工具类
 * 用于人脸特征向量的加密存储
 */
@Slf4j
public class AesUtils {

    /**
     * 固定密钥（生产环境应从配置或密钥管理服务获取）
     * 密钥长度必须为 16/24/32 字节（对应 AES-128/192/256）
     */
    private static final String SECRET_KEY = "RollCallSys@2026";

    private static final AES aes = new AES(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }
        return aes.encryptBase64(plainText);
    }

    /**
     * 解密字符串
     *
     * @param cipherText Base64 编码的密文
     * @return 明文
     */
    public static String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return null;
        }
        return aes.decryptStr(cipherText);
    }
}
