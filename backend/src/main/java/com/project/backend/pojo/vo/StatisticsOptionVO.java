package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统计筛选项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计筛选项")
public class StatisticsOptionVO implements Serializable {

    @Schema(description = "显示名称")
    private String label;

    @Schema(description = "选项值")
    private String value;
}
