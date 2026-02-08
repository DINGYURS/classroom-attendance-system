package com.project.backend.controller;

import com.project.backend.pojo.result.Result;
import com.project.backend.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件接口", description = "文件上传相关接口")
public class FileController {

    @Autowired
    private MinioService minioService;

    /**
     * 上传人脸图片
     */
    @PostMapping("/upload/face")
    @Operation(summary = "上传人脸图片", description = "上传学生人脸照片到 MinIO，返回存储路径")
    public Result<String> uploadFaceImage(@RequestParam("file") MultipartFile file) {
        log.info("上传人脸图片: {}", file.getOriginalFilename());
        String objectKey = minioService.uploadFile(file, "faces");
        return Result.success(objectKey);
    }

    /**
     * 上传考勤合照
     */
    @PostMapping("/upload/attendance")
    @Operation(summary = "上传考勤合照", description = "上传班级合照到 MinIO，用于人脸识别考勤")
    public Result<String> uploadAttendanceImage(@RequestParam("file") MultipartFile file) {
        log.info("上传考勤合照: {}", file.getOriginalFilename());
        String objectKey = minioService.uploadFile(file, "attendance");
        return Result.success(objectKey);
    }

    /**
     * 获取文件访问 URL
     */
    @GetMapping("/url")
    @Operation(summary = "获取文件URL", description = "根据存储路径获取文件访问URL")
    public Result<String> getFileUrl(@RequestParam String objectKey) {
        String url = minioService.getFileUrl(objectKey);
        return Result.success(url);
    }
}
