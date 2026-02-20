package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户登录响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录响应")
public class UserLoginVO implements Serializable {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "角色: 1-Teacher, 2-Student")
    private Integer role;

    @Schema(description = "头像地址")
    private String avatarUrl;

    @Schema(description = "JWT 令牌")
    private String token;

    @Schema(description = "行政班级")
    private String adminClass;
}
