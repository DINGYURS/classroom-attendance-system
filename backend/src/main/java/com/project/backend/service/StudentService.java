package com.project.backend.service;

import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.dto.StudentRegisterDTO;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentVO;

import java.util.List;

/**
 * 学生服务接口
 */
public interface StudentService {

    /**
     * 学生注册
     *
     * @param registerDTO 注册信息
     */
    void register(StudentRegisterDTO registerDTO);

    /**
     * 获取当前学生信息
     *
     * @return 学生信息
     */
    StudentVO getCurrentStudentInfo();

    /**
     * 上传人脸特征 (直接提供特征向量)
     *
     * @param faceFeatureDTO 人脸特征
     */
    void uploadFaceFeature(FaceFeatureDTO faceFeatureDTO);

    /**
     * 通过图片登记人脸 (调用 Python 服务提取特征)
     *
     * @param imageKey MinIO 中的图片存储路径
     */
    void registerFaceByImage(String imageKey);

    /**
     * 获取当前学生的考勤记录
     *
     * @return 考勤记录列表
     */
    List<AttendanceRecordVO> getAttendanceRecords();
}

