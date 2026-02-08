package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 发起点名请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "发起点名请求")
public class AttendanceStartDTO implements Serializable {

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "迟到阈值（分钟）", example = "10")
    private Integer lateThreshold;
}
