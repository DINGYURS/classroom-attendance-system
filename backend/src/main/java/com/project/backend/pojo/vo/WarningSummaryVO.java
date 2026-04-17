package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 预警中心汇总卡片数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警中心汇总卡片数据")
public class WarningSummaryVO implements Serializable {

    @Schema(description = "高缺勤学生数")
    private Integer highAbsenceCount;

    @Schema(description = "今日已通知数")
    private Integer todayNotifyCount;

    @Schema(description = "未读提醒数")
    private Integer unreadNotifyCount;

    @Schema(description = "最高缺勤次数")
    private Integer maxAbsenceCount;
}
