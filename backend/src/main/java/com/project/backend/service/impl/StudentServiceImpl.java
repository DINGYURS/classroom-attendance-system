package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.*;
import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.dto.StudentRegisterDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    @Transactional
    public void register(StudentRegisterDTO registerDTO) {
        // 检查用户名是否已存在
        User existUser = userMapper.findByUsername(registerDTO.getStudentNumber());
        if (existUser != null) {
            throw new BusinessException(MessageConstants.USER_EXISTS);
        }

        // 创建用户
        User user = User.builder()
                .username(registerDTO.getStudentNumber())
                .password(DigestUtil.md5Hex(registerDTO.getPassword()))
                .realName(registerDTO.getRealName())
                .role(RoleConstants.ROLE_STUDENT)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);

        // 创建学生信息
        Student student = Student.builder()
                .userId(user.getUserId())
                .studentNumber(registerDTO.getStudentNumber())
                .adminClass(registerDTO.getAdminClass())
                .gender(registerDTO.getGender())
                .build();
        studentMapper.insert(student);

        log.info("学生注册成功: {}", registerDTO.getStudentNumber());
    }

    @Override
    public StudentVO getCurrentStudentInfo() {
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.findById(userId);
        Student student = studentMapper.findByUserId(userId);

        if (user == null || student == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }

        return StudentVO.builder()
                .userId(user.getUserId())
                .studentNumber(student.getStudentNumber())
                .realName(user.getRealName())
                .adminClass(student.getAdminClass())
                .gender(student.getGender())
                .avatarUrl(user.getAvatarUrl())
                .hasFaceFeature(student.getFeatureVector() != null && !student.getFeatureVector().isEmpty())
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
    public void registerFaceByImage(String avatarUrl) {
        Long userId = BaseContext.getCurrentId();

        // 获取图片访问 URL
        String imageUrl = minioService.getFileUrl(avatarUrl);

        // 调用 Python 服务提取人脸特征
        String featureVector = pythonServiceClient.extractFaceFeature(imageUrl);
        if (featureVector == null || featureVector.isEmpty()) {
            throw new BusinessException("人脸特征提取失败，请确保图片中有清晰的人脸");
        }

        // AES 加密存储
        String encryptedFeature = AesUtils.encrypt(featureVector);
        studentMapper.updateFeatureVector(userId, encryptedFeature);

        // 更新学生的人脸图片路径
        studentMapper.updateFaceImageKey(userId, avatarUrl);

        log.info("学生 {} 通过图片登记人脸成功", userId);
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

