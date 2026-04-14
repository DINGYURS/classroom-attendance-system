package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤档案会话详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤档案会话详情")
public class AttendanceArchiveSessionDetailVO implements Serializable {

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "班级名称")
    private String className;

    @Schema(description = "点名时间")
    private String sessionTime;

    @Schema(description = "应到人数")
    private Integer expectedCount;

    @Schema(description = "实到人数")
    private Integer actualCount;

    @Schema(description = "缺勤人数")
    private Integer absentCount;

    @Schema(description = "迟到人数")
    private Integer lateCount;

    @Schema(description = "请假人数")
    private Integer leaveCount;

    @Schema(description = "出勤率")
    private String attendanceRate;

    @Schema(description = "会话类型")
    private String type;

    @Schema(description = "明细列表")
    private List<AttendanceArchiveDetailVO> detailList;
}
