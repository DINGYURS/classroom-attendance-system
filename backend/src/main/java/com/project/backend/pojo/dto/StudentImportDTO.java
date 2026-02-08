package com.project.backend.pojo.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生导入 Excel 数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportDTO {

    @ExcelProperty("学号")
    private String studentNumber;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("行政班级")
    private String adminClass;

    @ExcelProperty("性别")
    private String gender;

    @ExcelProperty("初始密码")
    private String password;
}
