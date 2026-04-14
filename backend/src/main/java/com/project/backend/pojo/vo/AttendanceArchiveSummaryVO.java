package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 考勤档案汇总信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤档案汇总信息")
public class AttendanceArchiveSummaryVO implements Serializable {

    @Schema(description = "点名次数")
    private Integer totalSessions;

    @Schema(description = "应到总人次")
    private Integer expectedTotal;

    @Schema(description = "实到总人次")
    private Integer actualTotal;

    @Schema(description = "缺勤总人次")
    private Integer absentTotal;

    @Schema(description = "迟到总人次")
    private Integer lateTotal;

    @Schema(description = "平均出勤率")
    private String avgRate;
}
