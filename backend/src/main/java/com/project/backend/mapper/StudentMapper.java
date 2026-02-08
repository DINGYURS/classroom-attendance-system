package com.project.backend.mapper;

import com.project.backend.pojo.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学生 Mapper 接口
 */
@Mapper
public interface StudentMapper {

    /**
     * 根据用户 ID 查询学生信息
     */
    Student findByUserId(@Param("userId") Long userId);

    /**
     * 插入学生信息
     */
    void insert(Student student);

    /**
     * 更新学生信息
     */
    void update(Student student);

    /**
     * 更新人脸特征向量
     */
    void updateFeatureVector(@Param("userId") Long userId, @Param("featureVector") String featureVector);

    /**
     * 更新人脸图片路径
     */
    void updateFaceImageKey(@Param("userId") Long userId, @Param("avatar_url") String avatarUrl);

    /**
     * 根据用户 ID 列表查询学生信息
     */
    List<Student> findByUserIds(@Param("userIds") List<Long> userIds);
}

