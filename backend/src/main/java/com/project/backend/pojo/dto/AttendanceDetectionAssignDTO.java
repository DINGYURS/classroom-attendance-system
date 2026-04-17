package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 指派检测框请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "指派检测框请求")
public class AttendanceDetectionAssignDTO implements Serializable {

    @Schema(description = "指派到的学生 ID")
    private Long studentId;
}
