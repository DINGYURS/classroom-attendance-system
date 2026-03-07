package com.project.backend.pojo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 教师端学生管理分页查询参数。
 */
@Data
public class TeacherStudentPageQueryDTO implements Serializable {

    /**
     * 关键字，可匹配课程名、学号、姓名、行政班。
     */
    private String keyword;

    /**
     * 当前页码，从 1 开始。
     */
    private Integer currentPage = 1;

    /**
     * 每页条数。
     */
    private Integer pageSize = 10;
}
