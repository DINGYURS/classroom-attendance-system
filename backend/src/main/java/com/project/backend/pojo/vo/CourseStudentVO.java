package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 课程学生信息 VO（用于名单展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程学生信息")
public class CourseStudentVO implements Serializable {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "学号")
    private String studentNumber;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "行政班级")
    private String adminClass;

    @Schema(description = "性别: 1-男, 2-女")
    private Integer gender;

    @Schema(description = "是否已录入人脸特征")
    private Boolean hasFaceFeature;
}
