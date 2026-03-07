package com.project.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceExportVO;
import com.project.backend.pojo.vo.TeacherStudentExportVO;
import com.project.backend.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Excel 导入导出控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/excel")
@Tag(name = "Excel接口", description = "Excel 导入导出相关接口")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    /**
     * 导入学生名单
     */
    @PostMapping(value = "/import/students", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入学生名单", description = "通过 Excel 模板批量导入教师名下课程的学生名单")
    public Result<String> importStudents(@RequestParam("file") MultipartFile file) {
        log.info("导入学生名单: fileName={}", file.getOriginalFilename());
        String result = excelService.importStudents(file);
        return Result.success(result);
    }

    /**
     * 导出教师端学生名单
     */
    @GetMapping("/export/students")
    @Operation(summary = "导出学生名单", description = "导出教师端学生管理列表")
    public void exportTeacherStudentList(@RequestParam(required = false) String keyword,
                                         HttpServletResponse response) throws IOException {
        log.info("导出学生名单: keyword={}", keyword);

        List<TeacherStudentExportVO> data = excelService.exportTeacherStudentList(keyword);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("学生名单_" + LocalDate.now(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), TeacherStudentExportVO.class)
                .sheet("学生名单")
                .doWrite(data);
    }

    /**
     * 导出考勤报表
     */
    @GetMapping("/export/{courseId}")
    @Operation(summary = "导出考勤报表", description = "导出指定课程的考勤统计报表（Excel 格式）")
    public void exportAttendance(@PathVariable Long courseId, HttpServletResponse response) throws IOException {
        log.info("导出考勤报表: courseId={}", courseId);

        List<AttendanceExportVO> data = excelService.exportAttendanceReport(courseId);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("考勤报表_" + courseId, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), AttendanceExportVO.class)
                .sheet("考勤统计")
                .doWrite(data);
    }
}