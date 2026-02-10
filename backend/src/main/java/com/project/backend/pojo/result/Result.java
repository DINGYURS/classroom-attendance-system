package com.project.backend.pojo.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一 API 响应结构
 *
 * @param <T> 数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一 API 响应结构")
public class Result<T> implements Serializable {

    /**
     * 状态码：1为成功，其0为失败
     */
    @Schema(description = "状态码：1为成功，0为失败", example = "1")
    private Integer code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return Result.<T>builder()
                .code(1)
                .message("success")
                .build();
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(1)
                .message("success")
                .data(data)
                .build();
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return Result.<T>builder()
                .code(0)
                .message(message)
                .build();
    }

    /**
     * 自定义状态码的失败响应
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
