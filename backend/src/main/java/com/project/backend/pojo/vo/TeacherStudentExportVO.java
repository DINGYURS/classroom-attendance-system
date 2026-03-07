package com.project.backend.pojo.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生名单导出 Excel 数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherStudentExportVO {

    @ExcelProperty("课程名称")
    private String courseName;

    @ExcelProperty("学期时间")
    private String semester;

    @ExcelProperty("学号")
    private String studentId;

    @ExcelProperty("真实姓名")
    private String realName;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("行政班级")
    private String className;
}