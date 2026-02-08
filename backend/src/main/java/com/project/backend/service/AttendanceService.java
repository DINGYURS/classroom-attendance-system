package com.project.backend.service;

import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.vo.AttendanceSessionVO;
import com.project.backend.pojo.vo.RecognitionResultVO;
import com.project.backend.pojo.vo.SessionRecordVO;

import java.util.List;

/**
 * 考勤服务接口
 */
public interface AttendanceService {

    /**
     * 发起点名
     *
     * @param startDTO 点名参数
     * @return 会话 ID
     */
    Long startAttendance(AttendanceStartDTO startDTO);

    /**
     * 结束点名
     *
     * @param sessionId 会话 ID
     */
    void endAttendance(Long sessionId);

    /**
     * 人脸识别考勤
     *
     * @param recognitionDTO 识别请求
     * @return 识别结果列表
     */
    List<RecognitionResultVO> recognizeFaces(FaceRecognitionDTO recognitionDTO);

    /**
     * 获取考勤会话详情
     *
     * @param sessionId 会话 ID
     * @return 会话信息
     */
    AttendanceSessionVO getSessionDetail(Long sessionId);

    /**
     * 获取会话的考勤记录列表
     *
     * @param sessionId 会话 ID
     * @return 考勤记录列表
     */
    List<SessionRecordVO> getSessionRecords(Long sessionId);

    /**
     * 获取课程的考勤历史
     *
     * @param courseId 课程 ID
     * @return 会话列表
     */
    List<AttendanceSessionVO> getCourseAttendanceHistory(Long courseId);

    /**
     * 修改考勤状态
     *
     * @param updateDTO 更新请求
     */
    void updateAttendanceStatus(AttendanceUpdateDTO updateDTO);
}
