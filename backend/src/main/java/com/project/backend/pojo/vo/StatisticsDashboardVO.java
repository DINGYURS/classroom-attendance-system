package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 数据中心聚合响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据中心聚合响应")
public class StatisticsDashboardVO implements Serializable {

    @Schema(description = "学期筛选项")
    private List<StatisticsOptionVO> semesterOptions;

    @Schema(description = "课程筛选项")
    private List<StatisticsOptionVO> courseOptions;

    @Schema(description = "班级筛选项")
    private List<StatisticsOptionVO> classOptions;

    @Schema(description = "顶部概览数据")
    private StatisticsSummaryVO summaryData;

    @Schema(description = "总体考勤状态占比")
    private List<StatisticsStatusItemVO> statusDistribution;

    @Schema(description = "考勤趋势数据")
    private List<StatisticsTrendItemVO> attendanceTrend;

    @Schema(description = "课程出勤率对比")
    private List<StatisticsCourseRateVO> courseAttendanceComparison;

    @Schema(description = "班级考勤状态构成")
    private List<StatisticsClassStatusVO> classStatusComposition;

    @Schema(description = "学生异常排行")
    private List<StatisticsStudentAnomalyVO> studentAnomalyRanking;

    @Schema(description = "算法自动与人工修正统计")
    private List<StatisticsCorrectionVO> correctionAnalysis;
}
