package com.project.backend.controller;

import com.project.backend.pojo.dto.TeacherStudentPageQueryDTO;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import com.project.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端学生管理接口。
 */
@Slf4j
@RestController
@RequestMapping("/api/teacher/student")
@Tag(name = "教师端学生管理接口", description = "教师端学生管理相关接口")
public class TeacherStudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 分页查询教师名下学生列表。
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询教师学生列表", description = "返回教师端学生管理页表格数据")
    public Result<PageResult<TeacherStudentTableVO>> getTeacherStudentPage(TeacherStudentPageQueryDTO queryDTO) {
        log.info("分页查询教师学生列表，currentPage={}, pageSize={}, keyword={}",
                queryDTO.getCurrentPage(), queryDTO.getPageSize(), queryDTO.getKeyword());
        return Result.success(studentService.getTeacherStudentPage(queryDTO));
    }

    /**
     * 查询学生历史考勤。
     */
    @GetMapping("/attendance")
    @Operation(summary = "查询学生历史考勤", description = "教师查看指定课程下某个学生的历史考勤记录")
    public Result<List<AttendanceRecordVO>> getTeacherStudentAttendanceRecords(@RequestParam Long courseId,
                                                                                @RequestParam Long studentId) {
        log.info("查询教师端学生历史考勤: courseId={}, studentId={}", courseId, studentId);
        return Result.success(studentService.getTeacherStudentAttendanceRecords(courseId, studentId));
    }
}