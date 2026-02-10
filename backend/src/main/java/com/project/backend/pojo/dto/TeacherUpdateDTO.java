package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 教师信息修改 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教师信息修改请求")
public class TeacherUpdateDTO implements Serializable {

    @Schema(description = "工号", example = "18888888888")
    private String jobNumber;

    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "真实姓名", example = "李老师")
    private String realName;
}
