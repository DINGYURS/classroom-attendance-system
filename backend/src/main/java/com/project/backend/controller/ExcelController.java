package com.project.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.AttendanceExportVO;
import com.project.backend.service.ExcelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    @PostMapping("/import/{courseId}")
    @Operation(summary = "导入学生名单", description = "通过 Excel 文件批量导入学生到指定课程")
    public Result<String> importStudents(@PathVariable Long courseId,
                                         @RequestParam("file") MultipartFile file) {
        log.info("导入学生名单: courseId={}, fileName={}", courseId, file.getOriginalFilename());
        String result = excelService.importStudents(courseId, file);
        return Result.success(result);
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
