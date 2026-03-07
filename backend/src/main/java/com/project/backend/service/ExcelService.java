package com.project.backend.service;

import com.project.backend.pojo.vo.AttendanceExportVO;
import com.project.backend.pojo.vo.TeacherStudentExportVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel 服务接口
 */
public interface ExcelService {

    /**
     * 按照模板批量导入教师名下课程的学生名单
     *
     * @param file Excel 文件
     * @return 导入结果信息
     */
    String importStudents(MultipartFile file);

    /**
     * 导出教师端学生名单
     *
     * @param keyword 搜索关键词
     * @return 学生名单数据
     */
    List<TeacherStudentExportVO> exportTeacherStudentList(String keyword);

    /**
     * 导出课程考勤报表
     *
     * @param courseId 课程 ID
     * @return 考勤数据列表
     */
    List<AttendanceExportVO> exportAttendanceReport(Long courseId);
}