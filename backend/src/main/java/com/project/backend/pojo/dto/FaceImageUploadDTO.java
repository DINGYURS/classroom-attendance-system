package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生人脸图片上传请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人脸图片上传请求")
public class FaceImageUploadDTO implements Serializable {

    @Schema(description = "图片 MinIO 存储路径 (上传后返回)")
    private String imageKey;
}
