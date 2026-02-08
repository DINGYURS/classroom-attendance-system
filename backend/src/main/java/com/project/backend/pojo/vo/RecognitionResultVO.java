package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 人脸识别结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "人脸识别结果")
public class RecognitionResultVO implements Serializable {

    @Schema(description = "学生 ID")
    private Long studentId;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "相似度分数")
    private Double similarity;

    @Schema(description = "是否识别成功")
    private Boolean matched;

    @Schema(description = "考勤状态: 1-已到, 2-迟到")
    private Integer status;
}
