package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 考勤档案筛选项。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "考勤档案筛选项")
public class AttendanceArchiveOptionsVO implements Serializable {

    @Schema(description = "课程选项")
    private List<StatisticsOptionVO> courseOptions;

    @Schema(description = "班级选项")
    private List<StatisticsOptionVO> classOptions;
}
