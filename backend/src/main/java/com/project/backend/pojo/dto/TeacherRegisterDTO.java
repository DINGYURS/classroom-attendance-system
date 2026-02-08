package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师注册请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教师注册请求")
public class TeacherRegisterDTO implements Serializable {

    @Schema(description = "工号", example = "T2022001")
    private String jobNumber;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "李老师")
    private String realName;
}
