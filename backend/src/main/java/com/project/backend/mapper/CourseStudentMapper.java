package com.project.backend.mapper;

import com.project.backend.pojo.entity.CourseStudent;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程学生关联 Mapper。
 */
@Mapper
public interface CourseStudentMapper {

    /**
     * 根据课程 ID 查询学生 ID 列表。
     */
    List<Long> findStudentIdsByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据课程 ID 查询关联记录。
     */
    List<CourseStudent> findByCourseId(@Param("courseId") Long courseId);

    /**
     * 批量插入课程学生关联。
     */
    void batchInsert(@Param("list") List<CourseStudent> list);

    /**
     * 删除单条课程学生关联。
     */
    void delete(@Param("courseId") Long courseId, @Param("studentId") Long studentId);

    /**
     * 根据课程 ID 删除全部关联。
     */
    void deleteByCourseId(@Param("courseId") Long courseId);

    /**
     * 统计课程学生数量。
     */
    Integer countByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询教师端学生管理表格数据。
     */
    List<TeacherStudentTableVO> pageTeacherStudents(@Param("teacherId") Long teacherId,
                                                    @Param("keyword") String keyword);
}
