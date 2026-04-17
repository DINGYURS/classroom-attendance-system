package com.project.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Python 检测接口返回的单张图片结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceDetectImageResultDTO implements Serializable {

    /**
     * 图片索引
     */
    private Integer imageIndex;

    /**
     * 图片内检测到的人脸列表
     */
    private List<FaceDetectFaceDTO> faces;
}
