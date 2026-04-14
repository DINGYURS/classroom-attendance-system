package com.project.backend.mapper;

import com.project.backend.pojo.dto.AttendanceArchiveQueryDTO;
import com.project.backend.pojo.entity.AttendanceSession;
import com.project.backend.pojo.vo.AttendanceArchiveSummaryVO;
import com.project.backend.pojo.vo.AttendanceArchiveSessionVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考勤会话 Mapper 接口
 */
@Mapper
public interface AttendanceSessionMapper {

    /**
     * 根据会话 ID 查询
     *
     * @param sessionId 会话 ID
     * @return 会话信息
     */
    AttendanceSession findById(@Param("sessionId") Long sessionId);

    /**
     * 根据课程 ID 查询会话列表
     *
     * @param courseId 课程 ID
     * @return 会话列表
     */
    List<AttendanceSession> findByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据课程 ID 列表查询会话列表
     *
     * @param courseIds 课程 ID 列表
     * @return 会话列表
     */
    List<AttendanceSession> findByCourseIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 分页查询教师端考勤档案会话 ID 列表。
     *
     * @param teacherId 教师 ID
     * @param queryDTO  查询条件
     * @return 会话 ID 列表
     */
    List<Long> pageArchiveSessionIds(@Param("teacherId") Long teacherId,
                                     @Param("query") AttendanceArchiveQueryDTO queryDTO);

    /**
     * 根据会话 ID 列表批量查询教师端考勤档案会话展示数据。
     *
     * @param sessionIds 会话 ID 列表
     * @return 会话展示数据
     */
    List<AttendanceArchiveSessionVO> listArchiveSessionsByIds(@Param("sessionIds") List<Long> sessionIds);

    /**
     * 查询教师端考勤档案顶部汇总数据。
     *
     * @param teacherId 教师 ID
     * @param queryDTO  查询条件
     * @return 汇总结果
     */
    AttendanceArchiveSummaryVO getArchiveSummary(@Param("teacherId") Long teacherId,
                                                 @Param("query") AttendanceArchiveQueryDTO queryDTO);

    /**
     * 插入会话
     *
     * @param session 会话信息
     */
    void insert(AttendanceSession session);

    /**
     * 更新会话（如更新实到人数）
     *
     * @param session 会话信息
     */
    void update(AttendanceSession session);
}
