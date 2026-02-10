package com.project.backend.service;

import com.project.backend.pojo.dto.UserLoginDTO;
import com.project.backend.pojo.dto.UserRegisterDTO;
import com.project.backend.pojo.dto.StudentUpdateDTO;
import com.project.backend.pojo.dto.TeacherUpdateDTO;
import com.project.backend.pojo.vo.UserLoginVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param userLoginDTO 登录请求
     * @return 登录响应（含 token）
     */
    UserLoginVO login(UserLoginDTO userLoginDTO);

    /**
     * 用户注册
     *
     * @param userRegisterDTO 注册信息
     */
    void register(UserRegisterDTO userRegisterDTO);

    /**
     * 修改教师信息
     *
     * @param updateDTO 修改信息
     */
    void updateTeacherInfo(TeacherUpdateDTO updateDTO);

    /**
     * 修改学生信息
     *
     * @param updateDTO 修改信息
     */
    void updateStudentInfo(StudentUpdateDTO updateDTO);
}
