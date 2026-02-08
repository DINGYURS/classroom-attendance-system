package com.project.backend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.project.backend.constant.AttendanceStatus;
import com.project.backend.constant.MessageConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.*;
import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.entity.*;
import com.project.backend.pojo.vo.AttendanceSessionVO;
import com.project.backend.pojo.vo.RecognitionResultVO;
import com.project.backend.pojo.vo.SessionRecordVO;
import com.project.backend.service.AttendanceService;
import com.project.backend.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 考勤服务实现类
 */
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final double SIMILARITY_THRESHOLD = 0.6; // 相似度阈值

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Override
    @Transactional
    public Long startAttendance(AttendanceStartDTO startDTO) {
        Long teacherId = BaseContext.getCurrentId();

        // 验证课程所有权
        Course course = courseMapper.findById(startDTO.getCourseId());
        if (course == null || !course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        // 创建考勤会话
        AttendanceSession session = AttendanceSession.builder()
                .courseId(startDTO.getCourseId())
                .teacherId(teacherId)
                .startTime(LocalDateTime.now())
                .lateThreshold(startDTO.getLateThreshold() != null ? startDTO.getLateThreshold() : 10)
                .status(0) // 进行中
                .build();
        attendanceSessionMapper.insert(session);

        // 为课程所有学生创建初始考勤记录（状态：缺勤）
        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(startDTO.getCourseId());
        if (!studentIds.isEmpty()) {
            List<AttendanceRecord> records = new ArrayList<>();
            for (Long studentId : studentIds) {
                records.add(AttendanceRecord.builder()
                        .sessionId(session.getSessionId())
                        .studentId(studentId)
                        .status(AttendanceStatus.ABSENT)
                        .build());
            }
            attendanceRecordMapper.batchInsert(records);
        }

        log.info("点名开始: 会话ID={}, 课程ID={}, 学生数={}", session.getSessionId(), startDTO.getCourseId(), studentIds.size());
        return session.getSessionId();
    }

    @Override
    public void endAttendance(Long sessionId) {
        Long teacherId = BaseContext.getCurrentId();
        AttendanceSession session = attendanceSessionMapper.findById(sessionId);

        if (session == null || !session.getTeacherId().equals(teacherId)) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        session.setStatus(1); // 已结束
        session.setEndTime(LocalDateTime.now());
        attendanceSessionMapper.update(session);

        log.info("点名结束: 会话ID={}", sessionId);
    }

    @Override
    @Transactional
    public List<RecognitionResultVO> recognizeFaces(FaceRecognitionDTO recognitionDTO) {
        AttendanceSession session = attendanceSessionMapper.findById(recognitionDTO.getSessionId());
        if (session == null || session.getStatus() == 1) {
            throw new BusinessException("考勤会话不存在或已结束");
        }

        // 获取课程学生及其特征向量
        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(session.getCourseId());
        List<Student> students = studentMapper.findByUserIds(studentIds);

        List<RecognitionResultVO> results = new ArrayList<>();

        // 遍历上传的人脸特征进行比对
        for (String featureVector : recognitionDTO.getFeatureVectors()) {
            double[] inputFeature = parseFeatureVector(featureVector);
            RecognitionResultVO bestMatch = null;
            double bestSimilarity = 0;

            for (Student student : students) {
                if (student.getFeatureVector() == null || student.getFeatureVector().isEmpty()) {
                    continue;
                }

                // 解密并解析存储的特征向量
                String decryptedFeature = AesUtils.decrypt(student.getFeatureVector());
                double[] storedFeature = parseFeatureVector(decryptedFeature);

                // 计算余弦相似度
                double similarity = cosineSimilarity(inputFeature, storedFeature);

                if (similarity > SIMILARITY_THRESHOLD && similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    User user = userMapper.findById(student.getUserId());
                    bestMatch = RecognitionResultVO.builder()
                            .studentId(student.getUserId())
                            .studentNumber(student.getStudentNumber())
                            .realName(user != null ? user.getRealName() : "未知")
                            .similarity(similarity)
                            .matched(true)
                            .build();
                }
            }

            if (bestMatch != null) {
                // 判断迟到
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime lateTime = session.getStartTime().plusMinutes(session.getLateThreshold());
                int status = now.isAfter(lateTime) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
                bestMatch.setStatus(status);

                // 更新考勤记录
                attendanceRecordMapper.updateStatus(
                        recognitionDTO.getSessionId(),
                        bestMatch.getStudentId(),
                        status,
                        BigDecimal.valueOf(bestMatch.getSimilarity())
                );

                results.add(bestMatch);
            } else {
                // 未识别到
                results.add(RecognitionResultVO.builder()
                        .matched(false)
                        .similarity(0.0)
                        .build());
            }
        }

        return results;
    }

    @Override
    public AttendanceSessionVO getSessionDetail(Long sessionId) {
        AttendanceSession session = attendanceSessionMapper.findById(sessionId);
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        Course course = courseMapper.findById(session.getCourseId());
        List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(sessionId);

        int presentCount = 0;
        for (AttendanceRecord record : records) {
            if (record.getStatus() != null && record.getStatus() != AttendanceStatus.ABSENT) {
                presentCount++;
            }
        }

        return AttendanceSessionVO.builder()
                .sessionId(session.getSessionId())
                .courseId(session.getCourseId())
                .courseName(course != null ? course.getCourseName() : "")
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .status(session.getStatus())
                .presentCount(presentCount)
                .totalCount(records.size())
                .build();
    }

    @Override
    public List<SessionRecordVO> getSessionRecords(Long sessionId) {
        List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(sessionId);
        List<SessionRecordVO> result = new ArrayList<>();

        for (AttendanceRecord record : records) {
            Student student = studentMapper.findByUserId(record.getStudentId());
            User user = userMapper.findById(record.getStudentId());

            result.add(SessionRecordVO.builder()
                    .recordId(record.getRecordId())
                    .studentId(record.getStudentId())
                    .studentNumber(student != null ? student.getStudentNumber() : "")
                    .realName(user != null ? user.getRealName() : "")
                    .status(record.getStatus())
                    .statusText(getStatusText(record.getStatus()))
                    .similarityScore(record.getSimilarityScore())
                    .build());
        }

        return result;
    }

    @Override
    public List<AttendanceSessionVO> getCourseAttendanceHistory(Long courseId) {
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        Course course = courseMapper.findById(courseId);

        List<AttendanceSessionVO> result = new ArrayList<>();
        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(session.getSessionId());
            int presentCount = 0;
            for (AttendanceRecord record : records) {
                if (record.getStatus() != null && record.getStatus() != AttendanceStatus.ABSENT) {
                    presentCount++;
                }
            }

            result.add(AttendanceSessionVO.builder()
                    .sessionId(session.getSessionId())
                    .courseId(session.getCourseId())
                    .courseName(course != null ? course.getCourseName() : "")
                    .startTime(session.getStartTime())
                    .endTime(session.getEndTime())
                    .status(session.getStatus())
                    .presentCount(presentCount)
                    .totalCount(records.size())
                    .build());
        }

        return result;
    }

    @Override
    public void updateAttendanceStatus(AttendanceUpdateDTO updateDTO) {
        AttendanceRecord record = attendanceRecordMapper.findById(updateDTO.getRecordId());
        if (record == null) {
            throw new BusinessException("考勤记录不存在");
        }

        record.setStatus(updateDTO.getStatus());
        attendanceRecordMapper.update(record);

        log.info("考勤状态已更新: recordId={}, status={}", updateDTO.getRecordId(), updateDTO.getStatus());
    }

    /**
     * 解析特征向量 JSON 字符串为 double 数组
     */
    private double[] parseFeatureVector(String featureVector) {
        try {
            List<Double> list = JSON.parseArray(featureVector, Double.class);
            double[] result = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
            return result;
        } catch (Exception e) {
            log.error("特征向量解析失败: {}", e.getMessage());
            return new double[0];
        }
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(double[] vec1, double[] vec2) {
        if (vec1.length != vec2.length || vec1.length == 0) {
            return 0;
        }

        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;

        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
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
