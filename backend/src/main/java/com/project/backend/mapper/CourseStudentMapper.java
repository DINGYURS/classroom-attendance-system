package com.project.backend.mapper;

import com.project.backend.pojo.entity.CourseStudent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程学生关联 Mapper 接口
 */
@Mapper
public interface CourseStudentMapper {

    /**
     * 根据课程 ID 查询关联的学生 ID 列表
     *
     * @param courseId 课程 ID
     * @return 学生 ID 列表
     */
    List<Long> findStudentIdsByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据课程 ID 查询关联信息
     *
     * @param courseId 课程 ID
     * @return 关联信息列表
     */
    List<CourseStudent> findByCourseId(@Param("courseId") Long courseId);

    /**
     * 批量插入课程学生关联
     *
     * @param list 关联列表
     */
    void batchInsert(@Param("list") List<CourseStudent> list);

    /**
     * 删除课程学生关联
     *
     * @param courseId  课程 ID
     * @param studentId 学生 ID
     */
    void delete(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    /**
     * 根据课程 ID 删除所有关联
     *
     * @param courseId 课程 ID
     */
    void deleteByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询课程学生数量
     *
     * @param courseId 课程 ID
     * @return 学生数量
     */
    Integer countByCourseId(@Param("courseId") Long courseId);
}
