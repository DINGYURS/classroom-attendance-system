package com.project.backend.controller;

import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceSessionVO;
import com.project.backend.pojo.vo.RecognitionResultVO;
import com.project.backend.pojo.vo.SessionRecordVO;
import com.project.backend.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考勤控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/attendance")
@Tag(name = "考勤接口", description = "点名发起、人脸识别、结果查询相关接口")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    /**
     * 发起点名
     */
    @PostMapping("/start")
    @Operation(summary = "发起点名", description = "教师发起课程点名")
    public Result<Long> startAttendance(@RequestBody AttendanceStartDTO startDTO) {
        log.info("发起点名: 课程ID={}", startDTO.getCourseId());
        Long sessionId = attendanceService.startAttendance(startDTO);
        return Result.success(sessionId);
    }

    /**
     * 结束点名
     */
    @PostMapping("/end/{sessionId}")
    @Operation(summary = "结束点名", description = "教师结束进行中的点名")
    public Result<Void> endAttendance(@PathVariable Long sessionId) {
        log.info("结束点名: 会话ID={}", sessionId);
        attendanceService.endAttendance(sessionId);
        return Result.success();
    }

    /**
     * 人脸识别考勤
     */
    @PostMapping("/recognize")
    @Operation(summary = "人脸识别", description = "上传人脸特征进行识别考勤")
    public Result<List<RecognitionResultVO>> recognizeFaces(@RequestBody FaceRecognitionDTO recognitionDTO) {
        log.info("人脸识别: 会话ID={}, 特征数={}", recognitionDTO.getSessionId(), 
                recognitionDTO.getFeatureVectors() != null ? recognitionDTO.getFeatureVectors().size() : 0);
        List<RecognitionResultVO> results = attendanceService.recognizeFaces(recognitionDTO);
        return Result.success(results);
    }

    /**
     * 获取考勤会话详情
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "获取会话详情", description = "获取考勤会话的统计信息")
    public Result<AttendanceSessionVO> getSessionDetail(@PathVariable Long sessionId) {
        AttendanceSessionVO sessionVO = attendanceService.getSessionDetail(sessionId);
        return Result.success(sessionVO);
    }

    /**
     * 获取会话考勤记录
     */
    @GetMapping("/session/{sessionId}/records")
    @Operation(summary = "获取考勤记录", description = "获取会话中所有学生的考勤记录")
    public Result<List<SessionRecordVO>> getSessionRecords(@PathVariable Long sessionId) {
        List<SessionRecordVO> records = attendanceService.getSessionRecords(sessionId);
        return Result.success(records);
    }

    /**
     * 获取课程考勤历史
     */
    @GetMapping("/course/{courseId}/history")
    @Operation(summary = "获取考勤历史", description = "获取课程的所有考勤会话记录")
    public Result<List<AttendanceSessionVO>> getCourseHistory(@PathVariable Long courseId) {
        List<AttendanceSessionVO> sessions = attendanceService.getCourseAttendanceHistory(courseId);
        return Result.success(sessions);
    }

    /**
     * 修改考勤状态
     */
    @PutMapping("/status")
    @Operation(summary = "修改考勤状态", description = "教师手动修改学生考勤状态（如请假、补签等）")
    public Result<Void> updateStatus(@RequestBody AttendanceUpdateDTO updateDTO) {
        log.info("修改考勤状态: recordId={}, status={}", updateDTO.getRecordId(), updateDTO.getStatus());
        attendanceService.updateAttendanceStatus(updateDTO);
        return Result.success();
    }
}
