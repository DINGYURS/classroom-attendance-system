package com.project.backend.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceRecordMapper;
import com.project.backend.mapper.AttendanceNoticeMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.dto.TeacherStudentPageQueryDTO;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentNoticeVO;
import com.project.backend.pojo.vo.StudentVO;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import com.project.backend.service.MinioService;
import com.project.backend.service.PythonServiceClient;
import com.project.backend.service.StudentService;
import com.project.backend.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生相关服务实现。
 */
@Slf4j
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceNoticeMapper attendanceNoticeMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private MinioService minioService;

    @Autowired
    private PythonServiceClient pythonServiceClient;

    @Override
    public StudentVO getCurrentStudentInfo() {
        Long userId = validateCurrentStudent();
        User user = userMapper.findById(userId);
        Student student = studentMapper.findByUserId(userId);

        if (user == null || student == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }

        String avatarUrl = null;
        if (student.getAvatarObjectKey() != null) {
            avatarUrl = minioService.getFileUrl(student.getAvatarObjectKey());
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
        Long userId = validateCurrentStudent();
        String encryptedFeature = AesUtils.encrypt(faceFeatureDTO.getFeatureVector());
        studentMapper.updateFeatureVector(userId, encryptedFeature);

        log.info("学生 {} 上传人脸特征成功", userId);
    }

    @Override
    public void registerFaceByImage(MultipartFile file) {
        Long userId = validateCurrentStudent();

        String objectKey = minioService.uploadFile(file, "faces");
        log.info("学生 {} 上传人脸图片成功: {}", userId, objectKey);

        String featureVector = pythonServiceClient.extractFaceFeature(objectKey);
        if (featureVector == null || featureVector.isEmpty()) {
            minioService.deleteFile(objectKey);
            throw new BusinessException("人脸特征提取失败，请确保图片中有清晰的人脸");
        }

        String encryptedFeature = AesUtils.encrypt(featureVector);
        studentMapper.updateFeatureVector(userId, encryptedFeature);
        studentMapper.updateFaceImageKey(userId, objectKey);

        log.info("学生 {} 完成人脸注册", userId);
    }

    @Override
    public List<AttendanceRecordVO> getAttendanceRecords() {
        Long userId = validateCurrentStudent();
        List<AttendanceRecordVO> result = attendanceRecordMapper.findAttendanceDetailsByStudentId(userId);
        result.forEach(record -> {
            record.setStatusText(getStatusText(record.getStatus()));
            record.setManualModified(Integer.valueOf(2).equals(record.getUpdateType()));
        });
        return result;
    }

    @Override
    public List<AttendanceRecordVO> getTeacherStudentAttendanceRecords(Long courseId, Long studentId) {
        if (courseId == null || studentId == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        Long teacherId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(teacherId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        var course = courseMapper.findById(courseId);
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        boolean studentInCourse = courseStudentMapper.findByCourseId(courseId).stream()
                .anyMatch(item -> studentId.equals(item.getStudentId()));
        if (!studentInCourse) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        List<AttendanceRecordVO> records = attendanceRecordMapper.findByCourseIdAndStudentId(courseId, studentId);
        records.forEach(record -> record.setStatusText(getStatusText(record.getStatus())));
        return records;
    }

    @Override
    public PageResult<TeacherStudentTableVO> getTeacherStudentPage(TeacherStudentPageQueryDTO queryDTO) {
        Long teacherId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(teacherId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        int currentPage = queryDTO.getCurrentPage() == null || queryDTO.getCurrentPage() < 1 ? 1 : queryDTO.getCurrentPage();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : Math.min(queryDTO.getPageSize(), 100);
        String keyword = normalizeKeyword(queryDTO.getKeyword());

        try (Page<TeacherStudentTableVO> page = PageHelper.startPage(currentPage, pageSize)) {
            List<TeacherStudentTableVO> records = courseStudentMapper.pageTeacherStudents(teacherId, keyword);
            records.forEach(this::fillTeacherStudentAvatarUrl);

            return PageResult.<TeacherStudentTableVO>builder()
                    .total(page.getTotal())
                    .records(records)
                    .build();
        }
    }

    @Override
    public List<StudentNoticeVO> getNoticeList(Integer status) {
        Long studentId = validateCurrentStudent();
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }
        return attendanceNoticeMapper.findByStudentId(studentId, status);
    }

    @Override
    public Integer countUnreadNotices() {
        Long studentId = validateCurrentStudent();
        Integer unreadCount = attendanceNoticeMapper.countUnreadByStudentId(studentId);
        return unreadCount == null ? 0 : unreadCount;
    }

    @Override
    public void markNoticeRead(Long noticeId) {
        if (noticeId == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        Long studentId = validateCurrentStudent();

        var notice = attendanceNoticeMapper.findById(noticeId);
        if (notice == null || !studentId.equals(notice.getStudentId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        attendanceNoticeMapper.markRead(noticeId);
    }

    /**
     * 将学生头像对象键转换为可直接访问的 MinIO URL。
     */
    private void fillTeacherStudentAvatarUrl(TeacherStudentTableVO record) {
        if (record == null || record.getAvatarUrl() == null || record.getAvatarUrl().isBlank()) {
            return;
        }

        String objectKey = record.getAvatarUrl();
        try {
            record.setAvatarUrl(minioService.getFileUrl(objectKey));
        } catch (Exception e) {
            log.warn("获取教师端学生头像失败，忽略头像展示: userId={}, objectKey={}",
                    record.getUserId(), objectKey, e);
            record.setAvatarUrl(null);
        }
    }

    /**
     * 规范化关键字，空白字符串按 null 处理。
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 校验当前登录用户是否为学生，并返回学生 ID。
     */
    private Long validateCurrentStudent() {
        Long studentId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(studentId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_STUDENT.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return studentId;
    }

    /**
     * 转换考勤状态文本。
     */
    private String getStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "缺勤";
            case 1 -> "已到";
            case 2 -> "迟到";
            case 3 -> "请假";
            default -> "未知";
        };
    }
}
