package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 学生考勤统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生考勤统计")
public class StudentStatisticsVO implements Serializable {

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "行政班级")
    private String adminClass;

    @Schema(description = "出勤次数")
    private Integer presentCount;

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "缺勤次数")
    private Integer absentCount;

    @Schema(description = "请假次数")
    private Integer leaveCount;

    @Schema(description = "总考勤次数")
    private Integer totalCount;

    @Schema(description = "出勤率")
    private BigDecimal attendanceRate;
}
