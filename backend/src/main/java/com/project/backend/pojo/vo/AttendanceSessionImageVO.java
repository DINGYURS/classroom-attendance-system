package com.project.backend.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会话原图信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话原图信息")
public class AttendanceSessionImageVO implements Serializable {

    @Schema(description = "图片索引")
    private Integer imageIndex;

    @Schema(description = "视角标识：left / center / right")
    private String viewKey;

    @Schema(description = "原始对象存储 key")
    private String objectKey;

    @Schema(description = "可访问图片 URL")
    private String imageUrl;
}
