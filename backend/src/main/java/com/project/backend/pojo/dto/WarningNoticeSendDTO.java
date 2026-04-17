package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 教师端发送考勤提醒请求。
 */
@Data
@Schema(description = "教师端发送考勤提醒请求")
public class WarningNoticeSendDTO implements Serializable {

    /**
     * 接收学生用户 ID。
     */
    @Schema(description = "接收学生用户 ID")
    private Long studentId;

    /**
     * 关联课程 ID。
     */
    @Schema(description = "关联课程 ID")
    private Long courseId;

    /**
     * 发送时累计缺勤次数快照。
     */
    @Schema(description = "发送时累计缺勤次数快照")
    private Integer absentCount;

    /**
     * 通知标题。
     */
    @Schema(description = "通知标题")
    private String title;

    /**
     * 通知内容。
     */
    @Schema(description = "通知内容")
    private String content;
}
