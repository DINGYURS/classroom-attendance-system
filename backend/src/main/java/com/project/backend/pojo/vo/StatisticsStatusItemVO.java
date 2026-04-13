package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 状态占比项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "状态占比项")
public class StatisticsStatusItemVO implements Serializable {

    @Schema(description = "状态名称")
    private String name;

    @Schema(description = "数量")
    private Integer value;
}
