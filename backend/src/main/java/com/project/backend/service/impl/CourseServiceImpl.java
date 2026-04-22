package com.project.backend.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.AttendanceSessionMapper;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.CourseDTO;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.CourseStudent;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.dto.TeacherStudentPageQueryDTO;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.vo.CourseStudentVO;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import com.project.backend.pojo.vo.CourseVO;
import com.project.backend.service.CourseService;
import com.project.backend.service.MinioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 课程服务实现类
 */
@Slf4j
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private AttendanceSessionMapper attendanceSessionMapper;

    @Autowired
    private MinioService minioService;

    @Override
    public Long createCourse(CourseDTO courseDTO) {
        Long teacherId = validateCurrentTeacher();

        Course course = Course.builder()
                .courseName(courseDTO.getCourseName())
                .teacherId(teacherId)
                .semester(courseDTO.getSemester())
                .description(courseDTO.getDescription())
                .createTime(LocalDateTime.now())
                .build();

        courseMapper.insert(course);
        log.info("课程创建成功: {} by {}", course.getCourseId(), teacherId);
        return course.getCourseId();
    }

    @Override
    public void updateCourse(CourseDTO courseDTO) {
        Long teacherId = validateCurrentTeacher();
        requireTeacherOwnedCourse(courseDTO.getCourseId(), teacherId);

        Course course = Course.builder()
                .courseId(courseDTO.getCourseId())
                .courseName(courseDTO.getCourseName())
                .semester(courseDTO.getSemester())
                .description(courseDTO.getDescription())
                .build();

        courseMapper.update(course);
        log.info("课程更新成功: {}", courseDTO.getCourseId());
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        requireTeacherOwnedCourse(courseId, teacherId);

        courseStudentMapper.deleteByCourseId(courseId);
        courseMapper.deleteById(courseId);

        log.info("课程删除成功: {}", courseId);
    }

    @Override
    public List<CourseVO> getMyCourses() {
        Long teacherId = validateCurrentTeacher();
        List<Course> courses = courseMapper.findByTeacherId(teacherId);

        List<CourseVO> result = new ArrayList<>();
        for (Course course : courses) {
            Integer studentCount = courseStudentMapper.countByCourseId(course.getCourseId());
            List<String> classes = studentMapper.findAdminClassesByCourseId(course.getCourseId());
            result.add(CourseVO.builder()
                    .courseId(course.getCourseId())
                    .courseName(course.getCourseName())
                    .semester(course.getSemester())
                    .description(course.getDescription())
                    .studentCount(studentCount != null ? studentCount : 0)
                    .classes(classes)
                    .attendanceRate(calculateLatestAttendanceRate(course.getCourseId()))
                    .build());
        }

        return result;
    }

    @Override
    public CourseVO getCourseDetail(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        Course course = requireTeacherOwnedCourse(courseId, teacherId);

        Integer studentCount = courseStudentMapper.countByCourseId(courseId);
        List<String> classes = studentMapper.findAdminClassesByCourseId(courseId);

        return CourseVO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .semester(course.getSemester())
                .description(course.getDescription())
                .studentCount(studentCount != null ? studentCount : 0)
                .classes(classes)
                .attendanceRate(calculateLatestAttendanceRate(courseId))
                .build();
    }

    @Override
    public PageResult<TeacherStudentTableVO> getCourseStudentPage(Long courseId, TeacherStudentPageQueryDTO queryDTO) {
        if (courseId == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        validateTeacherCoursePermission(courseId);

        int currentPage = queryDTO.getCurrentPage() == null || queryDTO.getCurrentPage() < 1 ? 1 : queryDTO.getCurrentPage();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1 ? 10 : Math.min(queryDTO.getPageSize(), 100);
        String keyword = normalizeKeyword(queryDTO.getKeyword());

        try (Page<TeacherStudentTableVO> page = PageHelper.startPage(currentPage, pageSize)) {
            List<TeacherStudentTableVO> records = courseStudentMapper.pageCourseStudents(courseId, keyword);
            records.forEach(this::fillTeacherStudentAvatarUrl);

            return PageResult.<TeacherStudentTableVO>builder()
                    .total(page.getTotal())
                    .records(records)
                    .build();
        }
    }

    @Override
    public List<CourseStudentVO> getCourseStudents(Long courseId) {
        validateTeacherCoursePermission(courseId);
        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(courseId);
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Student> students = studentMapper.findByUserIds(studentIds);
        List<CourseStudentVO> result = new ArrayList<>();

        for (Student student : students) {
            User user = userMapper.findById(student.getUserId());
            if (user != null) {
                result.add(CourseStudentVO.builder()
                        .userId(student.getUserId())
                        .studentNumber(student.getStudentNumber())
                        .realName(user.getRealName())
                        .adminClass(student.getAdminClass())
                        .gender(student.getGender())
                        .hasFaceFeature(student.getFeatureVector() != null && !student.getFeatureVector().isEmpty())
                        .build());
            }
        }

        return result;
    }

    @Override
    public void addStudentToCourse(Long courseId, Long studentId) {
        validateTeacherCoursePermission(courseId);
        CourseStudent cs = CourseStudent.builder()
                .courseId(courseId)
                .studentId(studentId)
                .joinTime(LocalDateTime.now())
                .build();

        List<CourseStudent> list = new ArrayList<>();
        list.add(cs);
        courseStudentMapper.batchInsert(list);

        log.info("学生 {} 添加到课程 {}", studentId, courseId);
    }

    @Override
    public void removeStudentFromCourse(Long courseId, Long studentId) {
        validateTeacherCoursePermission(courseId);
        courseStudentMapper.delete(courseId, studentId);
        log.info("学生 {} 从课程 {} 移除", studentId, courseId);
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
     * 校验当前登录教师是否有权限访问指定课程。
     */
    private void validateTeacherCoursePermission(Long courseId) {
        Long teacherId = validateCurrentTeacher();
        requireTeacherOwnedCourse(courseId, teacherId);
    }

    /**
     * 校验指定课程属于当前教师。
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
            log.warn("获取课程学生头像失败，忽略头像展示: courseId={}, userId={}, objectKey={}",
                    record.getCourseId(), record.getUserId(), objectKey, e);
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

    private Double calculateLatestAttendanceRate(Long courseId) {
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        AttendanceSession latestSession = sessions.getFirst();
        Integer totalStudent = latestSession.getTotalStudent();
        if (totalStudent == null || totalStudent <= 0) {
            return 0.0;
        }

        int actualStudent = latestSession.getActualStudent() != null ? latestSession.getActualStudent() : 0;
        return BigDecimal.valueOf(actualStudent)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalStudent), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
