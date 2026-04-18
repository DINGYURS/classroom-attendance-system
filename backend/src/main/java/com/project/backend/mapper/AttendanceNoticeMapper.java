package com.project.backend.mapper;

import com.project.backend.pojo.entity.AttendanceNotice;
import com.project.backend.pojo.vo.StudentNoticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考勤提醒通知 Mapper 接口。
 */
@Mapper
public interface AttendanceNoticeMapper {

    /**
     * 新增通知记录。
     */
    void insert(AttendanceNotice notice);

    /**
     * 查询学生端通知列表。
     */
    List<StudentNoticeVO> findByStudentId(@Param("studentId") Long studentId,
                                          @Param("status") Integer status);

    /**
     * 统计学生端未读通知数。
     */
    Integer countUnreadByStudentId(@Param("studentId") Long studentId);

    /**
     * 根据 ID 查询通知。
     */
    AttendanceNotice findById(@Param("noticeId") Long noticeId);

    /**
     * 标记通知为已读。
     */
    void markRead(@Param("noticeId") Long noticeId);
}
