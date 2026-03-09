package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师端学生管理表格 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教师端学生管理表格")
public class TeacherStudentTableVO implements Serializable {

    @Schema(description = "选课关联 ID")
    private Long id;

    @Schema(description = "课程 ID")
    private Long courseId;

    @Schema(description = "学生用户 ID")
    private Long userId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "学期")
    private String semester;

    @Schema(description = "学号")
    private String studentId;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "行政班级")
    private String className;

    @Schema(description = "头像 URL")
    private String avatarUrl;
}
