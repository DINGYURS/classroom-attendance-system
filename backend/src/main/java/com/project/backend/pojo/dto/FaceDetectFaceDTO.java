package com.project.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Python 检测接口返回的单张人脸数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaceDetectFaceDTO implements Serializable {

    /**
     * 检测框 [x1, y1, x2, y2]
     */
    private List<Integer> bbox;

    /**
     * 人脸特征向量
     */
    private List<Double> embedding;

    /**
     * 检测置信度
     */
    private Double detScore;
}
