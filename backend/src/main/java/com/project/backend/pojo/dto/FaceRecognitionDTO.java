package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 人脸识别考勤请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人脸识别考勤请求")
public class FaceRecognitionDTO implements Serializable {

    @Schema(description = "考勤会话 ID")
    private Long sessionId;

    @Schema(description = "合照图片的 MinIO objectKey 列表（如 [\"attendance/xxx.jpg\"]）")
    private List<String> imageKeys;
}
