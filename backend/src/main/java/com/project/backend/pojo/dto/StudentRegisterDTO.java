package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生注册请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生注册请求")
public class StudentRegisterDTO implements Serializable {

    @Schema(description = "学号", example = "2022001")
    private String studentNumber;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "行政班级", example = "计科225")
    private String adminClass;

    @Schema(description = "性别: 1-男, 2-女", example = "1")
    private Integer gender;
}
