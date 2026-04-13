package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生异常排行项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生异常排行项")
public class StatisticsStudentAnomalyVO implements Serializable {

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "行政班级")
    private String adminClass;

    @Schema(description = "异常次数")
    private Integer anomalyCount;
}
