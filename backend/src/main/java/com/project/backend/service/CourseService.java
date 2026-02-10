package com.project.backend.service;

import com.project.backend.pojo.dto.CourseDTO;
import com.project.backend.pojo.vo.CourseStudentVO;
import com.project.backend.pojo.vo.CourseVO;

import java.util.List;

/**
 * 教师/课程服务接口
 */
public interface CourseService {

    /**
     * 创建课程
     *
     * @param courseDTO 课程信息
     * @return 课程 ID
     */
    Long createCourse(CourseDTO courseDTO);

    /**
     * 更新课程
     *
     * @param courseDTO 课程信息
     */
    void updateCourse(CourseDTO courseDTO);

    /**
     * 删除课程
     *
     * @param courseId 课程 ID
     */
    void deleteCourse(Long courseId);

    /**
     * 获取当前教师的课程列表
     *
     * @return 课程列表
     */
    List<CourseVO> getMyCourses();

    /**
     * 获取课程详情
     *
     * @param courseId 课程 ID
     * @return 课程信息
     */
    CourseVO getCourseDetail(Long courseId);

    /**
     * 获取课程学生名单
     *
     * @param courseId 课程 ID
     * @return 学生列表
     */
    List<CourseStudentVO> getCourseStudents(Long courseId);

    /**
     * 添加学生到课程
     *
     * @param courseId  课程 ID
     * @param studentId 学生 ID
     */
    void addStudentToCourse(Long courseId, Long studentId);

    /**
     * 从课程移除学生
     *
     * @param courseId  课程 ID
     * @param studentId 学生 ID
     */
    void removeStudentFromCourse(Long courseId, Long studentId);
}
