package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 课程考勤统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程考勤统计")
public class CourseStatisticsVO implements Serializable {

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "总点名次数")
    private Integer totalSessions;

    @Schema(description = "总学生人数")
    private Integer totalStudents;

    @Schema(description = "平均出勤率")
    private BigDecimal avgAttendanceRate;

    @Schema(description = "出勤人次")
    private Integer presentCount;

    @Schema(description = "迟到人次")
    private Integer lateCount;

    @Schema(description = "缺勤人次")
    private Integer absentCount;

    @Schema(description = "请假人次")
    private Integer leaveCount;
}
