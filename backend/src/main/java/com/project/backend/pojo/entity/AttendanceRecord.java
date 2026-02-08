package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 考勤明细记录表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord implements Serializable {

    /**
     * 记录 ID
     */
    private Long recordId;

    /**
     * 关联会话 ID
     */
    private Long sessionId;

    /**
     * 关联学生 ID
     */
    private Long studentId;

    /**
     * 考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假
     */
    private Integer status;

    /**
     * 识别相似度（如 0.8521）
     */
    private BigDecimal similarityScore;

    /**
     * 人脸坐标（如 [x,y,w,h]）
     */
    private String faceLocation;

    /**
     * 修改类型: 1-算法自动, 2-人工修正
     */
    private Integer updateType;
}
