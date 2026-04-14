package com.project.backend.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 考勤档案筛选结果导出对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceArchiveExportVO {

    @ExcelProperty("点名时间")
    private String sessionTime;

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("学号")
    private String studentId;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("考勤状态")
    private String status;

    @ExcelProperty("记录方式")
    private String type;

    @ExcelProperty("相似度")
    private String similarityScore;
}
