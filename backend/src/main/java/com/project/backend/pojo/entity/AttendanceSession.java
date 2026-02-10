package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 考勤会话表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSession implements Serializable {

    /**
     * 会话 ID
     */
    private Long sessionId;

    /**
     * 所属课程 ID
     */
    private Long courseId;

    /**
     * 原始合照 URL 列表（JSON Array）
     */
    private String sourceImages;

    /**
     * 应到人数
     */
    private Integer totalStudent;

    /**
     * 实到人数
     */
    private Integer actualStudent;

    /**
     * 点名开始时间
     */
    private LocalDateTime startTime;
}

