package com.project.backend.service;

import com.project.backend.pojo.dto.AttendanceStartDTO;
import com.project.backend.pojo.dto.AttendanceUpdateDTO;
import com.project.backend.pojo.dto.AttendanceArchiveQueryDTO;
import com.project.backend.pojo.dto.FaceRecognitionDTO;
import com.project.backend.pojo.vo.AttendanceArchiveCourseSummaryExportVO;
import com.project.backend.pojo.vo.AttendanceArchiveExportVO;
import com.project.backend.pojo.vo.AttendanceArchiveOptionsVO;
import com.project.backend.pojo.vo.AttendanceArchivePageVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionDetailVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionExportVO;
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

    /**
     * 获取考勤档案筛选项。
     *
     * @param courseId 已选课程 ID，可为空
     * @return 筛选项
     */
    AttendanceArchiveOptionsVO getArchiveOptions(Long courseId);

    /**
     * 查询考勤档案分页数据。
     *
     * @param queryDTO 查询条件
     * @return 考勤档案分页结果
     */
    AttendanceArchivePageVO getArchivePage(AttendanceArchiveQueryDTO queryDTO);

    /**
     * 查询单次考勤会话详情。
     *
     * @param sessionId 会话 ID
     * @return 会话详情
     */
    AttendanceArchiveSessionDetailVO getArchiveSessionDetail(Long sessionId);

    /**
     * 导出当前筛选结果明细。
     *
     * @param queryDTO 查询条件
     * @return 明细数据
     */
    List<AttendanceArchiveExportVO> listArchiveExportData(AttendanceArchiveQueryDTO queryDTO);

    /**
     * 导出课程汇总结果。
     *
     * @param queryDTO 查询条件
     * @return 汇总数据
     */
    List<AttendanceArchiveCourseSummaryExportVO> listArchiveSummaryExportData(AttendanceArchiveQueryDTO queryDTO);

    /**
     * 导出单次会话详情。
     *
     * @param sessionId 会话 ID
     * @return 单次会话明细
     */
    List<AttendanceArchiveSessionExportVO> listArchiveSessionExportData(Long sessionId);
}
