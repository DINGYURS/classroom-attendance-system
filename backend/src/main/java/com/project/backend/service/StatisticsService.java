package com.project.backend.service;

import com.project.backend.pojo.vo.CourseStatisticsVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;

import java.util.List;

/**
 * 统计服务接口
 */
public interface StatisticsService {

    /**
     * 获取课程考勤统计
     *
     * @param courseId 课程 ID
     * @return 课程统计信息
     */
    CourseStatisticsVO getCourseStatistics(Long courseId);

    /**
     * 获取课程中各学生的考勤统计
     *
     * @param courseId 课程 ID
     * @return 学生统计列表
     */
    List<StudentStatisticsVO> getStudentStatistics(Long courseId);
}
