package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤提醒通知记录表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceNotice implements Serializable {

    /**
     * 通知 ID
     */
    private Long noticeId;

    /**
     * 发送教师 ID
     */
    private Long teacherId;

    /**
     * 接收学生 ID
     */
    private Long studentId;

    /**
     * 相关课程 ID，为空表示综合提醒
     */
    private Long courseId;

    /**
     * 通知类型: 1-缺勤提醒
     */
    private Integer noticeType;

    /**
     * 发送时累计缺勤次数快照
     */
    private Integer absentCount;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 发送状态: 0-待发送, 1-已发送, 2-发送失败
     */
    private Integer sendStatus;

    /**
     * 阅读状态: 0-未读, 1-已读
     */
    private Integer readStatus;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
