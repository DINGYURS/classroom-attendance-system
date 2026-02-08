package com.project.backend.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 课程学生关联表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseStudent implements Serializable {

    /**
     * 主键 ID
     */
    private Long id;

    /**
     * 课程 ID
     */
    private Long courseId;

    /**
     * 学生 ID
     */
    private Long studentId;

    /**
     * 加入时间
     */
    private LocalDateTime joinTime;
}
