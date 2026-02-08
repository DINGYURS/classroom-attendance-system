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

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假")
    private Integer status;

    @Schema(description = "考勤状态文字")
    private String statusText;

    @Schema(description = "点名时间")
    private LocalDateTime attendanceTime;

    @Schema(description = "识别相似度")
    private String similarityScore;
}
