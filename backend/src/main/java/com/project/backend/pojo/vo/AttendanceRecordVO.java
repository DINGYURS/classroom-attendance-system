package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学生考勤记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生考勤记录")
public class AttendanceRecordVO implements Serializable {

    @Schema(description = "记录 ID")
    private Long recordId;

    @Schema(description = "会话 ID")
    private Long sessionId;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "教学班级/行政班级")
    private String teachingClass;

    @Schema(description = "考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假")
    private Integer status;

    @Schema(description = "考勤状态文字")
    private String statusText;

    @Schema(description = "点名时间")
    private LocalDateTime attendanceTime;

    @Schema(description = "识别相似度")
    private String similarityScore;

    @Schema(description = "修改类型: 1-算法自动, 2-人工修正")
    private Integer updateType;

    @Schema(description = "是否包含人工修正")
    private Boolean manualModified;
}
