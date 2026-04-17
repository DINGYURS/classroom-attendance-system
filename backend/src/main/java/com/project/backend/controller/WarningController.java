package com.project.backend.controller;

import com.project.backend.pojo.dto.WarningNoticeSendDTO;
import com.project.backend.pojo.dto.WarningQueryDTO;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.WarningCenterPageVO;
import com.project.backend.pojo.vo.WarningDetailVO;
import com.project.backend.pojo.vo.WarningNoticeVO;
import com.project.backend.pojo.vo.WarningOptionsVO;
import com.project.backend.service.WarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预警中心控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/warning")
@Tag(name = "预警中心接口", description = "教师端缺勤排行与提醒通知相关接口")
public class WarningController {

    @Autowired
    private WarningService warningService;

    /**
     * 获取筛选项。
     */
    @GetMapping("/options")
    @Operation(summary = "获取预警中心筛选项", description = "获取教师端预警中心的课程和班级筛选项")
    public Result<WarningOptionsVO> getWarningOptions(@RequestParam(required = false) Long courseId) {
        log.info("获取预警中心筛选项: courseId={}", courseId);
        return Result.success(warningService.getWarningOptions(courseId));
    }

    /**
     * 获取分页结果。
     */
    @GetMapping("/page")
    @Operation(summary = "获取预警中心分页结果", description = "返回汇总卡片数据与缺勤排行分页结果")
    public Result<WarningCenterPageVO> getWarningPage(WarningQueryDTO queryDTO) {
        log.info("获取预警中心分页结果: {}", queryDTO);
        return Result.success(warningService.getWarningPage(queryDTO));
    }

    /**
     * 获取学生详情。
     */
    @GetMapping("/detail")
    @Operation(summary = "获取预警中心学生详情", description = "返回指定课程下学生的缺勤信息与考勤轨迹")
    public Result<WarningDetailVO> getWarningDetail(@RequestParam Long courseId,
                                                    @RequestParam Long studentId,
                                                    WarningQueryDTO queryDTO) {
        log.info("获取预警中心学生详情: courseId={}, studentId={}", courseId, studentId);
        return Result.success(warningService.getWarningDetail(courseId, studentId, queryDTO));
    }

    /**
     * 获取通知记录。
     */
    @GetMapping("/history")
    @Operation(summary = "获取通知记录", description = "返回教师端预警中心通知记录列表")
    public Result<List<WarningNoticeVO>> getNoticeHistory(WarningQueryDTO queryDTO) {
        log.info("获取预警中心通知记录: {}", queryDTO);
        return Result.success(warningService.getNoticeHistory(queryDTO));
    }

    /**
     * 发送提醒通知。
     */
    @PostMapping("/notice")
    @Operation(summary = "发送考勤提醒", description = "教师向指定学生发送考勤异常提醒")
    public Result<Void> sendNotice(@RequestBody WarningNoticeSendDTO sendDTO) {
        log.info("发送考勤提醒: courseId={}, studentId={}", sendDTO.getCourseId(), sendDTO.getStudentId());
        warningService.sendNotice(sendDTO);
        return Result.success();
    }
}
