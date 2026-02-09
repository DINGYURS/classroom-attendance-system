package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户注册请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户注册请求")
public class UserRegisterDTO implements Serializable {

    @Schema(description = "用户名(学号/工号)", example = "3220421100")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "角色: 1-Teacher, 2-Student", example = "2")
    private Integer role;

    @Schema(description = "行政班级(学生)", example = "计科225")
    private String adminClass;

    @Schema(description = "性别: 1-男 2-女(学生)", example = "1")
    private Integer gender;
}
