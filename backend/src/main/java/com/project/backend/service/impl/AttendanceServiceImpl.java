package com.project.backend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.project.backend.constant.AttendanceStatus;
import com.project.backend.constant.MessageConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceRecordMapper;
import com.project.backend.mapper.AttendanceSessionMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.entity.AttendanceRecord;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.AttendanceSessionVO;
import com.project.backend.pojo.vo.RecognitionResultVO;
import com.project.backend.pojo.vo.SessionRecordVO;
import com.project.backend.service.AttendanceService;
import com.project.backend.service.MinioService;
import com.project.backend.service.PythonServiceClient;
import com.project.backend.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 考勤服务实现类
 */
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final double SIMILARITY_THRESHOLD = 0.6;

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

    @Autowired
    private MinioService minioService;

    @Autowired
    private PythonServiceClient pythonServiceClient;

    @Override
    @Transactional
    public Long startAttendance(AttendanceStartDTO startDTO) {
        Long teacherId = BaseContext.getCurrentId();

        Course course = courseMapper.findById(startDTO.getCourseId());
        if (course == null || !course.getTeacherId().equals(teacherId)) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(startDTO.getCourseId());
        if (studentIds == null) {
            studentIds = new ArrayList<>();
        }

        AttendanceSession session = AttendanceSession.builder()
                .courseId(startDTO.getCourseId())
                .sourceImages(JSON.toJSONString(new ArrayList<>()))
                .totalStudent(studentIds.size())
                .actualStudent(0)
                .startTime(LocalDateTime.now())
                .build();
        attendanceSessionMapper.insert(session);

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

        log.info("点名开始: sessionId={}, courseId={}, studentCount={}",
                session.getSessionId(), startDTO.getCourseId(), studentIds.size());
        return session.getSessionId();
    }

    @Override
    public void endAttendance(Long sessionId) {
        AttendanceSession session = attendanceSessionMapper.findById(sessionId);
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(sessionId);
        session.setActualStudent(countActualStudents(records));
        attendanceSessionMapper.update(session);

        log.info("点名结束: sessionId={}", sessionId);
    }

    @Override
    @Transactional
    public List<RecognitionResultVO> recognizeFaces(FaceRecognitionDTO recognitionDTO) {
        AttendanceSession session = attendanceSessionMapper.findById(recognitionDTO.getSessionId());
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }
        if (recognitionDTO.getImageKeys() == null || recognitionDTO.getImageKeys().isEmpty()) {
            throw new BusinessException("未提供合照图片");
        }

        List<String> imageUrls = new ArrayList<>();
        for (String key : recognitionDTO.getImageKeys()) {
            imageUrls.add(minioService.getFileUrl(key));
        }

        List<List<Double>> detectedEmbeddings = pythonServiceClient.detectFaces(imageUrls);
        log.info("合照检测到人脸数: {}", detectedEmbeddings.size());

        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(session.getCourseId());
        if (studentIds == null) {
            studentIds = new ArrayList<>();
        }
        List<Student> students = studentIds.isEmpty()
                ? new ArrayList<>()
                : studentMapper.findByUserIds(studentIds);

        List<RecognitionResultVO> results = new ArrayList<>();
        Set<Long> matchedStudentIds = new HashSet<>();

        for (List<Double> detectedEmbedding : detectedEmbeddings) {
            double[] inputFeature = toDoubleArray(detectedEmbedding);
            RecognitionResultVO bestMatch = null;
            double bestSimilarity = 0;

            for (Student student : students) {
                if (matchedStudentIds.contains(student.getUserId())) {
                    continue;
                }
                if (student.getFeatureVector() == null || student.getFeatureVector().isEmpty()) {
                    continue;
                }

                String decryptedJson = AesUtils.decrypt(student.getFeatureVector());
                double[] storedFeature = parseFeatureVector(decryptedJson);
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
                matchedStudentIds.add(bestMatch.getStudentId());
                bestMatch.setStatus(AttendanceStatus.PRESENT);

                attendanceRecordMapper.updateStatus(
                        recognitionDTO.getSessionId(),
                        bestMatch.getStudentId(),
                        AttendanceStatus.PRESENT,
                        BigDecimal.valueOf(bestMatch.getSimilarity())
                );
                results.add(bestMatch);
                continue;
            }

            results.add(RecognitionResultVO.builder()
                    .matched(false)
                    .similarity(0.0)
                    .build());
        }

        session.setSourceImages(JSON.toJSONString(recognitionDTO.getImageKeys()));
        session.setActualStudent(matchedStudentIds.size());
        session.setTotalStudent(studentIds.size());
        attendanceSessionMapper.update(session);

        log.info("识别完成: totalFaces={}, matchedCount={}", detectedEmbeddings.size(), matchedStudentIds.size());
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

        return AttendanceSessionVO.builder()
                .sessionId(session.getSessionId())
                .courseId(session.getCourseId())
                .courseName(course != null ? course.getCourseName() : "")
                .startTime(session.getStartTime())
                .presentCount(countActualStudents(records))
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
            result.add(AttendanceSessionVO.builder()
                    .sessionId(session.getSessionId())
                    .courseId(session.getCourseId())
                    .courseName(course != null ? course.getCourseName() : "")
                    .startTime(session.getStartTime())
                    .presentCount(countActualStudents(records))
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

        AttendanceSession session = attendanceSessionMapper.findById(record.getSessionId());
        if (session != null) {
            List<AttendanceRecord> sessionRecords = attendanceRecordMapper.findBySessionId(record.getSessionId());
            session.setActualStudent(countActualStudents(sessionRecords));
            attendanceSessionMapper.update(session);
        }

        log.info("考勤状态已更新: recordId={}, status={}", updateDTO.getRecordId(), updateDTO.getStatus());
    }

    /**
     * 将 List<Double> 转为 double[]
     */
    private double[] toDoubleArray(List<Double> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
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
     * 统计实到人数。
     */
    private int countActualStudents(List<AttendanceRecord> records) {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (record.getStatus() != null
                    && (record.getStatus().equals(AttendanceStatus.PRESENT)
                    || record.getStatus().equals(AttendanceStatus.LATE))) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取状态文本。
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
