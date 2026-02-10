package com.project.backend.controller;

import com.project.backend.pojo.dto.CourseDTO;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.CourseStudentVO;
import com.project.backend.pojo.vo.CourseVO;
import com.project.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课程控制器（教师端）
 */
@Slf4j
@RestController
@RequestMapping("/api/course")
@Tag(name = "课程接口", description = "课程管理、班级管理相关接口（教师端）")
public class CourseController {

    @Autowired
    private CourseService courseService;

    /**
     * 创建课程
     */
    @PostMapping
    @Operation(summary = "创建课程", description = "教师创建新课程")
    public Result<Long> createCourse(@RequestBody CourseDTO courseDTO) {
        log.info("创建课程: {}", courseDTO.getCourseName());
        Long courseId = courseService.createCourse(courseDTO);
        return Result.success(courseId);
    }

    /**
     * 更新课程
     */
    @PutMapping
    @Operation(summary = "更新课程", description = "更新课程信息")
    public Result<Void> updateCourse(@RequestBody CourseDTO courseDTO) {
        log.info("更新课程: {}", courseDTO.getCourseId());
        courseService.updateCourse(courseDTO);
        return Result.success();
    }

    /**
     * 删除课程
     */
    @DeleteMapping("/{courseId}")
    @Operation(summary = "删除课程", description = "删除课程及其关联数据")
    public Result<Void> deleteCourse(@PathVariable Long courseId) {
        log.info("删除课程: {}", courseId);
        courseService.deleteCourse(courseId);
        return Result.success();
    }

    /**
     * 获取我的课程列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取我的课程", description = "获取当前教师的所有课程")
    public Result<List<CourseVO>> getMyCourses() {
        List<CourseVO> courses = courseService.getMyCourses();
        return Result.success(courses);
    }

    /**
     * 获取课程详情
     */
    @GetMapping("/{courseId}")
    @Operation(summary = "获取课程详情", description = "获取指定课程的详细信息")
    public Result<CourseVO> getCourseDetail(@PathVariable Long courseId) {
        CourseVO courseVO = courseService.getCourseDetail(courseId);
        return Result.success(courseVO);
    }

    /**
     * 获取课程学生名单
     */
    @GetMapping("/{courseId}/students")
    @Operation(summary = "获取学生名单", description = "获取课程中的所有学生")
    public Result<List<CourseStudentVO>> getCourseStudents(@PathVariable Long courseId) {
        List<CourseStudentVO> students = courseService.getCourseStudents(courseId);
        return Result.success(students);
    }

    /**
     * 添加学生到课程
     */
    @PostMapping("/{courseId}/student/{studentId}")
    @Operation(summary = "添加学生", description = "将学生添加到课程")
    public Result<Void> addStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        log.info("添加学生 {} 到课程 {}", studentId, courseId);
        courseService.addStudentToCourse(courseId, studentId);
        return Result.success();
    }

    /**
     * 从课程移除学生
     */
    @DeleteMapping("/{courseId}/student/{studentId}")
    @Operation(summary = "移除学生", description = "将学生从课程中移除")
    public Result<Void> removeStudent(@PathVariable Long courseId, @PathVariable Long studentId) {
        log.info("从课程 {} 移除学生 {}", courseId, studentId);
        courseService.removeStudentFromCourse(courseId, studentId);
        return Result.success();
    }
}
