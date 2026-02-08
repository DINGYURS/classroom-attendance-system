package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Python 算法服务请求 DTO - 人脸检测
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人脸检测请求")
public class FaceDetectRequestDTO implements Serializable {

    @Schema(description = "图片 URL 列表 (MinIO 中的地址)")
    private List<String> imageUrls;

    @Schema(description = "考勤会话 ID")
    private Long sessionId;
}
