package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户基础表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    /**
     * 主键 ID
     */
    private Long userId;

    /**
     * 账号（学号/工号）
     */
    private String username;

    /**
     * 加密密码
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 角色: 1-Teacher, 2-Student
     */
    private Integer role;

    /**
     * 头像地址（MinIO）
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
