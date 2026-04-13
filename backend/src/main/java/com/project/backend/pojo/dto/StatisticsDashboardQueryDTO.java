package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据中心查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据中心查询参数")
public class StatisticsDashboardQueryDTO implements Serializable {

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "行政班级")
    private String adminClass;

    @Schema(description = "开始日期，格式：yyyy-MM-dd")
    private String startDate;

    @Schema(description = "结束日期，格式：yyyy-MM-dd")
    private String endDate;

    @Schema(description = "异常统计是否包含请假")
    private Boolean anomalyIncludeLeave;
}
