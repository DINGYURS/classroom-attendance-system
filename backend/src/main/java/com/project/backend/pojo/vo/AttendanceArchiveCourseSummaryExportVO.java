package com.project.backend.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤档案课程汇总导出对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceArchiveCourseSummaryExportVO {

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("学期")
    private String semester;

    @ExcelProperty("点名次数")
    private Integer totalSessions;

    @ExcelProperty("应到总人次")
    private Integer expectedTotal;

    @ExcelProperty("实到总人次")
    private Integer actualTotal;

    @ExcelProperty("缺勤总人次")
    private Integer absentTotal;

    @ExcelProperty("迟到总人次")
    private Integer lateTotal;

    @ExcelProperty("请假总人次")
    private Integer leaveTotal;

    @ExcelProperty("平均出勤率")
    private String avgRate;
}
