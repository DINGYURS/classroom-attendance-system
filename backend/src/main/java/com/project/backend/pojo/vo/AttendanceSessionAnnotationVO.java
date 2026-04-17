package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 会话标注结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话标注结果")
public class AttendanceSessionAnnotationVO implements Serializable {

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "会话原图列表")
    private List<AttendanceSessionImageVO> images;

    @Schema(description = "检测框列表")
    private List<AttendanceDetectionVO> detections;
}
