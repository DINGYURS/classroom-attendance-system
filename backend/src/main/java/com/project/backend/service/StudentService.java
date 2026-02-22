package com.project.backend.service;

import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生服务接口
 */
public interface StudentService {

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
     * 通过图片登记人脸：上传图片到 MinIO，调用 Python 提取特征，存入数据库
     *
     * @param file 人脸图片文件
     */
    void registerFaceByImage(MultipartFile file);

    /**
     * 获取当前学生的考勤记录
     *
     * @return 考勤记录列表
     */
    List<AttendanceRecordVO> getAttendanceRecords();
}

