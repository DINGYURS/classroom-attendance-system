package com.project.backend.service;

import com.project.backend.pojo.dto.WarningNoticeSendDTO;
import com.project.backend.pojo.dto.WarningQueryDTO;
import com.project.backend.pojo.vo.WarningCenterPageVO;
import com.project.backend.pojo.vo.WarningDetailVO;
import com.project.backend.pojo.vo.WarningNoticeVO;
import com.project.backend.pojo.vo.WarningOptionsVO;

import java.util.List;

/**
 * 预警中心服务接口。
 */
public interface WarningService {

    /**
     * 获取预警中心筛选项。
     */
    WarningOptionsVO getWarningOptions(Long courseId);

    /**
     * 获取预警中心分页结果。
     */
    WarningCenterPageVO getWarningPage(WarningQueryDTO queryDTO);

    /**
     * 获取预警中心学生详情。
     */
    WarningDetailVO getWarningDetail(Long courseId, Long studentId, WarningQueryDTO queryDTO);

    /**
     * 获取通知记录列表。
     */
    List<WarningNoticeVO> getNoticeHistory(WarningQueryDTO queryDTO);

    /**
     * 发送考勤提醒。
     */
    void sendNotice(WarningNoticeSendDTO sendDTO);
}
