package com.project.backend.mapper;

import com.project.backend.pojo.entity.AttendanceDetection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考勤检测结果 Mapper
 */
@Mapper
public interface AttendanceDetectionMapper {

    /**
     * 根据会话 ID 查询检测结果
     */
    List<AttendanceDetection> findBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据 ID 查询检测结果
     */
    AttendanceDetection findById(@Param("detectionId") Long detectionId);

    /**
     * 查询考勤记录已绑定的检测框
     */
    AttendanceDetection findActiveByRecordId(@Param("recordId") Long recordId);

    /**
     * 批量插入检测结果
     */
    void batchInsert(@Param("list") List<AttendanceDetection> list);

    /**
     * 标记检测框为已忽略
     */
    void markIgnored(@Param("detectionId") Long detectionId, @Param("ignoreReason") String ignoreReason);

    /**
     * 人工指派检测框到学生
     */
    void assignStudent(@Param("detectionId") Long detectionId,
                       @Param("studentId") Long studentId,
                       @Param("recordId") Long recordId);

    /**
     * 删除会话下的旧检测结果
     */
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
