package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 趋势图数据项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势图数据项")
public class StatisticsTrendItemVO implements Serializable {

    @Schema(description = "横轴标签")
    private String label;

    @Schema(description = "出勤率")
    private BigDecimal attendanceRate;
}
