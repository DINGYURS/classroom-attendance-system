package com.project.backend.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生端通知记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生端通知记录")
public class StudentNoticeVO implements Serializable {

    @Schema(description = "通知 ID")
    private Long noticeId;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "缺勤次数快照")
    private Integer absentCount;

    @Schema(description = "发送时间")
    private String sendTime;

    @JsonProperty("isRead")
    @Schema(description = "是否已读")
    private Boolean isRead;

    @Schema(description = "阅读时间")
    private String readTime;

    @JsonProperty("isRead")
    public Boolean getIsRead() {
        return isRead;
    }

    @JsonProperty("isRead")
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
