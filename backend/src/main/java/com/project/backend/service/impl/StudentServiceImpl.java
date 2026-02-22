package com.project.backend.service.impl;

import com.project.backend.constant.MessageConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.*;
import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.entity.AttendanceRecord;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentVO;
import com.project.backend.service.MinioService;
import com.project.backend.service.PythonServiceClient;
import com.project.backend.service.StudentService;
import com.project.backend.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 学生服务实现类
 */
@Slf4j
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private MinioService minioService;

    @Autowired
    private PythonServiceClient pythonServiceClient;

    @Override
    public StudentVO getCurrentStudentInfo() {
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.findById(userId);
        Student student = studentMapper.findByUserId(userId);

        if (user == null || student == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }

        String avatarUrl = null;
        if (student.getAvatarUrl() != null) {
            avatarUrl = minioService.getFileUrl(student.getAvatarUrl());
        }

        return StudentVO.builder()
                .userId(user.getUserId())
                .studentNumber(student.getStudentNumber())
                .realName(user.getRealName())
                .adminClass(student.getAdminClass())
                .gender(student.getGender())
                .avatarUrl(avatarUrl)
                .build();
    }

    @Override
    public void uploadFaceFeature(FaceFeatureDTO faceFeatureDTO) {
        Long userId = BaseContext.getCurrentId();

        // AES 加密存储
        String encryptedFeature = AesUtils.encrypt(faceFeatureDTO.getFeatureVector());
        studentMapper.updateFeatureVector(userId, encryptedFeature);

        log.info("学生 {} 上传人脸特征成功", userId);
    }

    @Override
    public void registerFaceByImage(MultipartFile file) {
        Long userId = BaseContext.getCurrentId();

        // 1. 上传图片到 MinIO，返回对象路径 (objectKey)
        String objectKey = minioService.uploadFile(file, "faces");
        log.info("学生 {} 人脸图片上传成功: {}", userId, objectKey);

        // 2. 生成预签名 URL，供 Python 服务访问
        String imageUrl = minioService.getFileUrl(objectKey);

        // 3. 调用 Python 服务提取人脸特征
        String featureVector = pythonServiceClient.extractFaceFeature(imageUrl);
        if (featureVector == null || featureVector.isEmpty()) {
            // 特征提取失败，删除已上传的图片，避免垃圾数据
            minioService.deleteFile(objectKey);
            throw new BusinessException("人脸特征提取失败，请确保图片中有清晰的人脸");
        }

        // 4. AES 加密特征向量并存库
        String encryptedFeature = AesUtils.encrypt(featureVector);
        studentMapper.updateFeatureVector(userId, encryptedFeature);

        // 5. 保存 objectKey 到 student 表
        studentMapper.updateFaceImageKey(userId, objectKey);

        log.info("学生 {} 人脸登记成功", userId);
    }

    @Override
    public List<AttendanceRecordVO> getAttendanceRecords() {
        Long userId = BaseContext.getCurrentId();
        List<AttendanceRecord> records = attendanceRecordMapper.findByStudentId(userId);

        List<AttendanceRecordVO> result = new ArrayList<>();
        for (AttendanceRecord record : records) {
            AttendanceSession session = attendanceSessionMapper.findById(record.getSessionId());
            String courseName = "";
            if (session != null) {
                var course = courseMapper.findById(session.getCourseId());
                if (course != null) {
                    courseName = course.getCourseName();
                }
            }

            result.add(AttendanceRecordVO.builder()
                    .recordId(record.getRecordId())
                    .courseName(courseName)
                    .status(record.getStatus())
                    .statusText(getStatusText(record.getStatus()))
                    .attendanceTime(session != null ? session.getStartTime() : null)
                    .similarityScore(record.getSimilarityScore() != null ? record.getSimilarityScore().toString() : null)
                    .build());
        }

        return result;
    }

    /**
     * 获取状态文字
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "缺勤";
            case 1 -> "已到";
            case 2 -> "迟到";
            case 3 -> "请假";
            default -> "未知";
        };
    }
}

