package com.project.backend.pojo.vo;

import com.project.backend.pojo.result.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 考勤档案分页结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤档案分页结果")
public class AttendanceArchivePageVO implements Serializable {

    @Schema(description = "汇总信息")
    private AttendanceArchiveSummaryVO summary;

    @Schema(description = "分页会话列表")
    private PageResult<AttendanceArchiveSessionVO> pageData;
}
