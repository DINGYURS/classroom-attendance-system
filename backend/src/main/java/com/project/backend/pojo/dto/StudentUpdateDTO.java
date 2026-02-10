package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 学生信息修改 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "学生信息修改请求")
public class StudentUpdateDTO implements Serializable {

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "头像地址", example = "student/avatar/xxx.jpg")
    private String avatarUrl;
}
