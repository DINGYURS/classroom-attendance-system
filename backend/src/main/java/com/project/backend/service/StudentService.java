package com.project.backend.service;

import com.project.backend.pojo.dto.FaceFeatureDTO;
import com.project.backend.pojo.dto.TeacherStudentPageQueryDTO;
import com.project.backend.pojo.result.PageResult;
import com.project.backend.pojo.vo.AttendanceRecordVO;
import com.project.backend.pojo.vo.StudentVO;
import com.project.backend.pojo.vo.TeacherStudentTableVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学生相关服务接口。
 */
public interface StudentService {

    /**
     * 获取当前登录学生信息。
     */
    StudentVO getCurrentStudentInfo();

    /**
     * 上传人脸特征向量。
     */
    void uploadFaceFeature(FaceFeatureDTO faceFeatureDTO);

    /**
     * 通过图片注册人脸。
     */
    void registerFaceByImage(MultipartFile file);

    /**
     * 获取当前学生考勤记录。
     */
    List<AttendanceRecordVO> getAttendanceRecords();

    /**
     * 分页查询教师端学生管理表格数据。
     */
    PageResult<TeacherStudentTableVO> getTeacherStudentPage(TeacherStudentPageQueryDTO queryDTO);
}
