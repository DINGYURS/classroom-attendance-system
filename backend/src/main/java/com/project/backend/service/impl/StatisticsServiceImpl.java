package com.project.backend.service.impl;

import com.project.backend.constant.AttendanceStatus;
import com.project.backend.mapper.*;
import com.project.backend.pojo.entity.*;
import com.project.backend.pojo.vo.CourseStatisticsVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;
import com.project.backend.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

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
    public CourseStatisticsVO getCourseStatistics(Long courseId) {
        Course course = courseMapper.findById(courseId);
        if (course == null) {
            return null;
        }

        // 获取课程学生数
        Integer totalStudents = courseStudentMapper.countByCourseId(courseId);

        // 获取点名会话数
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        int totalSessions = sessions.size();

        // 统计各状态人次
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

        // 计算平均出勤率
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
        List<Long> studentIds = courseStudentMapper.findStudentIdsByCourseId(courseId);
        if (studentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取课程所有点名会话
        List<AttendanceSession> sessions = attendanceSessionMapper.findByCourseId(courseId);
        List<Long> sessionIds = sessions.stream().map(AttendanceSession::getSessionId).toList();

        List<StudentStatisticsVO> result = new ArrayList<>();

        for (Long studentId : studentIds) {
            Student student = studentMapper.findByUserId(studentId);
            User user = userMapper.findById(studentId);

            if (student == null || user == null) {
                continue;
            }

            // 统计该学生在所有会话中的考勤情况
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
}
