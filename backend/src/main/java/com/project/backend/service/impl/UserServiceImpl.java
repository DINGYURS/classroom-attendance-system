package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.project.backend.constant.JwtConstants;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.context.BaseContext;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.TeacherMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.StudentUpdateDTO;
import com.project.backend.pojo.dto.TeacherUpdateDTO;
import com.project.backend.pojo.dto.UserLoginDTO;
import com.project.backend.pojo.dto.UserRegisterDTO;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.Teacher;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.UserLoginVO;
import com.project.backend.properties.JwtProperties;
import com.project.backend.service.MinioService;
import com.project.backend.service.UserService;
import com.project.backend.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private TeacherMapper teacherMapper;

    @Autowired
    private MinioService minioService;

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }

        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(MessageConstants.LOGIN_FAILED);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIMS_USER_ID, user.getUserId());
        claims.put(JwtConstants.CLAIMS_USERNAME, user.getUsername());
        claims.put(JwtConstants.CLAIMS_ROLE, user.getRole());

        String token = JwtUtils.createJwt(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        String avatarUrl = null;
        Student student = null;
        if (RoleConstants.ROLE_STUDENT.equals(user.getRole())) {
            student = studentMapper.findByUserId(user.getUserId());
            if (student != null && student.getAvatarUrl() != null) {
                avatarUrl = minioService.getFileUrl(student.getAvatarUrl());
            }
        }

        return UserLoginVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .avatarUrl(avatarUrl)
                .adminClass(student != null ? student.getAdminClass() : null)
                .token(token)
                .build();
    }

    @Override
    @Transactional
    public void register(UserRegisterDTO userRegisterDTO) {
        String username = userRegisterDTO.getUsername();
        Integer role = userRegisterDTO.getRole();

        if (username == null || userRegisterDTO.getPassword() == null || userRegisterDTO.getRealName() == null || role == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(role) && !RoleConstants.ROLE_STUDENT.equals(role)) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        User existUser = userMapper.findByUsername(username);
        if (existUser != null) {
            throw new BusinessException(MessageConstants.USER_EXISTS);
        }

        User user = User.builder()
                .username(username)
                .password(DigestUtil.md5Hex(userRegisterDTO.getPassword()))
                .realName(userRegisterDTO.getRealName())
                .role(role)
                .createTime(LocalDateTime.now())
                .build();
        userMapper.insert(user);

        if (RoleConstants.ROLE_STUDENT.equals(role)) {
            Student student = Student.builder()
                    .userId(user.getUserId())
                    .studentNumber(username)
                    .adminClass(userRegisterDTO.getAdminClass())
                    .gender(userRegisterDTO.getGender())
                    .build();
            studentMapper.insert(student);
        } else {
            Teacher teacher = Teacher.builder()
                    .userId(user.getUserId())
                    .jobNumber(username)
                    .build();
            teacherMapper.insert(teacher);
        }

        log.info("用户注册成功: {}", username);
    }

    @Override
    @Transactional
    public void updateTeacherInfo(TeacherUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        Long userId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(userId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_TEACHER.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        boolean hasUpdate = updateDTO.getJobNumber() != null
                || updateDTO.getPassword() != null
                || updateDTO.getRealName() != null;
        if (!hasUpdate) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        if (updateDTO.getJobNumber() != null) {
            User existUser = userMapper.findByUsername(updateDTO.getJobNumber());
            if (existUser != null && !existUser.getUserId().equals(userId)) {
                throw new BusinessException(MessageConstants.USER_EXISTS);
            }
        }

        User updateUser = User.builder()
                .userId(userId)
                .username(updateDTO.getJobNumber())
                .realName(updateDTO.getRealName())
                .password(updateDTO.getPassword() != null ? DigestUtil.md5Hex(updateDTO.getPassword()) : null)
                .build();
        userMapper.update(updateUser);

        if (updateDTO.getJobNumber() != null) {
            Teacher teacher = Teacher.builder()
                    .userId(userId)
                    .jobNumber(updateDTO.getJobNumber())
                    .build();
            teacherMapper.update(teacher);
        }

        log.info("教师信息更新成功: {}", userId);
    }

    @Override
    @Transactional
    public void updateStudentInfo(StudentUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        Long userId = BaseContext.getCurrentId();
        User currentUser = userMapper.findById(userId);
        if (currentUser == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }
        if (!RoleConstants.ROLE_STUDENT.equals(currentUser.getRole())) {
            throw new BusinessException(MessageConstants.NO_PERMISSION);
        }

        boolean hasUpdate = updateDTO.getPassword() != null
                || updateDTO.getRealName() != null
                || updateDTO.getAvatarUrl() != null;
        if (!hasUpdate) {
            throw new BusinessException(MessageConstants.PARAM_ERROR);
        }

        User updateUser = User.builder()
                .userId(userId)
                .password(updateDTO.getPassword() != null ? DigestUtil.md5Hex(updateDTO.getPassword()) : null)
                .realName(updateDTO.getRealName())
                .build();
        userMapper.update(updateUser);

        if (updateDTO.getAvatarUrl() != null) {
            studentMapper.updateFaceImageKey(userId, updateDTO.getAvatarUrl());
        }

        log.info("学生信息更新成功: {}", userId);
    }
}
