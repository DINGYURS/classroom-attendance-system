package com.project.backend.mapper;

import com.project.backend.pojo.entity.AttendanceRecord;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考勤明细记录 Mapper 接口
 */
@Mapper
public interface AttendanceRecordMapper {

    /**
     * 根据 ID 查询
     */
    AttendanceRecord findById(@Param("recordId") Long recordId);

    /**
     * 根据会话 ID 查询考勤记录
     */
    List<AttendanceRecord> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据学生 ID 查询考勤记录
     */
    List<AttendanceRecord> findByStudentId(@Param("studentId") Long studentId);

    /**
     * 查询指定课程下某个学生的考勤记录
     */
    List<AttendanceRecordVO> findByCourseIdAndStudentId(@Param("courseId") Long courseId,
                                                        @Param("studentId") Long studentId);

    /**
     * 批量插入考勤记录
     */
    void batchInsert(@Param("list") List<AttendanceRecord> list);

    /**
     * 更新考勤记录
     */
    void update(AttendanceRecord record);

    /**
     * 根据会话和学生更新考勤状态
     */
    void updateStatus(@Param("sessionId") Long sessionId, @Param("studentId") Long studentId,
                      @Param("status") Integer status, @Param("similarityScore") BigDecimal similarityScore);

    /**
     * 统计会话中各状态的人数
     */
    Integer countBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") Integer status);
}