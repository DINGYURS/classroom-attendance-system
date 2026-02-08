package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生信息扩展表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student implements Serializable {

    /**
     * 关联 user.user_id
     */
    private Long userId;

    /**
     * 学号
     */
    private String studentNumber;

    /**
     * 行政班级（如: 计科225）
     */
    private String adminClass;

    /**
     * 性别: 1-男, 2-女
     */
    private Integer gender;

    /**
     * 人脸特征值（AES 加密密文）
     */
    private String featureVector;

    /**
     * 人脸图片 MinIO 存储路径
     */
    private String faceImageKey;
}

