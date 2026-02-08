package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人脸特征上传 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人脸特征上传请求")
public class FaceFeatureDTO implements Serializable {

    @Schema(description = "人脸特征向量 (JSON 数组格式的 512 维向量)")
    private String featureVector;
}
