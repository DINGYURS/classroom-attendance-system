package com.project.backend.service;

import java.util.List;
import java.util.Map;

/**
 * Python 算法服务客户端接口
 */
public interface PythonServiceClient {

    /**
     * 提取人脸特征向量
     *
     * @param imageUrl 人脸图片 URL (MinIO)
     * @return 特征向量 JSON 字符串
     */
    String extractFaceFeature(String imageUrl);

    /**
     * 批量检测并识别人脸 (用于考勤)
     *
     * @param imageUrls        班级合照 URL 列表
     * @param studentFeatures  学生特征向量 Map (studentId -> featureVector)
     * @return 识别结果列表 (studentId -> similarity)
     */
    List<Map<String, Object>> recognizeFaces(List<String> imageUrls, Map<Long, String> studentFeatures);

    /**
     * 检查服务健康状态
     *
     * @return 是否健康
     */
    boolean healthCheck();
}
