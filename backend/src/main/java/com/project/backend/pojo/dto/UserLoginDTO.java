package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求")
public class UserLoginDTO implements Serializable {

    @Schema(description = "用户名（学号/工号）", example = "2022001")
    private String username;

    @Schema(description = "密码", example = "123456")
    private String password;
}
