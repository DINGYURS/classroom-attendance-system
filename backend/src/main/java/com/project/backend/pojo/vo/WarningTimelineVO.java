package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 预警中心考勤轨迹项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警中心考勤轨迹项")
public class WarningTimelineVO implements Serializable {

    @Schema(description = "记录 ID")
    private Long id;

    @Schema(description = "时间")
    private String date;

    @Schema(description = "课程名称")
    private String course;

    @Schema(description = "考勤状态")
    private Integer status;

    @Schema(description = "状态文本")
    private String statusText;

    @Schema(description = "状态类型")
    private String statusType;
}
