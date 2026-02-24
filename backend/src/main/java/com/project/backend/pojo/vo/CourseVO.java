package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 课程信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程信息")
public class CourseVO implements Serializable {

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "课程描述")
    private String description;

    @Schema(description = "学生人数")
    private Integer studentCount;

    @Schema(description = "班级列表（课程涉及的所有行政班级）")
    private List<String> classes;

    @Schema(description = "最近出勤率（百分比，如 95.0 表示 95%）")
    private Double attendanceRate;
}
