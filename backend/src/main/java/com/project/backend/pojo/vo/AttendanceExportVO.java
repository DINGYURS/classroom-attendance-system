package com.project.backend.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤报表导出 Excel 数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceExportVO {

    @ExcelProperty("学号")
    private String studentNumber;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("行政班级")
    private String adminClass;

    @ExcelProperty("出勤次数")
    private Integer presentCount;

    @ExcelProperty("迟到次数")
    private Integer lateCount;

    @ExcelProperty("缺勤次数")
    private Integer absentCount;

    @ExcelProperty("请假次数")
    private Integer leaveCount;

    @ExcelProperty("出勤率(%)")
    private String attendanceRate;
}
