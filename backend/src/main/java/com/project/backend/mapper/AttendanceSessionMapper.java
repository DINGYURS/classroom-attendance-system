package com.project.backend.mapper;

import com.project.backend.pojo.entity.AttendanceSession;
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
