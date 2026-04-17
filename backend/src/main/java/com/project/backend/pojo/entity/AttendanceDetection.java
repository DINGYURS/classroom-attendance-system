package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 考勤检测结果实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDetection implements Serializable {

    /**
     * 检测结果 ID
     */
    private Long detectionId;

    /**
     * 关联会话 ID
     */
    private Long sessionId;

    /**
     * 来源图片索引
     */
    private Integer imageIndex;

    /**
     * 图片内人脸序号
     */
    private Integer faceIndex;

    /**
     * 检测框坐标 JSON，如 [x1,y1,x2,y2]
     */
    private String bbox;

    /**
     * 检测置信度
     */
    private BigDecimal detectionScore;

    /**
     * 是否匹配成功
     */
    private Boolean matched;

    /**
     * 匹配到的学生 ID
     */
    private Long studentId;

    /**
     * 匹配到的考勤记录 ID
     */
    private Long recordId;

    /**
     * 相似度分数
     */
    private BigDecimal similarityScore;

    /**
     * 是否已忽略
     */
    private Boolean ignored;

    /**
     * 忽略原因
     */
    private String ignoreReason;
}
