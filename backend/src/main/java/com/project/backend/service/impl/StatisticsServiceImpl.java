package com.project.backend.service.impl;

import com.project.backend.constant.AttendanceStatus;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceRecordMapper;
import com.project.backend.mapper.AttendanceSessionMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.StatisticsDashboardQueryDTO;
import com.project.backend.pojo.entity.AttendanceRecord;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.CourseStatisticsVO;
import com.project.backend.pojo.vo.StatisticsClassStatusVO;
import com.project.backend.pojo.vo.StatisticsCorrectionVO;
import com.project.backend.pojo.vo.StatisticsCourseRateVO;
import com.project.backend.pojo.vo.StatisticsDashboardVO;
import com.project.backend.pojo.vo.StatisticsOptionVO;
import com.project.backend.pojo.vo.StatisticsStatusItemVO;
import com.project.backend.pojo.vo.StatisticsStudentAnomalyVO;
import com.project.backend.pojo.vo.StatisticsSummaryVO;
import com.project.backend.pojo.vo.StatisticsTrendItemVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;
import com.project.backend.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final DateTimeFormatter DATE_QUERY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TREND_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private AttendanceRecordMapper attendanceRecordMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public StatisticsDashboardVO getDashboard(StatisticsDashboardQueryDTO queryDTO) {
        Long teacherId = validateCurrentTeacher();

        List<Course> teacherCourses = courseMapper.findByTeacherId(teacherId);
        List<StatisticsOptionVO> semesterOptions = buildSemesterOptions(teacherCourses);
        List<Course> semesterScopedCourses = filterCoursesBySemester(teacherCourses, queryDTO.getSemester());
        List<StatisticsOptionVO> courseOptions = buildCourseOptions(semesterScopedCourses);

        List<Course> selectedCourses = filterCoursesByCourseId(semesterScopedCourses, queryDTO.getCourseId());
        Set<Long> selectedCourseIds = selectedCourses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<Long> allStudentIds = loadStudentIdsByCourseIds(selectedCourseIds);
        Map<Long, Student> allStudentMap = loadStudentMap(allStudentIds);
        List<StatisticsOptionVO> classOptions = buildClassOptions(allStudentMap.values());

        Set<Long> scopedStudentIds = filterStudentIdsByAdminClass(allStudentMap, queryDTO.getAdminClass());
        List<Course> scopedCourses = filterCoursesByStudentScope(selectedCourses, scopedStudentIds);
        Set<Long> scopedCourseIds = scopedCourses.stream()
                .map(Course::getCourseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<AttendanceSession> scopedSessions = filterSessionsByDateRange(
                scopedCourseIds.isEmpty()
                        ? new ArrayList<>()
                        : attendanceSessionMapper.findByCourseIds(new ArrayList<>(scopedCourseIds)),
                queryDTO.getStartDate(),
                queryDTO.getEndDate()
        );

        Map<Long, AttendanceSession> sessionMap = scopedSessions.stream()
                .collect(Collectors.toMap(AttendanceSession::getSessionId, session -> session, (left, right) -> left, LinkedHashMap::new));
        List<AttendanceRecord> scopedRecords = loadScopedRecords(sessionMap.keySet(), scopedStudentIds);

        Map<Long, List<AttendanceRecord>> recordGroupBySession = scopedRecords.stream()
                .collect(Collectors.groupingBy(AttendanceRecord::getSessionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, List<AttendanceSession>> sessionGroupByCourse = scopedSessions.stream()
                .collect(Collectors.groupingBy(AttendanceSession::getCourseId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, Student> scopedStudentMap = filterStudentsByIds(allStudentMap, scopedStudentIds);
        Map<Long, User> scopedUserMap = loadUserMap(scopedStudentIds);

        return StatisticsDashboardVO.builder()
                .semesterOptions(semesterOptions)
                .courseOptions(courseOptions)
                .classOptions(classOptions)
                .summaryData(buildSummary(
                        scopedCourses,
                        scopedStudentIds,
                        scopedStudentMap,
                        scopedSessions,
                        scopedRecords,
                        Boolean.TRUE.equals(queryDTO.getAnomalyIncludeLeave())
                ))
                .statusDistribution(buildStatusDistribution(scopedRecords))
                .attendanceTrend(buildTrendData(scopedSessions, recordGroupBySession))
                .courseAttendanceComparison(buildCourseRateComparison(scopedCourses, sessionGroupByCourse, recordGroupBySession))
                .classStatusComposition(buildClassStatusComposition(scopedRecords, scopedStudentMap))
                .studentAnomalyRanking(buildStudentAnomalyRanking(
                        scopedRecords,
                        scopedStudentMap,
                        scopedUserMap,
                        Boolean.TRUE.equals(queryDTO.getAnomalyIncludeLeave())
                ))
                .correctionAnalysis(buildCorrectionAnalysis(scopedCourses, sessionGroupByCourse, recordGroupBySession))
                .build();
    }

    @Override
    public CourseStatisticsVO getCourseStatistics(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        Course course = requireTeacherOwnedCourse(courseId, teacherId);

        Integer totalStudents = courseStudentMapper.countByCourseId(courseId);
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        int totalSessions = sessions.size();

        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int leaveCount = 0;

        for (AttendanceSession session : sessions) {
            Integer present = attendanceRecordMapper.countBySessionIdAndStatus(session.getSessionId(), AttendanceStatus.PRESENT);
            Integer late = attendanceRecordMapper.countBySessionIdAndStatus(session.getSessionId(), AttendanceStatus.LATE);
            Integer absent = attendanceRecordMapper.countBySessionIdAndStatus(session.getSessionId(), AttendanceStatus.ABSENT);
            Integer leave = attendanceRecordMapper.countBySessionIdAndStatus(session.getSessionId(), AttendanceStatus.LEAVE);

            presentCount += present != null ? present : 0;
            lateCount += late != null ? late : 0;
            absentCount += absent != null ? absent : 0;
            leaveCount += leave != null ? leave : 0;
        }

        int totalRecords = presentCount + lateCount + absentCount + leaveCount;
        BigDecimal avgRate = BigDecimal.ZERO;
        if (totalRecords > 0) {
            avgRate = BigDecimal.valueOf(presentCount + lateCount)
                    .divide(BigDecimal.valueOf(totalRecords), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return CourseStatisticsVO.builder()
                .courseId(courseId)
                .courseName(course.getCourseName())
                .totalSessions(totalSessions)
                .totalStudents(totalStudents != null ? totalStudents : 0)
                .avgAttendanceRate(avgRate)
                .presentCount(presentCount)
                .lateCount(lateCount)
                .absentCount(absentCount)
                .leaveCount(leaveCount)
                .build();
    }

    @Override
    public List<StudentStatisticsVO> getStudentStatistics(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        requireTeacherOwnedCourse(courseId, teacherId);
        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(courseId);
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getSessionId).toList();

        List<StudentStatisticsVO> result = new ArrayList<>();

        for (Long studentId : studentIds) {
            Student student = studentMapper.findByUserId(studentId);
            User user = userMapper.findById(studentId);

            if (student == null || user == null) {
                continue;
            }

            int presentCount = 0;
            int lateCount = 0;
            int absentCount = 0;
            int leaveCount = 0;

            for (Long sessionId : sessionIds) {
                List<AttendanceRecord> records = attendanceRecordMapper.findBySessionId(sessionId);
                for (AttendanceRecord record : records) {
                    if (record.getStudentId().equals(studentId)) {
                        switch (record.getStatus()) {
                            case 0 -> absentCount++;
                            case 1 -> presentCount++;
                            case 2 -> lateCount++;
                            case 3 -> leaveCount++;
                            default -> {
                            }
                        }
                    }
                }
            }

            int totalCount = presentCount + lateCount + absentCount + leaveCount;
            BigDecimal rate = BigDecimal.ZERO;
            if (totalCount > 0) {
                rate = BigDecimal.valueOf(presentCount + lateCount)
                        .divide(BigDecimal.valueOf(totalCount), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }

            result.add(StudentStatisticsVO.builder()
                    .studentId(studentId)
                    .studentNumber(student.getStudentNumber())
                    .realName(user.getRealName())
                    .adminClass(student.getAdminClass())
                    .presentCount(presentCount)
                    .lateCount(lateCount)
                    .absentCount(absentCount)
                    .leaveCount(leaveCount)
                    .totalCount(totalCount)
                    .attendanceRate(rate)
                    .build());
        }

        return result;
    }

    /**
     * 校验当前登录用户是否为教师。
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
     * 校验课程归属当前教师。
     */
    private Course requireTeacherOwnedCourse(Long courseId, Long teacherId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return course;
    }

    /**
     * 构建学期筛选项。
     */
    private List<StatisticsOptionVO> buildSemesterOptions(List<Course> courses) {
        return courses.stream()
                .map(Course::getSemester)
                .filter(semester -> semester != null && !semester.isBlank())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .map(semester -> StatisticsOptionVO.builder()
                        .label(formatSemesterLabel(semester))
                        .value(semester)
                        .build())
                .toList();
    }

    /**
     * 构建课程筛选项。
     */
    private List<StatisticsOptionVO> buildCourseOptions(List<Course> courses) {
        return courses.stream()
                .sorted(Comparator.comparing(Course::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Course::getCourseId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(course -> StatisticsOptionVO.builder()
                        .label(buildCourseOptionLabel(course))
                        .value(String.valueOf(course.getCourseId()))
                        .build())
                .toList();
    }

    /**
     * 构建班级筛选项。
     */
    private List<StatisticsOptionVO> buildClassOptions(Collection<Student> students) {
        return students.stream()
                .map(Student::getAdminClass)
                .filter(adminClass -> adminClass != null && !adminClass.isBlank())
                .distinct()
                .sorted()
                .map(adminClass -> StatisticsOptionVO.builder()
                        .label(adminClass)
                        .value(adminClass)
                        .build())
                .toList();
    }

    /**
     * 按学期筛选课程。
     */
    private List<Course> filterCoursesBySemester(List<Course> courses, String semester) {
        if (semester == null || semester.isBlank()) {
            return new ArrayList<>(courses);
        }
        return courses.stream()
                .filter(course -> Objects.equals(course.getSemester(), semester))
                .toList();
    }

    /**
     * 按课程 ID 筛选课程。
     */
    private List<Course> filterCoursesByCourseId(List<Course> courses, Long courseId) {
        if (courseId == null) {
            return new ArrayList<>(courses);
        }
        return courses.stream()
                .filter(course -> Objects.equals(course.getCourseId(), courseId))
                .toList();
    }

    /**
     * 加载课程下的全部学生 ID。
     */
    private Set<Long> loadStudentIdsByCourseIds(Set<Long> courseIds) {
        Set<Long> result = new LinkedHashSet<>();
        for (Long courseId : courseIds) {
            result.addAll(courseStudentMapper.findStudentIdsByCourseId(courseId));
        }
        return result;
    }

    /**
     * 加载学生映射。
     */
    private Map<Long, Student> loadStudentMap(Set<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return studentMapper.findByUserIds(new ArrayList<>(studentIds)).stream()
                .collect(Collectors.toMap(Student::getUserId, student -> student, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 加载用户映射。
     */
    private Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return userMapper.findByIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(User::getUserId, user -> user, (left, right) -> left, LinkedHashMap::new));
    }

    /**
     * 按行政班级筛选学生 ID。
     */
    private Set<Long> filterStudentIdsByAdminClass(Map<Long, Student> studentMap, String adminClass) {
        if (adminClass == null || adminClass.isBlank()) {
            return new LinkedHashSet<>(studentMap.keySet());
        }
        return studentMap.values().stream()
                .filter(student -> Objects.equals(student.getAdminClass(), adminClass))
                .map(Student::getUserId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 按学生范围筛选课程。
     */
    private List<Course> filterCoursesByStudentScope(List<Course> courses, Set<Long> scopedStudentIds) {
        if (scopedStudentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Course> result = new ArrayList<>();
        for (Course course : courses) {
            List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(course.getCourseId());
            boolean hasMatchedStudent = studentIds.stream().anyMatch(scopedStudentIds::contains);
            if (hasMatchedStudent) {
                result.add(course);
            }
        }
        return result;
    }

    /**
     * 按时间范围筛选会话。
     */
    private List<AttendanceSession> filterSessionsByDateRange(List<AttendanceSession> sessions, String startDate, String endDate) {
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);

        return sessions.stream()
                .filter(session -> {
                    LocalDate sessionDate = session.getStartTime() == null ? null : session.getStartTime().toLocalDate();
                    if (sessionDate == null) {
                        return false;
                    }
                    if (start != null && sessionDate.isBefore(start)) {
                        return false;
                    }
                    return end == null || !sessionDate.isAfter(end);
                })
                .toList();
    }

    /**
     * 根据会话和学生范围加载考勤记录。
     */
    private List<AttendanceRecord> loadScopedRecords(Set<Long> sessionIds, Set<Long> scopedStudentIds) {
        if (sessionIds.isEmpty() || scopedStudentIds.isEmpty()) {
            return new ArrayList<>();
        }

        return attendanceRecordMapper.findBySessionIds(new ArrayList<>(sessionIds)).stream()
                .filter(record -> scopedStudentIds.contains(record.getStudentId()))
                .toList();
    }

    /**
     * 将学生映射裁剪到指定用户 ID 范围。
     */
    private Map<Long, Student> filterStudentsByIds(Map<Long, Student> studentMap, Set<Long> scopedStudentIds) {
        Map<Long, Student> result = new LinkedHashMap<>();
        for (Long studentId : scopedStudentIds) {
            Student student = studentMap.get(studentId);
            if (student != null) {
                result.put(studentId, student);
            }
        }
        return result;
    }

    /**
     * 构建顶部概览统计。
     */
    private StatisticsSummaryVO buildSummary(List<Course> courses,
                                             Set<Long> scopedStudentIds,
                                             Map<Long, Student> studentMap,
                                             List<AttendanceSession> sessions,
                                             List<AttendanceRecord> records,
                                             boolean anomalyIncludeLeave) {
        int presentCount = countByStatus(records, AttendanceStatus.PRESENT);
        int lateCount = countByStatus(records, AttendanceStatus.LATE);
        int absentCount = countByStatus(records, AttendanceStatus.ABSENT);
        int leaveCount = countByStatus(records, AttendanceStatus.LEAVE);
        int totalRecords = presentCount + lateCount + absentCount + leaveCount;
        int totalAnomalies = lateCount + absentCount + (anomalyIncludeLeave ? leaveCount : 0);

        long enteredFaceCount = studentMap.values().stream()
                .filter(student -> student.getFeatureVector() != null && !student.getFeatureVector().isBlank())
                .count();

        return StatisticsSummaryVO.builder()
                .totalCourses(courses.size())
                .coveredStudents(scopedStudentIds.size())
                .totalSessions(sessions.size())
                .avgAttendanceRate(calculateRate(presentCount + lateCount, totalRecords))
                .totalAnomalies(totalAnomalies)
                .faceEntryRate(calculateRate((int) enteredFaceCount, scopedStudentIds.size()))
                .build();
    }

    /**
     * 构建状态占比数据。
     */
    private List<StatisticsStatusItemVO> buildStatusDistribution(List<AttendanceRecord> records) {
        return List.of(
                StatisticsStatusItemVO.builder().name("已到").value(countByStatus(records, AttendanceStatus.PRESENT)).build(),
                StatisticsStatusItemVO.builder().name("迟到").value(countByStatus(records, AttendanceStatus.LATE)).build(),
                StatisticsStatusItemVO.builder().name("缺勤").value(countByStatus(records, AttendanceStatus.ABSENT)).build(),
                StatisticsStatusItemVO.builder().name("请假").value(countByStatus(records, AttendanceStatus.LEAVE)).build()
        );
    }

    /**
     * 构建趋势图数据。
     */
    private List<StatisticsTrendItemVO> buildTrendData(List<AttendanceSession> sessions,
                                                       Map<Long, List<AttendanceRecord>> recordGroupBySession) {
        List<AttendanceSession> sortedSessions = sessions.stream()
                .sorted(Comparator.comparing(AttendanceSession::getStartTime))
                .toList();

        if (sortedSessions.size() > 12) {
            sortedSessions = sortedSessions.subList(sortedSessions.size() - 12, sortedSessions.size());
        }

        List<StatisticsTrendItemVO> result = new ArrayList<>();
        for (AttendanceSession session : sortedSessions) {
            List<AttendanceRecord> sessionRecords = recordGroupBySession.getOrDefault(session.getSessionId(), List.of());
            int totalRecords = sessionRecords.size();
            int attendanceCount = countByStatus(sessionRecords, AttendanceStatus.PRESENT)
                    + countByStatus(sessionRecords, AttendanceStatus.LATE);

            result.add(StatisticsTrendItemVO.builder()
                    .label(formatTrendLabel(session.getStartTime()))
                    .attendanceRate(calculateRate(attendanceCount, totalRecords))
                    .build());
        }
        return result;
    }

    /**
     * 构建课程出勤率对比数据。
     */
    private List<StatisticsCourseRateVO> buildCourseRateComparison(List<Course> courses,
                                                                   Map<Long, List<AttendanceSession>> sessionGroupByCourse,
                                                                   Map<Long, List<AttendanceRecord>> recordGroupBySession) {
        List<StatisticsCourseRateVO> result = new ArrayList<>();
        for (Course course : courses) {
            int totalRecords = 0;
            int attendanceCount = 0;

            for (AttendanceSession session : sessionGroupByCourse.getOrDefault(course.getCourseId(), List.of())) {
                List<AttendanceRecord> sessionRecords = recordGroupBySession.getOrDefault(session.getSessionId(), List.of());
                totalRecords += sessionRecords.size();
                attendanceCount += countByStatus(sessionRecords, AttendanceStatus.PRESENT)
                        + countByStatus(sessionRecords, AttendanceStatus.LATE);
            }

            result.add(StatisticsCourseRateVO.builder()
                    .courseName(course.getCourseName())
                    .attendanceRate(calculateRate(attendanceCount, totalRecords))
                    .build());
        }

        result.sort(Comparator.comparing(StatisticsCourseRateVO::getAttendanceRate).reversed()
                .thenComparing(StatisticsCourseRateVO::getCourseName));
        return result;
    }

    /**
     * 构建班级考勤状态构成数据。
     */
    private List<StatisticsClassStatusVO> buildClassStatusComposition(List<AttendanceRecord> records,
                                                                      Map<Long, Student> studentMap) {
        Map<String, StatisticsClassStatusVO> classStatusMap = new LinkedHashMap<>();

        for (AttendanceRecord record : records) {
            Student student = studentMap.get(record.getStudentId());
            if (student == null) {
                continue;
            }

            String adminClass = defaultClassName(student.getAdminClass());
            StatisticsClassStatusVO item = classStatusMap.computeIfAbsent(adminClass, key -> StatisticsClassStatusVO.builder()
                    .adminClass(key)
                    .presentCount(0)
                    .lateCount(0)
                    .absentCount(0)
                    .leaveCount(0)
                    .build());

            switch (record.getStatus()) {
                case 0 -> item.setAbsentCount(item.getAbsentCount() + 1);
                case 1 -> item.setPresentCount(item.getPresentCount() + 1);
                case 2 -> item.setLateCount(item.getLateCount() + 1);
                case 3 -> item.setLeaveCount(item.getLeaveCount() + 1);
                default -> {
                }
            }
        }

        return classStatusMap.values().stream()
                .sorted(Comparator.comparing(StatisticsClassStatusVO::getAdminClass))
                .toList();
    }

    /**
     * 构建学生异常排行数据。
     */
    private List<StatisticsStudentAnomalyVO> buildStudentAnomalyRanking(List<AttendanceRecord> records,
                                                                        Map<Long, Student> studentMap,
                                                                        Map<Long, User> userMap,
                                                                        boolean anomalyIncludeLeave) {
        Map<Long, Integer> anomalyCountMap = new LinkedHashMap<>();

        for (AttendanceRecord record : records) {
            if (!isAnomalyStatus(record.getStatus(), anomalyIncludeLeave)) {
                continue;
            }
            anomalyCountMap.merge(record.getStudentId(), 1, Integer::sum);
        }

        return anomalyCountMap.entrySet().stream()
                .map(entry -> {
                    Long studentId = entry.getKey();
                    Student student = studentMap.get(studentId);
                    User user = userMap.get(studentId);
                    return StatisticsStudentAnomalyVO.builder()
                            .studentName(user != null ? user.getRealName() : "未知学生")
                            .adminClass(student != null ? defaultClassName(student.getAdminClass()) : "未分班")
                            .anomalyCount(entry.getValue())
                            .build();
                })
                .sorted(Comparator.comparing(StatisticsStudentAnomalyVO::getAnomalyCount).reversed()
                        .thenComparing(StatisticsStudentAnomalyVO::getStudentName))
                .limit(10)
                .toList();
    }

    /**
     * 构建人工修正统计数据。
     */
    private List<StatisticsCorrectionVO> buildCorrectionAnalysis(List<Course> courses,
                                                                 Map<Long, List<AttendanceSession>> sessionGroupByCourse,
                                                                 Map<Long, List<AttendanceRecord>> recordGroupBySession) {
        List<StatisticsCorrectionVO> result = new ArrayList<>();
        for (Course course : courses) {
            int autoCount = 0;
            int manualCount = 0;

            for (AttendanceSession session : sessionGroupByCourse.getOrDefault(course.getCourseId(), List.of())) {
                for (AttendanceRecord record : recordGroupBySession.getOrDefault(session.getSessionId(), List.of())) {
                    if (Integer.valueOf(2).equals(record.getUpdateType())) {
                        manualCount++;
                    } else {
                        autoCount++;
                    }
                }
            }

            result.add(StatisticsCorrectionVO.builder()
                    .courseName(course.getCourseName())
                    .autoCount(autoCount)
                    .manualCount(manualCount)
                    .build());
        }

        result.sort(Comparator.comparing((StatisticsCorrectionVO item) -> item.getAutoCount() + item.getManualCount())
                .reversed()
                .thenComparing(StatisticsCorrectionVO::getCourseName));
        return result;
    }

    /**
     * 计算百分比。
     */
    private BigDecimal calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    /**
     * 统计指定状态数量。
     */
    private int countByStatus(List<AttendanceRecord> records, Integer status) {
        int count = 0;
        for (AttendanceRecord record : records) {
            if (Objects.equals(record.getStatus(), status)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断记录是否属于异常状态。
     */
    private boolean isAnomalyStatus(Integer status, boolean anomalyIncludeLeave) {
        if (Objects.equals(status, AttendanceStatus.ABSENT) || Objects.equals(status, AttendanceStatus.LATE)) {
            return true;
        }
        return anomalyIncludeLeave && Objects.equals(status, AttendanceStatus.LEAVE);
    }

    /**
     * 解析日期。
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value, DATE_QUERY_FORMATTER);
    }

    /**
     * 格式化趋势图标签。
     */
    private String formatTrendLabel(LocalDateTime startTime) {
        if (startTime == null) {
            return "--";
        }
        return startTime.format(TREND_LABEL_FORMATTER);
    }

    /**
     * 格式化学期文案。
     */
    private String formatSemesterLabel(String semester) {
        String[] parts = semester.split("-");
        if (parts.length == 3) {
            return parts[0] + "-" + parts[1] + " 学年 " + parts[2] + " 学期";
        }
        return semester;
    }

    /**
     * 构建课程下拉显示文案。
     */
    private String buildCourseOptionLabel(Course course) {
        if (course.getSemester() == null || course.getSemester().isBlank()) {
            return course.getCourseName();
        }
        return course.getCourseName() + "（" + course.getSemester() + "）";
    }

    /**
     * 兜底班级名称。
     */
    private String defaultClassName(String adminClass) {
        if (adminClass == null || adminClass.isBlank()) {
            return "未分班";
        }
        return adminClass;
    }
}
