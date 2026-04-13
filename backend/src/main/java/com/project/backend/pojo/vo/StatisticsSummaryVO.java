package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据中心概览统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据中心概览统计")
public class StatisticsSummaryVO implements Serializable {

    @Schema(description = "课程总数")
    private Integer totalCourses;

    @Schema(description = "覆盖学生数")
    private Integer coveredStudents;

    @Schema(description = "累计点名次数")
    private Integer totalSessions;

    @Schema(description = "平均出勤率")
    private BigDecimal avgAttendanceRate;

    @Schema(description = "异常总人次")
    private Integer totalAnomalies;

    @Schema(description = "人脸录入率")
    private BigDecimal faceEntryRate;
}
