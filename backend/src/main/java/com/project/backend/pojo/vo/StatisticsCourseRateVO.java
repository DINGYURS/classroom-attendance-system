package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 课程出勤率对比项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程出勤率对比项")
public class StatisticsCourseRateVO implements Serializable {

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "出勤率")
    private BigDecimal attendanceRate;
}
