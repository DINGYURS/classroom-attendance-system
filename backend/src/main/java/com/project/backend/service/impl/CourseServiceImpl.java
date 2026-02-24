package com.project.backend.service.impl;

import com.project.backend.constant.MessageConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.CourseMapper;
import com.project.backend.mapper.CourseStudentMapper;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.CourseDTO;
import com.project.backend.pojo.entity.Course;
import com.project.backend.pojo.entity.CourseStudent;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.CourseStudentVO;
import com.project.backend.pojo.vo.CourseVO;
import com.project.backend.service.CourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public Long createCourse(CourseDTO courseDTO) {
        Long teacherId = BaseContext.getCurrentId();

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
        Long teacherId = BaseContext.getCurrentId();
        Course existing = courseMapper.findById(courseDTO.getCourseId());

        if (existing == null) {
            throw new BusinessException("课程不存在");
        }
        if (!existing.getTeacherId().equals(teacherId)) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

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
        Long teacherId = BaseContext.getCurrentId();
        Course existing = courseMapper.findById(courseId);

        if (existing == null) {
            throw new BusinessException("课程不存在");
        }
        if (!existing.getTeacherId().equals(teacherId)) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        courseStudentMapper.deleteByCourseId(courseId);
        courseMapper.deleteById(courseId);

        log.info("课程删除成功: {}", courseId);
    }

    @Override
    public List<CourseVO> getMyCourses() {
        Long teacherId = BaseContext.getCurrentId();
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
                    .attendanceRate(100.0)
                    .build());
        }

        return result;
    }

    @Override
    public CourseVO getCourseDetail(Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }

        Integer studentCount = courseStudentMapper.countByCourseId(courseId);
        List<String> classes = studentMapper.findAdminClassesByCourseId(courseId);

        return CourseVO.builder()
                .courseId(course.getCourseId())
                .courseName(course.getCourseName())
                .semester(course.getSemester())
                .description(course.getDescription())
                .studentCount(studentCount != null ? studentCount : 0)
                .classes(classes)
                .build();
    }

    @Override
    public List<CourseStudentVO> getCourseStudents(Long courseId) {
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
        courseStudentMapper.delete(courseId, studentId);
        log.info("学生 {} 从课程 {} 移除", studentId, courseId);
    }
}
