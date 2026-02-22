package com.project.backend.service;

import java.util.List;

/**
 * Python 算法服务客户端接口
 * Python 只负责图像处理（特征提取），所有比对逻辑在 Java 完成。
 */
public interface PythonServiceClient {

    /**
     * 单人人脸注册：提取单张图片中唯一人脸的特征向量
     *
     * @param imageUrl MinIO 预签名图片 URL
     * @return 特征向量 JSON 字符串（"[0.123, ...]"），用于 AES 加密后存库；失败返回 null
     */
    String extractFaceFeature(String imageUrl);

    /**
     * 考勤合照检测：从多张图片中检测并提取所有人脸的特征向量
     *
     * @param imageUrls 合照的 MinIO 预签名 URL 列表
     * @return 每张人脸的 embedding，List<List<Double>>，Java 拿到后自行与数据库比对
     */
    List<List<Double>> detectFaces(List<String> imageUrls);

    /**
     * 检查 Python 服务健康状态
     */
    boolean healthCheck();
}
