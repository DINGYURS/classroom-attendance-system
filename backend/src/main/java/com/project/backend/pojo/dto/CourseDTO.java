package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 课程创建/更新 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程信息")
public class CourseDTO implements Serializable {

    @Schema(description = "课程 ID（更新时必填）")
    private Long courseId;

    @Schema(description = "课程名称", example = "Java程序设计")
    private String courseName;

    @Schema(description = "学期", example = "2025-2026-1")
    private String semester;

    @Schema(description = "课程描述", example = "本课程主要讲解Java编程基础...")
    private String description;
}
