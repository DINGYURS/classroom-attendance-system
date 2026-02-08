package com.project.backend.service;

import com.project.backend.pojo.dto.StudentImportDTO;
import com.project.backend.pojo.vo.AttendanceExportVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Excel 服务接口
 */
public interface ExcelService {

    /**
     * 批量导入学生到课程
     *
     * @param courseId 课程 ID
     * @param file     Excel 文件
     * @return 导入结果信息
     */
    String importStudents(Long courseId, MultipartFile file);

    /**
     * 导出课程考勤报表
     *
     * @param courseId 课程 ID
     * @return 考勤数据列表
     */
    List<AttendanceExportVO> exportAttendanceReport(Long courseId);
}
