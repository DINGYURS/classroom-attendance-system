package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 算法自动与人工修正统计项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "算法自动与人工修正统计项")
public class StatisticsCorrectionVO implements Serializable {

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "算法自动识别记录数")
    private Integer autoCount;

    @Schema(description = "人工手动修正记录数")
    private Integer manualCount;
}
