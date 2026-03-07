package com.project.backend.pojo.dto;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生名单导入模板对应的数据对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class StudentImportDTO {

    @ExcelProperty(index = 0)
    private String courseName;

    @ExcelProperty(index = 1)
    private String semester;

    @ExcelProperty(index = 2)
    private String studentNumber;

    @ExcelProperty(index = 3)
    private String realName;

    @ExcelProperty(index = 4)
    private String gender;

    @ExcelProperty(index = 5)
    private String adminClass;
}