package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 预警中心学生详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警中心学生详情")
public class WarningDetailVO implements Serializable {

    @Schema(description = "学生用户 ID")
    private Long userId;

    @Schema(description = "学号")
    private String studentId;

    @Schema(description = "学生姓名")
    private String studentName;

    @Schema(description = "行政班级")
    private String className;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "缺勤次数")
    private Integer absenceCount;

    @Schema(description = "最近缺勤时间")
    private String lastAbsenceTime;

    @Schema(description = "最近通知时间")
    private String lastNotifyTime;

    @Schema(description = "通知次数")
    private Integer notifyCount;

    @Schema(description = "是否存在未读提醒")
    private Boolean hasUnread;

    @Schema(description = "考勤轨迹")
    private List<WarningTimelineVO> timeline;
}
