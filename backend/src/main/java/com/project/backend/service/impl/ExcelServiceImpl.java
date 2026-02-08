package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.project.backend.constant.RoleConstants;
import com.project.backend.mapper.*;
import com.project.backend.pojo.dto.StudentImportDTO;
import com.project.backend.pojo.entity.*;
import com.project.backend.pojo.vo.AttendanceExportVO;
import com.project.backend.pojo.vo.StudentStatisticsVO;
import com.project.backend.service.ExcelService;
import com.project.backend.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 服务实现类
 */
@Slf4j
@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private CourseStudentMapper courseStudentMapper;

    @Autowired
    private StatisticsService statisticsService;

    @Override
    @Transactional
    public String importStudents(Long courseId, MultipartFile file) {
        try {
            List<StudentImportDTO> dataList = EasyExcel.read(file.getInputStream())
                    .head(StudentImportDTO.class)
                    .sheet()
                    .doReadSync();

            int successCount = 0;
            int skipCount = 0;
            List<String> errors = new ArrayList<>();

            for (StudentImportDTO dto : dataList) {
                try {
                    // 检查用户是否已存在
                    User existUser = userMapper.findByUsername(dto.getStudentNumber());
                    Long userId;

                    if (existUser != null) {
                        // 用户已存在，直接使用
                        userId = existUser.getUserId();
                    } else {
                        // 创建新用户
                        User user = User.builder()
                                .username(dto.getStudentNumber())
                                .password(DigestUtil.md5Hex(dto.getPassword() != null ? dto.getPassword() : "123456"))
                                .realName(dto.getRealName())
                                .role(RoleConstants.ROLE_STUDENT)
                                .createTime(LocalDateTime.now())
                                .build();
                        userMapper.insert(user);
                        userId = user.getUserId();

                        // 创建学生信息
                        Student student = Student.builder()
                                .userId(userId)
                                .studentNumber(dto.getStudentNumber())
                                .adminClass(dto.getAdminClass())
                                .gender(parseGender(dto.getGender()))
                                .build();
                        studentMapper.insert(student);
                    }

                    // 添加到课程（如果不存在）
                    List<Long> existingIds = courseStudentMapper.findStudentIdsByCourseId(courseId);
                    if (!existingIds.contains(userId)) {
                        CourseStudent cs = CourseStudent.builder()
                                .courseId(courseId)
                                .studentId(userId)
                                .joinTime(LocalDateTime.now())
                                .build();
                        List<CourseStudent> list = new ArrayList<>();
                        list.add(cs);
                        courseStudentMapper.batchInsert(list);
                        successCount++;
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    errors.add(dto.getStudentNumber() + ": " + e.getMessage());
                }
            }

            StringBuilder result = new StringBuilder();
            result.append("导入完成：成功 ").append(successCount).append(" 人，跳过 ").append(skipCount).append(" 人");
            if (!errors.isEmpty()) {
                result.append("，失败 ").append(errors.size()).append(" 人");
            }

            log.info("Excel 导入结果: {}", result);
            return result.toString();

        } catch (IOException e) {
            log.error("Excel 读取失败", e);
            return "文件读取失败：" + e.getMessage();
        }
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

    /**
     * 解析性别文字为数字
     */
    private Integer parseGender(String gender) {
        if (gender == null) return null;
        if ("男".equals(gender)) return 1;
        if ("女".equals(gender)) return 2;
        return null;
    }
}
