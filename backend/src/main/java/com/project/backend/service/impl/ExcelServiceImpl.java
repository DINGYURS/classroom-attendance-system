package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.StudentImportDTO;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.CourseStudent;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.AttendanceExportVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;
import com.project.backend.pojo.vo.TeacherStudentExportVO;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import com.project.backend.service.ExcelService;
import com.project.backend.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Excel 服务实现类
 */
@Slf4j
@Service
public class ExcelServiceImpl implements ExcelService {

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Override
    @Transactional
    public String importStudents(MultipartFile file) {
        Long teacherId = validateCurrentTeacher();
        validateFile(file);

        List<StudentImportDTO> dataList = readImportData(file);
        if (dataList.isEmpty()) {
            return "导入文件中没有可用数据";
        }

        Map<String, List<Course>> teacherCourseMap = buildTeacherCourseMap(teacherId);
        int successCount = 0;
        int skipCount = 0;
        Map<Long, Set<Long>> courseStudentCache = new HashMap<>();

        for (int i = 0; i < dataList.size(); i++) {
            StudentImportDTO dto = sanitizeRow(dataList.get(i));
            int rowNum = i + 2;

            try {
                validateRow(dto);
                Long userId = upsertStudent(dto);
                Course course = resolveCourse(teacherId, dto, teacherCourseMap);
                Set<Long> existingStudentIds = courseStudentCache.computeIfAbsent(
                        course.getCourseId(),
                        courseId -> new HashSet<>(courseStudentMapper.findStudentIdsByCourseId(courseId))
                );
                if (!existingStudentIds.add(userId)) {
                    skipCount++;
                    continue;
                }

                CourseStudent courseStudent = CourseStudent.builder()
                        .courseId(course.getCourseId())
                        .studentId(userId)
                        .joinTime(LocalDateTime.now())
                        .build();
                courseStudentMapper.batchInsert(List.of(courseStudent));
                successCount++;
            } catch (BusinessException e) {
                throw new BusinessException("第" + rowNum + "行导入失败，已回滚本次导入：" + e.getMessage());
            } catch (Exception e) {
                log.warn("导入学生名单时处理第 {} 行失败", rowNum, e);
                throw new BusinessException("第" + rowNum + "行导入失败，已回滚本次导入");
            }
        }

        String result = buildImportResult(successCount, skipCount);
        log.info("Excel 导入结果: {}", result);
        return result;
    }

    @Override
    public List<TeacherStudentExportVO> exportTeacherStudentList(String keyword) {
        Long teacherId = validateCurrentTeacher();
        List<TeacherStudentTableVO> records = courseStudentMapper.pageTeacherStudents(teacherId, normalizeKeyword(keyword));

        List<TeacherStudentExportVO> result = new ArrayList<>();
        for (TeacherStudentTableVO record : records) {
            result.add(TeacherStudentExportVO.builder()
                    .courseName(record.getCourseName())
                    .semester(record.getSemester())
                    .studentId(record.getStudentId())
                    .realName(record.getRealName())
                    .gender(record.getGender())
                    .className(record.getClassName())
                    .build());
        }
        return result;
    }

    @Override
    public List<AttendanceExportVO> exportAttendanceReport(Long courseId) {
        List<StudentStatisticsVO> statistics = statisticsService.getStudentStatistics(courseId);

        List<AttendanceExportVO> result = new ArrayList<>();
        for (StudentStatisticsVO stat : statistics) {
            result.add(AttendanceExportVO.builder()
                    .studentNumber(stat.getStudentNumber())
                    .realName(stat.getRealName())
                    .adminClass(stat.getAdminClass())
                    .presentCount(stat.getPresentCount())
                    .lateCount(stat.getLateCount())
                    .absentCount(stat.getAbsentCount())
                    .leaveCount(stat.getLeaveCount())
                    .attendanceRate(stat.getAttendanceRate() != null ? stat.getAttendanceRate().toString() : "0")
                    .build());
        }

        return result;
    }

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

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 Excel 文件");
        }
    }

    private List<StudentImportDTO> readImportData(MultipartFile file) {
        try {
            return EasyExcel.read(file.getInputStream())
                    .head(StudentImportDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (IOException e) {
            log.error("读取 Excel 文件失败", e);
            throw new BusinessException("读取 Excel 文件失败：" + e.getMessage());
        }
    }

    private Map<String, List<Course>> buildTeacherCourseMap(Long teacherId) {
        Map<String, List<Course>> courseMap = new HashMap<>();
        List<Course> teacherCourses = courseMapper.findByTeacherId(teacherId);
        if (teacherCourses == null || teacherCourses.isEmpty()) {
            return courseMap;
        }

        for (Course course : teacherCourses) {
            String courseKey = normalize(course.getCourseName());
            courseMap.computeIfAbsent(courseKey, key -> new ArrayList<>()).add(course);
        }
        return courseMap;
    }

    private StudentImportDTO sanitizeRow(StudentImportDTO dto) {
        if (dto == null) {
            return new StudentImportDTO();
        }
        return StudentImportDTO.builder()
                .courseName(normalize(dto.getCourseName()))
                .semester(normalize(dto.getSemester()))
                .studentNumber(normalizeStudentNumber(dto.getStudentNumber()))
                .realName(normalize(dto.getRealName()))
                .gender(normalize(dto.getGender()))
                .adminClass(normalize(dto.getAdminClass()))
                .build();
    }

    private void validateRow(StudentImportDTO dto) {
        if (!StringUtils.hasText(dto.getCourseName())) {
            throw new BusinessException("课程名称不能为空");
        }
        if (!StringUtils.hasText(dto.getStudentNumber())) {
            throw new BusinessException("学号不能为空");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            throw new BusinessException("真实姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getAdminClass())) {
            throw new BusinessException("行政班级不能为空");
        }
    }

    private Course resolveCourse(Long teacherId, StudentImportDTO dto, Map<String, List<Course>> courseMap) {
        String courseKey = normalize(dto.getCourseName());
        List<Course> courseList = courseMap.get(courseKey);
        if (courseList == null || courseList.isEmpty()) {
            Course course = Course.builder()
                    .courseName(dto.getCourseName())
                    .teacherId(teacherId)
                    .semester(dto.getSemester())
                    .createTime(LocalDateTime.now())
                    .build();
            courseMapper.insert(course);
            courseMap.computeIfAbsent(courseKey, key -> new ArrayList<>()).add(course);
            return course;
        }

        if (StringUtils.hasText(dto.getSemester())) {
            for (Course course : courseList) {
                if (Objects.equals(normalize(course.getSemester()), dto.getSemester())) {
                    return course;
                }
            }
        }

        return courseList.getFirst();
    }

    private Long upsertStudent(StudentImportDTO dto) {
        User existUser = userMapper.findByUsername(dto.getStudentNumber());
        Integer gender = parseGender(dto.getGender());

        if (existUser != null && !RoleConstants.ROLE_STUDENT.equals(existUser.getRole())) {
            throw new BusinessException("学号已被非学生账号占用：" + dto.getStudentNumber());
        }

        Long userId;
        if (existUser == null) {
            User user = User.builder()
                    .username(dto.getStudentNumber())
                    .password(DigestUtil.md5Hex(DEFAULT_PASSWORD))
                    .realName(dto.getRealName())
                    .role(RoleConstants.ROLE_STUDENT)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            userId = user.getUserId();

            Student student = Student.builder()
                    .userId(userId)
                    .studentNumber(dto.getStudentNumber())
                    .adminClass(dto.getAdminClass())
                    .gender(gender)
                    .build();
            studentMapper.insert(student);
            return userId;
        }

        userId = existUser.getUserId();
        updateUserIfNecessary(existUser, dto.getRealName());

        Student existStudent = studentMapper.findByUserId(userId);
        if (existStudent == null) {
            Student student = Student.builder()
                    .userId(userId)
                    .studentNumber(dto.getStudentNumber())
                    .adminClass(dto.getAdminClass())
                    .gender(gender)
                    .build();
            studentMapper.insert(student);
            return userId;
        }

        updateStudentIfNecessary(existStudent, dto, gender);
        return userId;
    }

    private void updateUserIfNecessary(User existUser, String realName) {
        if (StringUtils.hasText(realName) && !Objects.equals(realName, existUser.getRealName())) {
            userMapper.update(User.builder()
                    .userId(existUser.getUserId())
                    .realName(realName)
                    .build());
        }
    }

    private void updateStudentIfNecessary(Student existStudent, StudentImportDTO dto, Integer gender) {
        boolean needUpdate = false;
        Student.StudentBuilder builder = Student.builder()
                .userId(existStudent.getUserId());

        if (!Objects.equals(dto.getStudentNumber(), existStudent.getStudentNumber())) {
            builder.studentNumber(dto.getStudentNumber());
            needUpdate = true;
        }
        if (!Objects.equals(dto.getAdminClass(), existStudent.getAdminClass())) {
            builder.adminClass(dto.getAdminClass());
            needUpdate = true;
        }
        if (gender != null && !Objects.equals(gender, existStudent.getGender())) {
            builder.gender(gender);
            needUpdate = true;
        }

        if (needUpdate) {
            studentMapper.update(builder.build());
        }
    }

    private Integer parseGender(String gender) {
        if (!StringUtils.hasText(gender)) {
            return null;
        }

        return switch (gender.trim()) {
            case "男", "1", "M", "m", "male", "Male" -> 1;
            case "女", "2", "F", "f", "female", "Female" -> 2;
            default -> throw new BusinessException("性别仅支持填写“男”或“女”");
        };
    }

    private String buildImportResult(int successCount, int skipCount) {
        return new StringBuilder()
                .append("导入完成：成功 ")
                .append(successCount)
                .append(" 人，跳过 ")
                .append(skipCount)
                .append(" 人")
                .toString();
    }

    private String normalizeKeyword(String keyword) {
        String normalized = normalize(keyword);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeStudentNumber(String value) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        if (normalized.matches("^-?\\d+\\.0$")) {
            return normalized.substring(0, normalized.length() - 2);
        }
        return normalized;
    }
}