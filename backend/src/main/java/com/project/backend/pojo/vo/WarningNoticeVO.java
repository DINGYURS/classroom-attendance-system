package com.project.backend.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师端预警通知记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教师端预警通知记录")
public class WarningNoticeVO implements Serializable {

    @Schema(description = "通知 ID")
    private Long id;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "学号")
    private String studentId;

    @Schema(description = "关联课程 ID")
    private Long courseId;

    @Schema(description = "关联课程名称")
    private String courseName;

    @Schema(description = "缺勤次数快照")
    private Integer absenceSnapshot;

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "发送时间")
    private String sentTime;

    @JsonProperty("isRead")
    @Schema(description = "是否已读")
    private Boolean isRead;

    @JsonProperty("isRead")
    public Boolean getIsRead() {
        return isRead;
    }

    @JsonProperty("isRead")
    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
