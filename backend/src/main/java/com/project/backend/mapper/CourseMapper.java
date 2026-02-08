package com.project.backend.mapper;

import com.project.backend.pojo.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程 Mapper 接口
 */
@Mapper
public interface CourseMapper {

    /**
     * 根据课程 ID 查询课程
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    Course findById(@Param("courseId") Long courseId);

    /**
     * 根据教师 ID 查询课程列表
     *
     * @param teacherId 教师 ID
     * @return 课程列表
     */
    List<Course> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 插入课程
     *
     * @param course 课程信息
     */
    void insert(Course course);

    /**
     * 更新课程
     *
     * @param course 课程信息
     */
    void update(Course course);

    /**
     * 删除课程
     *
     * @param courseId 课程 ID
     */
    void deleteById(@Param("courseId") Long courseId);
}
