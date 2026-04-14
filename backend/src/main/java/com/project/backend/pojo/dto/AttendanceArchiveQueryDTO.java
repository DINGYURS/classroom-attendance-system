package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 教师端考勤档案查询参数。
 */
@Data
@Schema(description = "教师端考勤档案查询参数")
public class AttendanceArchiveQueryDTO implements Serializable {

    /**
     * 课程 ID。
     */
    @Schema(description = "课程 ID")
    private Long courseId;

    /**
     * 行政班级。
     */
    @Schema(description = "行政班级")
    private String adminClass;

    /**
     * 开始日期，格式 yyyy-MM-dd。
     */
    @Schema(description = "开始日期，格式：yyyy-MM-dd")
    private String startDate;

    /**
     * 结束日期，格式 yyyy-MM-dd。
     */
    @Schema(description = "结束日期，格式：yyyy-MM-dd")
    private String endDate;

    /**
     * 考勤状态。
     */
    @Schema(description = "考勤状态：0-缺勤，1-已到，2-迟到，3-请假")
    private Integer status;

    /**
     * 会话类型。
     */
    @Schema(description = "会话类型：1-课堂识别，2-人工修正")
    private Integer type;

    /**
     * 学号或姓名关键字。
     */
    @Schema(description = "学号或姓名关键字")
    private String keyword;

    /**
     * 当前页。
     */
    @Schema(description = "当前页码，从 1 开始")
    private Integer currentPage = 1;

    /**
     * 每页条数。
     */
    @Schema(description = "每页条数")
    private Integer pageSize = 10;
}
