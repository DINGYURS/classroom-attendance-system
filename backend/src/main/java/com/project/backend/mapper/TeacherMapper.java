package com.project.backend.mapper;

import com.project.backend.pojo.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教师 Mapper 接口
 */
@Mapper
public interface TeacherMapper {

    /**
     * 根据用户 ID 查询教师信息
     *
     * @param userId 用户 ID
     * @return 教师信息
     */
    Teacher findByUserId(@Param("userId") Long userId);

    /**
     * 插入教师信息
     *
     * @param teacher 教师信息
     */
    void insert(Teacher teacher);

    /**
     * 更新教师信息
     *
     * @param teacher 教师信息
     */
    void update(Teacher teacher);
}
