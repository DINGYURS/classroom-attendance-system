package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
