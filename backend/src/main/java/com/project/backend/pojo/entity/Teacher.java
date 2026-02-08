package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师信息扩展表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Teacher implements Serializable {

    /**
     * 关联 user.user_id
     */
    private Long userId;

    /**
     * 工号
     */
    private String jobNumber;
}
