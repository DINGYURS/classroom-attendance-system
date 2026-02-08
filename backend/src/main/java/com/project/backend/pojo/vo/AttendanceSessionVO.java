package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤会话 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤会话信息")
public class AttendanceSessionVO implements Serializable {

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态: 0-进行中, 1-已结束")
    private Integer status;

    @Schema(description = "已到人数")
    private Integer presentCount;

    @Schema(description = "总人数")
    private Integer totalCount;
}
