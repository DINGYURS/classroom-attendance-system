package com.project.backend.pojo.vo;

import com.project.backend.pojo.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 预警中心分页结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "预警中心分页结果")
public class WarningCenterPageVO implements Serializable {

    @Schema(description = "汇总卡片数据")
    private WarningSummaryVO summary;

    @Schema(description = "缺勤排行分页数据")
    private PageResult<WarningRankingVO> pageData;
}
