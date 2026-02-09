package com.project.backend.controller;

import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.dto.FaceImageUploadDTO;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentVO;
import com.project.backend.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
     * 上传人脸特征 (直接提供特征向量)
     */
    @PostMapping("/face")
    @Operation(summary = "上传人脸特征", description = "直接上传人脸特征向量（前端已提取）")
    public Result<Void> uploadFaceFeature(@RequestBody FaceFeatureDTO faceFeatureDTO) {
        log.info("上传人脸特征");
        studentService.uploadFaceFeature(faceFeatureDTO);
        return Result.success();
    }

    /**
     * 通过图片登记人脸 (推荐使用)
     */
    @PostMapping("/face/image")
    @Operation(summary = "通过图片登记人脸", description = "先调用 /api/file/upload/face 上传图片，再传入返回的 imageKey 进行人脸特征提取")
    public Result<Void> registerFaceByImage(@RequestBody FaceImageUploadDTO uploadDTO) {
        log.info("通过图片登记人脸: {}", uploadDTO.getImageKey());
        studentService.registerFaceByImage(uploadDTO.getImageKey());
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
}

