package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 会话考勤详情 VO（教师查看）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话考勤详情")
public class SessionRecordVO implements Serializable {

    @Schema(description = "记录 ID")
    private Long recordId;

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假")
    private Integer status;

    @Schema(description = "状态文字")
    private String statusText;

    @Schema(description = "相似度分数")
    private BigDecimal similarityScore;
}
