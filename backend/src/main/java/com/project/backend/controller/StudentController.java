package com.project.backend.controller;

import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentNoticeVO;
import com.project.backend.pojo.vo.StudentVO;
import com.project.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/student")
@Tag(name = "学生接口", description = "学生注册、人脸入库、考勤查询相关接口")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 获取当前学生信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取学生信息", description = "获取当前登录学生的个人信息")
    public Result<StudentVO> getInfo() {
        StudentVO studentVO = studentService.getCurrentStudentInfo();
        return Result.success(studentVO);
    }

    /**
     * 上传人脸图片并登记人脸特征
     */
    @PostMapping("/face/register")
    @Operation(summary = "人脸登记", description = "上传人脸照片，后端自动存入 MinIO 并调用 Python 提取人脸特征存库")
    public Result<Void> registerFace(@RequestParam("file") MultipartFile file) {
        log.info("上传人脸图片进行特征登记: {}", file.getOriginalFilename());
        studentService.registerFaceByImage(file);
        return Result.success();
    }

    /**
     * 上传人脸特征 (直接提供特征向量，备用接口)
     */
    @PostMapping("/face/feature")
    @Operation(summary = "直接上传人脸特征向量", description = "前端已在本地提取特征向量时使用（备用）")
    public Result<Void> uploadFaceFeature(@RequestBody com.project.backend.pojo.dto.FaceFeatureDTO faceFeatureDTO) {
        log.info("上传人脸特征向量");
        studentService.uploadFaceFeature(faceFeatureDTO);
        return Result.success();
    }

    /**
     * 获取考勤记录
     */
    @GetMapping("/attendance")
    @Operation(summary = "获取考勤记录", description = "获取当前学生的所有考勤记录")
    public Result<List<AttendanceRecordVO>> getAttendanceRecords() {
        List<AttendanceRecordVO> records = studentService.getAttendanceRecords();
        return Result.success(records);
    }

    /**
     * 获取学生端通知列表
     */
    @GetMapping("/notices")
    @Operation(summary = "获取学生通知", description = "获取当前学生收到的考勤提醒通知")
    public Result<List<StudentNoticeVO>> getNoticeList() {
        return Result.success(studentService.getNoticeList());
    }

    /**
     * 标记通知为已读
     */
    @PutMapping("/notice/{noticeId}/read")
    @Operation(summary = "标记通知已读", description = "学生将某条通知标记为已读")
    public Result<Void> markNoticeRead(@PathVariable Long noticeId) {
        log.info("学生标记通知已读: noticeId={}", noticeId);
        studentService.markNoticeRead(noticeId);
        return Result.success();
    }
}

