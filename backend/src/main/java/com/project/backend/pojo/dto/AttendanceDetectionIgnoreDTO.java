package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 忽略检测框请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "忽略检测框请求")
public class AttendanceDetectionIgnoreDTO implements Serializable {

    @Schema(description = "忽略原因")
    private String ignoreReason;
}
