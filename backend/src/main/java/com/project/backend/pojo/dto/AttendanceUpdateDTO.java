package com.project.backend.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 修改考勤状态请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "修改考勤状态请求")
public class AttendanceUpdateDTO implements Serializable {

    @Schema(description = "考勤记录 ID")
    private Long recordId;

    @Schema(description = "新状态: 0-缺勤, 1-已到, 2-迟到, 3-请假")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
