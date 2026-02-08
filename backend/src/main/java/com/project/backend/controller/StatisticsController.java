package com.project.backend.controller;

import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.CourseStatisticsVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;
import com.project.backend.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "统计接口", description = "考勤数据统计相关接口")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 获取课程考勤统计
     */
    @GetMapping("/course/{courseId}")
    @Operation(summary = "课程考勤统计", description = "获取课程的整体考勤统计数据")
    public Result<CourseStatisticsVO> getCourseStatistics(@PathVariable Long courseId) {
        log.info("获取课程统计: courseId={}", courseId);
        CourseStatisticsVO statistics = statisticsService.getCourseStatistics(courseId);
        return Result.success(statistics);
    }

    /**
     * 获取课程中各学生的考勤统计
     */
    @GetMapping("/course/{courseId}/students")
    @Operation(summary = "学生考勤统计", description = "获取课程中每个学生的考勤统计数据")
    public Result<List<StudentStatisticsVO>> getStudentStatistics(@PathVariable Long courseId) {
        log.info("获取学生统计: courseId={}", courseId);
        List<StudentStatisticsVO> statistics = statisticsService.getStudentStatistics(courseId);
        return Result.success(statistics);
    }
}
