package com.project.backend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.project.backend.constant.AttendanceStatus;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceDetectionMapper;
import com.project.backend.mapper.AttendanceRecordMapper;
import com.project.backend.mapper.AttendanceSessionMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.AttendanceArchiveQueryDTO;
import com.project.backend.pojo.dto.AttendanceDetectionAssignDTO;
import com.project.backend.pojo.dto.AttendanceDetectionIgnoreDTO;
import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.FaceDetectFaceDTO;
import com.project.backend.pojo.dto.FaceDetectImageResultDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.entity.AttendanceDetection;
import com.project.backend.pojo.entity.AttendanceRecord;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.vo.AttendanceArchiveCourseSummaryExportVO;
import com.project.backend.pojo.vo.AttendanceArchiveDetailVO;
import com.project.backend.pojo.vo.AttendanceArchiveExportVO;
import com.project.backend.pojo.vo.AttendanceArchiveOptionsVO;
import com.project.backend.pojo.vo.AttendanceArchivePageVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionDetailVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionExportVO;
import com.project.backend.pojo.vo.AttendanceDetectionVO;
import com.project.backend.pojo.vo.AttendanceSessionAnnotationVO;
import com.project.backend.pojo.vo.AttendanceSessionImageVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionVO;
import com.project.backend.pojo.vo.AttendanceArchiveSummaryVO;
import com.project.backend.pojo.vo.AttendanceSessionVO;
import com.project.backend.pojo.vo.RecognitionResultVO;
import com.project.backend.pojo.vo.SessionRecordVO;
import com.project.backend.pojo.vo.StatisticsOptionVO;
import com.project.backend.service.AttendanceService;
import com.project.backend.service.MinioService;
import com.project.backend.service.PythonServiceClient;
import com.project.backend.utils.AesUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 考勤服务实现类
 */
@Slf4j
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final double SIMILARITY_THRESHOLD = 0.6;
    private static final Integer ARCHIVE_TYPE_AUTO = 1;
    private static final Integer ARCHIVE_TYPE_MANUAL = 2;
    private static final DateTimeFormatter ARCHIVE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ARCHIVE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private AttendanceDetectionMapper attendanceDetectionMapper;

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

        List<FaceDetectImageResultDTO> detectResults = pythonServiceClient.detectFaces(imageUrls);
        int totalDetectedFaces = detectResults.stream()
                .map(FaceDetectImageResultDTO::getFaces)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
        log.info("合照检测到人脸数: {}", totalDetectedFaces);

        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(session.getCourseId());
        if (studentIds == null) {
            studentIds = new ArrayList<>();
        }
        List<Student> students = studentIds.isEmpty()
                ? new ArrayList<>()
                : studentMapper.findByUserIds(studentIds);

        Map<Long, double[]> studentFeatureMap = new HashMap<>();
        int invalidFeatureCount = 0;
        for (Student student : students) {
            if (student.getFeatureVector() == null || student.getFeatureVector().isEmpty()) {
                continue;
            }

            double[] storedFeature = loadStudentFeatureVector(student);
            if (storedFeature.length == 0) {
                invalidFeatureCount++;
                continue;
            }
            studentFeatureMap.put(student.getUserId(), storedFeature);
        }
        log.info("点名特征预处理完成: totalStudents={}, validFeatures={}, invalidFeatures={}",
                students.size(), studentFeatureMap.size(), invalidFeatureCount);

        attendanceRecordMapper.resetBySessionId(recognitionDTO.getSessionId());
        attendanceDetectionMapper.deleteBySessionId(recognitionDTO.getSessionId());

        Map<Long, AttendanceRecord> recordMap = attendanceRecordMapper.findBySessionId(recognitionDTO.getSessionId()).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, record -> record, (left, right) -> left, LinkedHashMap::new));

        List<RecognitionResultVO> results = new ArrayList<>();
        List<AttendanceDetection> detections = new ArrayList<>();
        Set<Long> matchedStudentIds = new HashSet<>();

        for (FaceDetectImageResultDTO imageResult : detectResults) {
            List<FaceDetectFaceDTO> faces = imageResult.getFaces();
            if (faces == null || faces.isEmpty()) {
                continue;
            }

            for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
                FaceDetectFaceDTO face = faces.get(faceIndex);
                List<Double> embedding = face.getEmbedding();
                String bboxJson = JSON.toJSONString(face.getBbox() == null ? List.of() : face.getBbox());
                BigDecimal detectionScore = face.getDetScore() == null ? null : BigDecimal.valueOf(face.getDetScore());

                if (embedding == null || embedding.isEmpty()) {
                    results.add(RecognitionResultVO.builder()
                            .matched(false)
                            .similarity(0.0)
                            .build());
                    detections.add(AttendanceDetection.builder()
                            .sessionId(recognitionDTO.getSessionId())
                            .imageIndex(imageResult.getImageIndex())
                            .faceIndex(faceIndex)
                            .bbox(bboxJson)
                            .detectionScore(detectionScore)
                            .matched(false)
                            .build());
                    continue;
                }

                double[] inputFeature = toDoubleArray(embedding);
                RecognitionResultVO bestMatch = null;
                double bestSimilarity = 0;

                for (Student student : students) {
                    double[] storedFeature = studentFeatureMap.get(student.getUserId());
                    if (storedFeature == null || storedFeature.length == 0) {
                        continue;
                    }
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
                    boolean firstMatch = matchedStudentIds.add(bestMatch.getStudentId());
                    bestMatch.setStatus(AttendanceStatus.PRESENT);
                    BigDecimal matchSimilarity = BigDecimal.valueOf(bestMatch.getSimilarity());

                    AttendanceRecord matchedRecord = recordMap.get(bestMatch.getStudentId());
                    if (matchedRecord != null) {
                        BigDecimal currentSimilarity = matchedRecord.getSimilarityScore();
                        boolean betterEvidence = currentSimilarity == null || matchSimilarity.compareTo(currentSimilarity) > 0;
                        if (firstMatch || betterEvidence) {
                            attendanceRecordMapper.updateStatus(
                                    recognitionDTO.getSessionId(),
                                    bestMatch.getStudentId(),
                                    AttendanceStatus.PRESENT,
                                    matchSimilarity,
                                    bboxJson
                            );

                            matchedRecord.setStatus(AttendanceStatus.PRESENT);
                            matchedRecord.setSimilarityScore(matchSimilarity);
                            matchedRecord.setFaceLocation(bboxJson);
                            matchedRecord.setUpdateType(1);
                        }
                    }

                    detections.add(AttendanceDetection.builder()
                            .sessionId(recognitionDTO.getSessionId())
                            .imageIndex(imageResult.getImageIndex())
                            .faceIndex(faceIndex)
                            .bbox(bboxJson)
                            .detectionScore(detectionScore)
                            .matched(true)
                            .studentId(bestMatch.getStudentId())
                            .recordId(matchedRecord != null ? matchedRecord.getRecordId() : null)
                            .similarityScore(matchSimilarity)
                            .build());
                    results.add(bestMatch);
                    continue;
                }

                detections.add(AttendanceDetection.builder()
                        .sessionId(recognitionDTO.getSessionId())
                        .imageIndex(imageResult.getImageIndex())
                        .faceIndex(faceIndex)
                        .bbox(bboxJson)
                        .detectionScore(detectionScore)
                        .matched(false)
                        .build());

                results.add(RecognitionResultVO.builder()
                        .matched(false)
                        .similarity(0.0)
                        .build());
            }
        }

        if (!detections.isEmpty()) {
            attendanceDetectionMapper.batchInsert(detections);
        }

        session.setSourceImages(JSON.toJSONString(recognitionDTO.getImageKeys()));
        session.setActualStudent(matchedStudentIds.size());
        session.setTotalStudent(studentIds.size());
        attendanceSessionMapper.update(session);

        log.info("识别完成: totalFaces={}, matchedCount={}", totalDetectedFaces, matchedStudentIds.size());
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
    public AttendanceSessionAnnotationVO getSessionAnnotations(Long sessionId) {
        Long teacherId = validateCurrentTeacher();
        AttendanceSession session = attendanceSessionMapper.findById(sessionId);
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        Course course = courseMapper.findById(session.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        List<String> imageKeys = parseSourceImages(session.getSourceImages());
        List<AttendanceSessionImageVO> imageList = new ArrayList<>();
        for (int index = 0; index < imageKeys.size(); index++) {
            String objectKey = imageKeys.get(index);
            imageList.add(AttendanceSessionImageVO.builder()
                    .imageIndex(index)
                    .viewKey(resolveViewKey(index))
                    .objectKey(objectKey)
                    .imageUrl(StringUtils.hasText(objectKey) ? minioService.getFileUrl(objectKey) : "")
                    .build());
        }

        List<AttendanceDetection> detections = attendanceDetectionMapper.findBySessionId(sessionId);
        Map<Long, Student> studentMap = loadStudentMap(detections.stream()
                .map(AttendanceDetection::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, User> userMap = loadUserMap(studentMap.keySet());
        Map<Long, AttendanceRecord> recordMap = attendanceRecordMapper.findBySessionId(sessionId).stream()
                .collect(Collectors.toMap(AttendanceRecord::getRecordId, record -> record, (left, right) -> left, LinkedHashMap::new));

        List<AttendanceDetectionVO> detectionList = detections.stream()
                .map(detection -> buildDetectionVO(detection, studentMap, userMap, recordMap))
                .toList();

        return AttendanceSessionAnnotationVO.builder()
                .sessionId(sessionId)
                .images(imageList)
                .detections(detectionList)
                .build();
    }

    @Override
    @Transactional
    public void ignoreDetection(Long detectionId, AttendanceDetectionIgnoreDTO ignoreDTO) {
        AttendanceDetection detection = validateDetectionPermission(detectionId);
        if (Boolean.TRUE.equals(detection.getMatched())) {
            throw new BusinessException("已匹配检测框不能直接忽略");
        }

        String reason = ignoreDTO == null ? "" : defaultText(ignoreDTO.getIgnoreReason());
        attendanceDetectionMapper.markIgnored(detectionId, StringUtils.hasText(reason) ? reason : "教师确认忽略");
        log.info("检测框已忽略: detectionId={}, sessionId={}", detectionId, detection.getSessionId());
    }

    @Override
    @Transactional
    public void assignDetection(Long detectionId, AttendanceDetectionAssignDTO assignDTO) {
        if (assignDTO == null || assignDTO.getStudentId() == null) {
            throw new BusinessException("请选择要指派的学生");
        }

        AttendanceDetection detection = validateDetectionPermission(detectionId);
        if (Boolean.TRUE.equals(detection.getMatched())) {
            throw new BusinessException("该检测框已匹配学生");
        }

        AttendanceSession session = attendanceSessionMapper.findById(detection.getSessionId());
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        Long studentId = assignDTO.getStudentId();
        List<Long> courseStudentIds = courseStudentMapper.findStudentIdsByCourseId(session.getCourseId());
        if (courseStudentIds == null || !courseStudentIds.contains(studentId)) {
            throw new BusinessException("该学生不属于当前课程");
        }

        AttendanceRecord targetRecord = attendanceRecordMapper.findBySessionId(session.getSessionId()).stream()
                .filter(record -> studentId.equals(record.getStudentId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("学生考勤记录不存在"));

        AttendanceDetection existingDetection = attendanceDetectionMapper.findActiveByRecordId(targetRecord.getRecordId());
        if (existingDetection != null && !existingDetection.getDetectionId().equals(detectionId)) {
            throw new BusinessException("该学生已有绑定的检测框");
        }

        targetRecord.setStatus(AttendanceStatus.PRESENT);
        targetRecord.setSimilarityScore(null);
        targetRecord.setFaceLocation(detection.getBbox());
        targetRecord.setUpdateType(2);
        attendanceRecordMapper.update(targetRecord);
        attendanceDetectionMapper.assignStudent(detectionId, studentId, targetRecord.getRecordId());

        List<AttendanceRecord> sessionRecords = attendanceRecordMapper.findBySessionId(session.getSessionId());
        session.setActualStudent(countActualStudents(sessionRecords));
        attendanceSessionMapper.update(session);

        log.info("检测框已人工指派: detectionId={}, studentId={}, recordId={}",
                detectionId, studentId, targetRecord.getRecordId());
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
                    .faceLocation(record.getFaceLocation())
                    .manualModified(Integer.valueOf(2).equals(record.getUpdateType()))
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
        record.setUpdateType(2);
        attendanceRecordMapper.update(record);

        AttendanceSession session = attendanceSessionMapper.findById(record.getSessionId());
        if (session != null) {
            List<AttendanceRecord> sessionRecords = attendanceRecordMapper.findBySessionId(record.getSessionId());
            session.setActualStudent(countActualStudents(sessionRecords));
            attendanceSessionMapper.update(session);
        }

        log.info("考勤状态已更新: recordId={}, status={}", updateDTO.getRecordId(), updateDTO.getStatus());
    }

    @Override
    public AttendanceArchiveOptionsVO getArchiveOptions(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        List<Course> teacherCourses = courseMapper.findByTeacherId(teacherId);
        if (teacherCourses == null) {
            teacherCourses = new ArrayList<>();
        }
        List<Course> selectedCourses = filterTeacherCourses(teacherCourses, courseId);

        List<StatisticsOptionVO> courseOptions = teacherCourses.stream()
                .map(course -> StatisticsOptionVO.builder()
                        .label(buildCourseOptionLabel(course))
                        .value(String.valueOf(course.getCourseId()))
                        .build())
                .toList();

        Set<String> classSet = new LinkedHashSet<>();
        for (Course course : selectedCourses) {
            List<String> adminClasses = studentMapper.findAdminClassesByCourseId(course.getCourseId());
            if (adminClasses != null) {
                classSet.addAll(adminClasses.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .sorted()
                        .toList());
            }
        }

        List<StatisticsOptionVO> classOptions = classSet.stream()
                .map(adminClass -> StatisticsOptionVO.builder()
                        .label(adminClass)
                        .value(adminClass)
                        .build())
                .toList();

        return AttendanceArchiveOptionsVO.builder()
                .courseOptions(courseOptions)
                .classOptions(classOptions)
                .build();
    }

    @Override
    public AttendanceArchivePageVO getArchivePage(AttendanceArchiveQueryDTO queryDTO) {
        AttendanceArchiveQueryDTO safeQuery = normalizeArchiveQuery(queryDTO);
        Long teacherId = validateCurrentTeacher();
        AttendanceArchiveSummaryVO summary = attendanceSessionMapper.getArchiveSummary(teacherId, safeQuery);
        if (summary == null) {
            summary = AttendanceArchiveSummaryVO.builder()
                    .totalSessions(0)
                    .expectedTotal(0)
                    .actualTotal(0)
                    .absentTotal(0)
                    .lateTotal(0)
                    .build();
        }
        summary.setAvgRate(formatRate(
                defaultArchiveCount(summary.getActualTotal()),
                defaultArchiveCount(summary.getExpectedTotal())
        ));

        int currentPage = getCurrentPage(safeQuery);
        int pageSize = getPageSize(safeQuery);
        Page<Long> page = PageHelper.startPage(currentPage, pageSize);
        List<Long> sessionIds = attendanceSessionMapper.pageArchiveSessionIds(teacherId, safeQuery);
        List<AttendanceArchiveSessionVO> pageRecords = new ArrayList<>();
        if (sessionIds != null && !sessionIds.isEmpty()) {
            Map<Long, AttendanceArchiveSessionVO> sessionMap = attendanceSessionMapper.listArchiveSessionsByIds(sessionIds)
                    .stream()
                    .collect(Collectors.toMap(AttendanceArchiveSessionVO::getId,
                            session -> session,
                            (left, right) -> left,
                            LinkedHashMap::new));
            for (Long sessionId : sessionIds) {
                AttendanceArchiveSessionVO session = sessionMap.get(sessionId);
                if (session != null) {
                    pageRecords.add(session);
                }
            }
        }

        return AttendanceArchivePageVO.builder()
                .summary(summary)
                .pageData(PageResult.<AttendanceArchiveSessionVO>builder()
                        .total(page.getTotal())
                        .records(pageRecords)
                        .build())
                .build();
    }

    @Override
    public AttendanceArchiveSessionDetailVO getArchiveSessionDetail(Long sessionId) {
        Long teacherId = validateCurrentTeacher();
        AttendanceSession session = attendanceSessionMapper.findById(sessionId);
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        Course course = courseMapper.findById(session.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(sessionId);
        Map<Long, Student> studentMap = loadStudentMap(records.stream()
                .map(AttendanceRecord::getStudentId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        Map<Long, User> userMap = loadUserMap(studentMap.keySet());

        List<AttendanceArchiveDetailVO> detailList = records.stream()
                .sorted(Comparator.comparing(record -> {
                    Student student = studentMap.get(record.getStudentId());
                    return student != null && student.getStudentNumber() != null ? student.getStudentNumber() : "";
                }))
                .map(record -> buildArchiveDetailVO(record, studentMap, userMap))
                .toList();

        return AttendanceArchiveSessionDetailVO.builder()
                .sessionId(session.getSessionId())
                .courseName(course.getCourseName())
                .className(buildSessionClassName(records, studentMap))
                .sessionTime(formatDateTime(session.getStartTime()))
                .expectedCount(resolveExpectedCount(session, records))
                .actualCount(countActualStudents(records))
                .absentCount(countByStatus(records, AttendanceStatus.ABSENT))
                .lateCount(countByStatus(records, AttendanceStatus.LATE))
                .leaveCount(countByStatus(records, AttendanceStatus.LEAVE))
                .attendanceRate(formatRate(countActualStudents(records), resolveExpectedCount(session, records)))
                .type(getSessionTypeText(records))
                .detailList(detailList)
                .build();
    }

    @Override
    public List<AttendanceArchiveExportVO> listArchiveExportData(AttendanceArchiveQueryDTO queryDTO) {
        ArchiveContext context = buildArchiveContext(normalizeArchiveQuery(queryDTO));
        List<AttendanceArchiveExportVO> result = new ArrayList<>();

        for (AttendanceSession session : context.matchedSessions()) {
            Course course = context.courseMap().get(session.getCourseId());
            List<AttendanceRecord> displayRecords = context.displayRecordsBySession()
                    .getOrDefault(session.getSessionId(), List.of());
            for (AttendanceRecord record : displayRecords) {
                AttendanceArchiveDetailVO detailVO = buildArchiveDetailVO(record, context.studentMap(), context.userMap());
                result.add(AttendanceArchiveExportVO.builder()
                        .sessionTime(formatDateTime(session.getStartTime()))
                        .courseName(course != null ? course.getCourseName() : "")
                        .className(detailVO.getClassName())
                        .studentId(detailVO.getStudentId())
                        .studentName(detailVO.getStudentName())
                        .status(detailVO.getStatus())
                        .type(detailVO.getType())
                        .similarityScore(detailVO.getSimilarityScore())
                        .build());
            }
        }
        return result;
    }

    @Override
    public List<AttendanceArchiveCourseSummaryExportVO> listArchiveSummaryExportData(AttendanceArchiveQueryDTO queryDTO) {
        ArchiveContext context = buildArchiveContext(normalizeArchiveQuery(queryDTO));
        Map<Long, List<AttendanceSession>> sessionGroupByCourse = context.matchedSessions().stream()
                .collect(Collectors.groupingBy(AttendanceSession::getCourseId, LinkedHashMap::new, Collectors.toList()));

        List<AttendanceArchiveCourseSummaryExportVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<AttendanceSession>> entry : sessionGroupByCourse.entrySet()) {
            Long courseId = entry.getKey();
            Course course = context.courseMap().get(courseId);
            List<AttendanceSession> courseSessions = entry.getValue();

            int expectedTotal = 0;
            int actualTotal = 0;
            int absentTotal = 0;
            int lateTotal = 0;
            int leaveTotal = 0;

            for (AttendanceSession session : courseSessions) {
                List<AttendanceRecord> records = context.allRecordsBySession()
                        .getOrDefault(session.getSessionId(), List.of());
                expectedTotal += resolveExpectedCount(session, records);
                actualTotal += countActualStudents(records);
                absentTotal += countByStatus(records, AttendanceStatus.ABSENT);
                lateTotal += countByStatus(records, AttendanceStatus.LATE);
                leaveTotal += countByStatus(records, AttendanceStatus.LEAVE);
            }

            result.add(AttendanceArchiveCourseSummaryExportVO.builder()
                    .courseName(course != null ? course.getCourseName() : "")
                    .semester(course != null ? defaultText(course.getSemester()) : "")
                    .totalSessions(courseSessions.size())
                    .expectedTotal(expectedTotal)
                    .actualTotal(actualTotal)
                    .absentTotal(absentTotal)
                    .lateTotal(lateTotal)
                    .leaveTotal(leaveTotal)
                    .avgRate(formatRate(actualTotal, expectedTotal))
                    .build());
        }
        return result;
    }

    @Override
    public List<AttendanceArchiveSessionExportVO> listArchiveSessionExportData(Long sessionId) {
        AttendanceArchiveSessionDetailVO detail = getArchiveSessionDetail(sessionId);
        List<AttendanceArchiveSessionExportVO> result = new ArrayList<>();
        for (AttendanceArchiveDetailVO record : detail.getDetailList()) {
            result.add(AttendanceArchiveSessionExportVO.builder()
                    .sessionTime(detail.getSessionTime())
                    .courseName(detail.getCourseName())
                    .className(record.getClassName())
                    .studentId(record.getStudentId())
                    .studentName(record.getStudentName())
                    .status(record.getStatus())
                    .type(record.getType())
                    .similarityScore(record.getSimilarityScore())
                    .build());
        }
        return result;
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
    private double[] loadStudentFeatureVector(Student student) {
        String rawFeature = student.getFeatureVector();
        if (rawFeature == null || rawFeature.isEmpty()) {
            return new double[0];
        }

        try {
            String decryptedJson = AesUtils.decrypt(rawFeature);
            double[] decryptedFeature = parseFeatureVector(decryptedJson);
            if (decryptedFeature.length > 0) {
                return decryptedFeature;
            }

            log.warn("[ATTEND-301] decrypted feature is not valid JSON, skip student: userId={}, studentNumber={}",
                    student.getUserId(), student.getStudentNumber());
            return new double[0];
        } catch (Exception decryptException) {
            String trimmedFeature = rawFeature.trim();
            if (!trimmedFeature.startsWith("[")) {
                log.warn("[ATTEND-302] invalid encrypted feature, skip student: userId={}, studentNumber={}, reason={}",
                        student.getUserId(), student.getStudentNumber(), decryptException.getMessage());
                return new double[0];
            }

            double[] plainFeature = parseFeatureVector(trimmedFeature);
            if (plainFeature.length == 0) {
                log.warn("[ATTEND-303] invalid plain-json feature, skip student: userId={}, studentNumber={}",
                        student.getUserId(), student.getStudentNumber());
                return new double[0];
            }

            try {
                studentMapper.updateFeatureVector(student.getUserId(), AesUtils.encrypt(trimmedFeature));
                log.info("[ATTEND-304] repaired plain-json feature to encrypted storage: userId={}, studentNumber={}",
                        student.getUserId(), student.getStudentNumber());
            } catch (Exception repairException) {
                log.warn("[ATTEND-305] parsed plain-json feature but failed to persist repair: userId={}, studentNumber={}, reason={}",
                        student.getUserId(), student.getStudentNumber(), repairException.getMessage());
            }

            return plainFeature;
        }
    }

    private double[] parseFeatureVector(String featureVector) {
        try {
            List<Double> list = JSON.parseArray(featureVector, Double.class);
            double[] result = new double[list.size()];
            for (int i = 0; i < list.size(); i++) {
                result[i] = list.get(i);
            }
            return result;
        } catch (Exception e) {
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

    /**
     * 校验当前登录用户必须为教师。
     */
    private Long validateCurrentTeacher() {
        Long teacherId = BaseContext.getCurrentId();
        User currentUser = teacherId == null ? null : userMapper.findById(teacherId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return teacherId;
    }

    /**
     * 校验检测框归属当前教师。
     */
    private AttendanceDetection validateDetectionPermission(Long detectionId) {
        if (detectionId == null) {
            throw new BusinessException("检测框参数无效");
        }

        Long teacherId = validateCurrentTeacher();
        AttendanceDetection detection = attendanceDetectionMapper.findById(detectionId);
        if (detection == null) {
            throw new BusinessException("检测框不存在");
        }

        AttendanceSession session = attendanceSessionMapper.findById(detection.getSessionId());
        if (session == null) {
            throw new BusinessException("考勤会话不存在");
        }

        Course course = courseMapper.findById(session.getCourseId());
        if (course == null || !teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return detection;
    }

    /**
     * 构建考勤档案查询上下文。
     */
    private ArchiveContext buildArchiveContext(AttendanceArchiveQueryDTO queryDTO) {
        AttendanceArchiveQueryDTO safeQuery = normalizeArchiveQuery(queryDTO);
        Long teacherId = validateCurrentTeacher();
        List<Course> teacherCourses = courseMapper.findByTeacherId(teacherId);
        if (teacherCourses == null) {
            teacherCourses = new ArrayList<>();
        }
        List<Course> selectedCourses = filterTeacherCourses(teacherCourses, safeQuery.getCourseId());
        Map<Long, Course> courseMap = selectedCourses.stream()
                .collect(Collectors.toMap(Course::getCourseId, course -> course, (left, right) -> left, LinkedHashMap::new));
        if (selectedCourses.isEmpty()) {
            return new ArchiveContext(courseMap, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        List<Long> courseIds = selectedCourses.stream().map(Course::getCourseId).toList();
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseIds(courseIds).stream()
                .filter(session -> matchesDateRange(session.getStartTime(), safeQuery))
                .sorted(Comparator.comparing(AttendanceSession::getStartTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AttendanceSession::getSessionId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (sessions.isEmpty()) {
            return new ArchiveContext(courseMap, new ArrayList<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getSessionId).toList();
        List<AttendanceRecord> records = attendanceRecordMapper.findBySessionIds(sessionIds);
        Map<Long, List<AttendanceRecord>> allRecordsBySession = records.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getSessionId, LinkedHashMap::new, Collectors.toList()));

        Set<Long> studentIds = records.stream()
                .map(AttendanceRecord::getStudentId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Student> studentMap = loadStudentMap(studentIds);
        Map<Long, User> userMap = loadUserMap(studentIds);

        List<AttendanceSession> matchedSessions = new ArrayList<>();
        Map<Long, List<AttendanceRecord>> displayRecordsBySession = new LinkedHashMap<>();
        boolean hasRecordFilters = hasRecordLevelFilters(safeQuery);

        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> sessionRecords = allRecordsBySession.getOrDefault(session.getSessionId(), List.of());
            Integer sessionType = getSessionType(sessionRecords);
            if (safeQuery.getType() != null && !Objects.equals(safeQuery.getType(), sessionType)) {
                continue;
            }

            List<AttendanceRecord> displayRecords = filterDisplayRecords(sessionRecords, studentMap, userMap, safeQuery);
            if (hasRecordFilters && displayRecords.isEmpty()) {
                continue;
            }

            matchedSessions.add(session);
            displayRecordsBySession.put(session.getSessionId(), displayRecords.isEmpty() ? sessionRecords : displayRecords);
        }

        return new ArchiveContext(courseMap, matchedSessions, allRecordsBySession, displayRecordsBySession, studentMap, userMap);
    }

    /**
     * 过滤当前教师有权限访问的课程。
     */
    private List<Course> filterTeacherCourses(List<Course> teacherCourses, Long courseId) {
        if (courseId == null) {
            return teacherCourses == null ? new ArrayList<>() : teacherCourses;
        }
        if (teacherCourses == null) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return teacherCourses.stream()
                .filter(course -> Objects.equals(course.getCourseId(), courseId))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new BusinessException(MessageConstants.NO_PERMISSION));
    }

    /**
     * 构建抽屉明细行。
     */
    private AttendanceArchiveDetailVO buildArchiveDetailVO(AttendanceRecord record,
                                                           Map<Long, Student> studentMap,
                                                           Map<Long, User> userMap) {
        Student student = studentMap.get(record.getStudentId());
        User user = userMap.get(record.getStudentId());
        return AttendanceArchiveDetailVO.builder()
                .id(record.getRecordId())
                .studentId(student != null ? defaultText(student.getStudentNumber()) : "")
                .studentName(user != null ? defaultText(user.getRealName()) : "")
                .className(student != null ? defaultText(student.getAdminClass()) : "")
                .status(getStatusText(record.getStatus()))
                .type(getRecordTypeText(record.getUpdateType()))
                .similarityScore(formatSimilarity(record.getSimilarityScore()))
                .build();
    }

    /**
     * 过滤用于展示/导出的记录。
     */
    private List<AttendanceRecord> filterDisplayRecords(List<AttendanceRecord> records,
                                                        Map<Long, Student> studentMap,
                                                        Map<Long, User> userMap,
                                                        AttendanceArchiveQueryDTO queryDTO) {
        return records.stream()
                .filter(record -> matchesRecordFilters(record, studentMap, userMap, queryDTO))
                .sorted(Comparator.comparing(AttendanceRecord::getRecordId))
                .toList();
    }

    /**
     * 判断记录是否满足记录层筛选条件。
     */
    private boolean matchesRecordFilters(AttendanceRecord record,
                                         Map<Long, Student> studentMap,
                                         Map<Long, User> userMap,
                                         AttendanceArchiveQueryDTO queryDTO) {
        Student student = studentMap.get(record.getStudentId());
        User user = userMap.get(record.getStudentId());

        if (StringUtils.hasText(queryDTO.getAdminClass())) {
            String adminClass = student != null ? student.getAdminClass() : null;
            if (!Objects.equals(queryDTO.getAdminClass().trim(), normalizeText(adminClass))) {
                return false;
            }
        }
        if (queryDTO.getStatus() != null && !Objects.equals(queryDTO.getStatus(), record.getStatus())) {
            return false;
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword().trim();
            String studentNumber = student != null ? defaultText(student.getStudentNumber()) : "";
            String realName = user != null ? defaultText(user.getRealName()) : "";
            return studentNumber.contains(keyword) || realName.contains(keyword);
        }
        return true;
    }

    /**
     * 判断是否存在记录层筛选条件。
     */
    private boolean hasRecordLevelFilters(AttendanceArchiveQueryDTO queryDTO) {
        return StringUtils.hasText(queryDTO.getAdminClass())
                || queryDTO.getStatus() != null
                || StringUtils.hasText(queryDTO.getKeyword());
    }

    /**
     * 判断会话时间是否命中日期范围。
     */
    private boolean matchesDateRange(LocalDateTime startTime, AttendanceArchiveQueryDTO queryDTO) {
        if (startTime == null) {
            return false;
        }

        LocalDate startDate = parseDate(queryDTO.getStartDate());
        LocalDate endDate = parseDate(queryDTO.getEndDate());
        LocalDate currentDate = startTime.toLocalDate();

        if (startDate != null && currentDate.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !currentDate.isAfter(endDate);
    }

    /**
     * 解析日期字符串。
     */
    private LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), ARCHIVE_DATE_FORMATTER);
        } catch (Exception e) {
            throw new BusinessException("日期格式错误，请使用 yyyy-MM-dd");
        }
    }

    /**
     * 统计指定状态人数。
     */
    private int countByStatus(List<AttendanceRecord> records, Integer status) {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (Objects.equals(status, record.getStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * 计算会话应到人数。
     */
    private int resolveExpectedCount(AttendanceSession session, List<AttendanceRecord> records) {
        if (session != null && session.getTotalStudent() != null && session.getTotalStudent() > 0) {
            return session.getTotalStudent();
        }
        return records.size();
    }

    /**
     * 构建会话班级展示文本。
     */
    private String buildSessionClassName(List<AttendanceRecord> records,
                                         Map<Long, Student> studentMap) {
        Set<String> classNames = new LinkedHashSet<>();
        for (AttendanceRecord record : records) {
            Student student = studentMap.get(record.getStudentId());
            if (student != null && StringUtils.hasText(student.getAdminClass())) {
                classNames.add(student.getAdminClass().trim());
            }
        }
        if (classNames.isEmpty()) {
            return "未分班";
        }
        return String.join(" / ", classNames);
    }

    /**
     * 获取会话类型编码。
     */
    private Integer getSessionType(List<AttendanceRecord> records) {
        for (AttendanceRecord record : records) {
            if (Integer.valueOf(2).equals(record.getUpdateType())) {
                return ARCHIVE_TYPE_MANUAL;
            }
        }
        return ARCHIVE_TYPE_AUTO;
    }

    /**
     * 获取会话类型文本。
     */
    private String getSessionTypeText(List<AttendanceRecord> records) {
        return Objects.equals(getSessionType(records), ARCHIVE_TYPE_MANUAL) ? "人工修正" : "课堂识别";
    }

    /**
     * 获取记录方式文本。
     */
    private String getRecordTypeText(Integer updateType) {
        return Integer.valueOf(2).equals(updateType) ? "人工修正" : "课堂识别";
    }

    /**
     * 格式化时间。
     */
    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(ARCHIVE_TIME_FORMATTER);
    }

    /**
     * 格式化出勤率。
     */
    private String formatRate(int actualCount, int expectedCount) {
        if (expectedCount <= 0) {
            return "0%";
        }
        BigDecimal rate = BigDecimal.valueOf(actualCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(expectedCount), 1, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rate.toPlainString() + "%";
    }

    /**
     * 格式化相似度。
     */
    private String formatSimilarity(BigDecimal value) {
        if (value == null) {
            return "--";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    /**
     * 批量加载学生信息并转为 Map。
     */
    private Map<Long, Student> loadStudentMap(Set<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return studentMapper.findByUserIds(new ArrayList<>(studentIds)).stream()
                .collect(Collectors.toMap(Student::getUserId, student -> student, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 批量加载用户信息并转为 Map。
     */
    private Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return userMapper.findByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 构建课程选项显示名称。
     */
    private String buildCourseOptionLabel(Course course) {
        if (course == null) {
            return "";
        }
        if (!StringUtils.hasText(course.getSemester())) {
            return course.getCourseName();
        }
        return course.getCourseName() + "（" + course.getSemester().trim() + "）";
    }

    /**
     * 获取当前页码。
     */
    private int getCurrentPage(AttendanceArchiveQueryDTO queryDTO) {
        return queryDTO.getCurrentPage() == null || queryDTO.getCurrentPage() < 1 ? 1 : queryDTO.getCurrentPage();
    }

    /**
     * 获取每页条数。
     */
    private int getPageSize(AttendanceArchiveQueryDTO queryDTO) {
        if (queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1) {
            return 10;
        }
        return Math.min(queryDTO.getPageSize(), 100);
    }

    /**
     * 将档案统计中的空值转为 0。
     */
    private int defaultArchiveCount(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 默认文本转换。
     */
    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 解析会话原图列表。
     */
    private List<String> parseSourceImages(String sourceImages) {
        if (!StringUtils.hasText(sourceImages)) {
            return new ArrayList<>();
        }
        try {
            List<String> imageKeys = JSON.parseArray(sourceImages, String.class);
            return imageKeys == null ? new ArrayList<>() : imageKeys;
        } catch (Exception e) {
            log.warn("解析会话原图列表失败: sourceImages={}", sourceImages, e);
            return new ArrayList<>();
        }
    }

    /**
     * 解析标注视角。
     */
    private String resolveViewKey(Integer imageIndex) {
        if (imageIndex == null) {
            return "unknown";
        }
        return switch (imageIndex) {
            case 0 -> "left";
            case 1 -> "center";
            case 2 -> "right";
            default -> "extra-" + imageIndex;
        };
    }

    /**
     * 构建检测框展示数据。
     */
    private AttendanceDetectionVO buildDetectionVO(AttendanceDetection detection,
                                                   Map<Long, Student> studentMap,
                                                   Map<Long, User> userMap,
                                                   Map<Long, AttendanceRecord> recordMap) {
        Student student = detection.getStudentId() == null ? null : studentMap.get(detection.getStudentId());
        User user = detection.getStudentId() == null ? null : userMap.get(detection.getStudentId());
        AttendanceRecord record = detection.getRecordId() == null ? null : recordMap.get(detection.getRecordId());

        return AttendanceDetectionVO.builder()
                .detectionId(detection.getDetectionId())
                .imageIndex(detection.getImageIndex())
                .viewKey(resolveViewKey(detection.getImageIndex()))
                .faceIndex(detection.getFaceIndex())
                .bbox(detection.getBbox())
                .detectionScore(formatSimilarity(detection.getDetectionScore()))
                .matched(Boolean.TRUE.equals(detection.getMatched()))
                .ignored(Boolean.TRUE.equals(detection.getIgnored()))
                .ignoreReason(defaultText(detection.getIgnoreReason()))
                .studentId(detection.getStudentId())
                .recordId(detection.getRecordId())
                .studentNumber(student != null ? defaultText(student.getStudentNumber()) : "")
                .realName(user != null ? defaultText(user.getRealName()) : "")
                .similarityScore(formatSimilarity(detection.getSimilarityScore()))
                .finalStatus(record != null ? record.getStatus() : null)
                .finalStatusText(record != null ? getStatusText(record.getStatus()) : "")
                .manualModified(record != null && Integer.valueOf(2).equals(record.getUpdateType()))
                .build();
    }

    /**
     * 规范化文本。
     */
    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 规范化考勤档案查询参数。
     */
    private AttendanceArchiveQueryDTO normalizeArchiveQuery(AttendanceArchiveQueryDTO queryDTO) {
        return queryDTO == null ? new AttendanceArchiveQueryDTO() : queryDTO;
    }

    /**
     * 考勤档案查询上下文。
     */
    private record ArchiveContext(Map<Long, Course> courseMap,
                                  List<AttendanceSession> matchedSessions,
                                  Map<Long, List<AttendanceRecord>> allRecordsBySession,
                                  Map<Long, List<AttendanceRecord>> displayRecordsBySession,
                                  Map<Long, Student> studentMap,
                                  Map<Long, User> userMap) {
    }
}
