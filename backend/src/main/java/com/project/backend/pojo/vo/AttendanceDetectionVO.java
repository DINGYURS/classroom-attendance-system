package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会话检测框展示 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话检测框展示数据")
public class AttendanceDetectionVO implements Serializable {

    @Schema(description = "检测结果 ID")
    private Long detectionId;

    @Schema(description = "图片索引")
    private Integer imageIndex;

    @Schema(description = "视角标识：left / center / right")
    private String viewKey;

    @Schema(description = "图片内人脸序号")
    private Integer faceIndex;

    @Schema(description = "检测框坐标 JSON，如 [x1,y1,x2,y2]")
    private String bbox;

    @Schema(description = "人脸检测分，仅表示该区域像人脸，不代表身份识别相似度")
    private String detectionScore;

    @Schema(description = "是否匹配成功")
    private Boolean matched;

    @Schema(description = "是否已忽略")
    private Boolean ignored;

    @Schema(description = "忽略原因")
    private String ignoreReason;

    @Schema(description = "匹配到的学生 ID")
    private Long studentId;

    @Schema(description = "匹配到的考勤记录 ID")
    private Long recordId;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "相似度分数")
    private String similarityScore;

    @Schema(description = "最终考勤状态")
    private Integer finalStatus;

    @Schema(description = "最终考勤状态文本")
    private String finalStatusText;

    @Schema(description = "是否人工修正")
    private Boolean manualModified;
}
