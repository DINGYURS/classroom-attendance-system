package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 班级考勤状态构成项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "班级考勤状态构成项")
public class StatisticsClassStatusVO implements Serializable {

    @Schema(description = "行政班级")
    private String adminClass;

    @Schema(description = "已到人次")
    private Integer presentCount;

    @Schema(description = "迟到人次")
    private Integer lateCount;

    @Schema(description = "缺勤人次")
    private Integer absentCount;

    @Schema(description = "请假人次")
    private Integer leaveCount;
}
