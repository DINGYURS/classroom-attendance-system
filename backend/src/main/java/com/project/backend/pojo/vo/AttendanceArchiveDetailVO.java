package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 考勤档案单次会话明细。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤档案单次会话明细")
public class AttendanceArchiveDetailVO implements Serializable {

    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "学号")
    private String studentId;

    @Schema(description = "姓名")
    private String studentName;

    @Schema(description = "班级")
    private String className;

    @Schema(description = "考勤状态")
    private String status;

    @Schema(description = "记录方式")
    private String type;

    @Schema(description = "相似度")
    private String similarityScore;
}
