package com.project.backend.service.impl;

import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceNoticeMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.mapper.WarningMapper;
import com.project.backend.pojo.dto.WarningNoticeSendDTO;
import com.project.backend.pojo.dto.WarningQueryDTO;
import com.project.backend.pojo.entity.AttendanceNotice;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.vo.StatisticsOptionVO;
import com.project.backend.pojo.vo.WarningCenterPageVO;
import com.project.backend.pojo.vo.WarningDetailVO;
import com.project.backend.pojo.vo.WarningNoticeVO;
import com.project.backend.pojo.vo.WarningOptionsVO;
import com.project.backend.pojo.vo.WarningRankingVO;
import com.project.backend.pojo.vo.WarningSummaryVO;
import com.project.backend.pojo.vo.WarningTimelineVO;
import com.project.backend.service.WarningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 预警中心服务实现。
 */
@Slf4j
@Service
public class WarningServiceImpl implements WarningService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private WarningMapper warningMapper;

    @Autowired
    private AttendanceNoticeMapper attendanceNoticeMapper;

    @Override
    public WarningOptionsVO getWarningOptions(Long courseId) {
        Long teacherId = validateCurrentTeacher();

        List<Course> teacherCourses = courseMapper.findByTeacherId(teacherId);
        List<StatisticsOptionVO> courseOptions = teacherCourses.stream()
                .map(course -> StatisticsOptionVO.builder()
                        .label(buildCourseLabel(course))
                        .value(String.valueOf(course.getCourseId()))
                        .build())
                .toList();

        List<String> classes;
        if (courseId == null) {
            classes = studentMapper.findAdminClassesByTeacherId(teacherId);
        } else {
            requireTeacherOwnedCourse(courseId, teacherId);
            classes = studentMapper.findAdminClassesByCourseId(courseId);
        }

        List<StatisticsOptionVO> classOptions = classes.stream()
                .map(item -> StatisticsOptionVO.builder()
                        .label(item)
                        .value(item)
                        .build())
                .toList();

        return WarningOptionsVO.builder()
                .courseOptions(courseOptions)
                .classOptions(classOptions)
                .build();
    }

    @Override
    public WarningCenterPageVO getWarningPage(WarningQueryDTO queryDTO) {
        Long teacherId = validateCurrentTeacher();
        WarningQueryDTO safeQuery = normalizeQuery(queryDTO);

        List<WarningRankingVO> allRankings = warningMapper.listWarningRankings(teacherId, safeQuery);
        List<WarningNoticeVO> noticeHistory = warningMapper.listNoticeHistory(teacherId, safeQuery);

        WarningSummaryVO summary = buildSummary(allRankings, noticeHistory);
        PageResult<WarningRankingVO> pageData = buildPageResult(allRankings, safeQuery.getCurrentPage(), safeQuery.getPageSize());

        return WarningCenterPageVO.builder()
                .summary(summary)
                .pageData(pageData)
                .build();
    }

    @Override
    public WarningDetailVO getWarningDetail(Long courseId, Long studentId, WarningQueryDTO queryDTO) {
        Long teacherId = validateCurrentTeacher();
        validateCourseAndStudentScope(teacherId, courseId, studentId);

        WarningQueryDTO safeQuery = normalizeQuery(queryDTO);
        WarningRankingVO ranking = warningMapper.getWarningRankingDetail(teacherId, courseId, studentId, safeQuery);
        if (ranking == null) {
            throw new BusinessException("未找到该学生在当前课程下的缺勤记录");
        }

        List<WarningTimelineVO> timeline = warningMapper.listWarningTimelines(teacherId, courseId, studentId, safeQuery);
        return WarningDetailVO.builder()
                .userId(ranking.getUserId())
                .studentId(ranking.getStudentId())
                .studentName(ranking.getStudentName())
                .className(ranking.getClassName())
                .courseId(ranking.getCourseId())
                .courseName(ranking.getCourseName())
                .absenceCount(ranking.getAbsenceCount())
                .lastAbsenceTime(ranking.getLastAbsenceTime())
                .lastNotifyTime(ranking.getLastNotifyTime())
                .notifyCount(ranking.getNotifyCount())
                .hasUnread(Boolean.TRUE.equals(ranking.getHasUnread()))
                .timeline(timeline)
                .build();
    }

    @Override
    public List<WarningNoticeVO> getNoticeHistory(WarningQueryDTO queryDTO) {
        Long teacherId = validateCurrentTeacher();
        return warningMapper.listNoticeHistory(teacherId, normalizeQuery(queryDTO));
    }

    @Override
    public void sendNotice(WarningNoticeSendDTO sendDTO) {
        if (sendDTO == null || sendDTO.getStudentId() == null || sendDTO.getCourseId() == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }
        if (sendDTO.getTitle() == null || sendDTO.getTitle().trim().isEmpty()
                || sendDTO.getContent() == null || sendDTO.getContent().trim().isEmpty()) {
            throw new BusinessException("通知标题和内容不能为空");
        }

        Long teacherId = validateCurrentTeacher();
        validateCourseAndStudentScope(teacherId, sendDTO.getCourseId(), sendDTO.getStudentId());

        AttendanceNotice notice = AttendanceNotice.builder()
                .teacherId(teacherId)
                .studentId(sendDTO.getStudentId())
                .courseId(sendDTO.getCourseId())
                .noticeType(1)
                .absentCount(defaultCount(sendDTO.getAbsentCount()))
                .title(sendDTO.getTitle().trim())
                .content(sendDTO.getContent().trim())
                .sendStatus(1)
                .readStatus(0)
                .sendTime(LocalDateTime.now())
                .createTime(LocalDateTime.now())
                .build();
        attendanceNoticeMapper.insert(notice);
    }

    /**
     * 校验当前用户是否为教师，并返回教师 ID。
     */
    private Long validateCurrentTeacher() {
        Long teacherId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(teacherId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
        return teacherId;
    }

    /**
     * 校验课程归属与学生选课范围。
     */
    private void validateCourseAndStudentScope(Long teacherId, Long courseId, Long studentId) {
        requireTeacherOwnedCourse(courseId, teacherId);

        boolean matched = courseStudentMapper.findStudentIdsByCourseId(courseId).stream()
                .anyMatch(id -> id.equals(studentId));
        if (!matched) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
    }

    /**
     * 校验课程归属当前教师。
     */
    private void requireTeacherOwnedCourse(Long courseId, Long teacherId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }
    }

    /**
     * 构建汇总卡片数据。
     */
    private WarningSummaryVO buildSummary(List<WarningRankingVO> rankings, List<WarningNoticeVO> notices) {
        int highAbsenceCount = (int) rankings.stream()
                .filter(item -> defaultCount(item.getAbsenceCount()) >= 3)
                .count();

        int maxAbsenceCount = rankings.stream()
                .map(WarningRankingVO::getAbsenceCount)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        String today = LocalDate.now().toString();
        int todayNotifyCount = (int) notices.stream()
                .map(WarningNoticeVO::getSentTime)
                .filter(item -> item != null && item.startsWith(today))
                .count();

        int unreadNotifyCount = (int) notices.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsRead()))
                .count();

        return WarningSummaryVO.builder()
                .highAbsenceCount(highAbsenceCount)
                .todayNotifyCount(todayNotifyCount)
                .unreadNotifyCount(unreadNotifyCount)
                .maxAbsenceCount(maxAbsenceCount)
                .build();
    }

    /**
     * 构建内存分页结果。
     */
    private PageResult<WarningRankingVO> buildPageResult(List<WarningRankingVO> allRankings, Integer currentPage, Integer pageSize) {
        int safeCurrentPage = currentPage == null || currentPage < 1 ? 1 : currentPage;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        int fromIndex = Math.max(0, (safeCurrentPage - 1) * safePageSize);
        int toIndex = Math.min(allRankings.size(), fromIndex + safePageSize);

        List<WarningRankingVO> pageRecords = fromIndex >= allRankings.size()
                ? new ArrayList<>()
                : allRankings.subList(fromIndex, toIndex);

        return PageResult.<WarningRankingVO>builder()
                .total((long) allRankings.size())
                .records(pageRecords)
                .build();
    }

    /**
     * 规范化查询参数。
     */
    private WarningQueryDTO normalizeQuery(WarningQueryDTO queryDTO) {
        WarningQueryDTO safeQuery = queryDTO == null ? new WarningQueryDTO() : queryDTO;
        safeQuery.setKeyword(trimToNull(safeQuery.getKeyword()));
        safeQuery.setAdminClass(trimToNull(safeQuery.getAdminClass()));
        safeQuery.setStartDate(trimToNull(safeQuery.getStartDate()));
        safeQuery.setEndDate(trimToNull(safeQuery.getEndDate()));
        safeQuery.setCurrentPage(safeQuery.getCurrentPage() == null || safeQuery.getCurrentPage() < 1 ? 1 : safeQuery.getCurrentPage());
        safeQuery.setPageSize(safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1 ? 10 : Math.min(safeQuery.getPageSize(), 100));
        return safeQuery;
    }

    /**
     * 课程筛选项标签。
     */
    private String buildCourseLabel(Course course) {
        if (course.getSemester() == null || course.getSemester().isBlank()) {
            return course.getCourseName();
        }
        return course.getCourseName() + "（" + course.getSemester() + "）";
    }

    /**
     * 规范化字符串。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 默认整型值。
     */
    private Integer defaultCount(Integer value) {
        return value == null ? 0 : value;
    }
}
