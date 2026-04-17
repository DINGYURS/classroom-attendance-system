package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 预警中心筛选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警中心筛选项")
public class WarningOptionsVO implements Serializable {

    @Schema(description = "课程筛选项")
    private List<StatisticsOptionVO> courseOptions;

    @Schema(description = "班级筛选项")
    private List<StatisticsOptionVO> classOptions;
}
