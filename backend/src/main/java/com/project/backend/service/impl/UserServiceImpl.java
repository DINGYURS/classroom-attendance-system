package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.project.backend.constant.JwtConstants;
import com.project.backend.constant.MessageConstants;
import com.project.backend.constant.RoleConstants;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.StudentMapper;
import com.project.backend.mapper.TeacherMapper;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.UserLoginDTO;
import com.project.backend.pojo.dto.UserRegisterDTO;
import com.project.backend.pojo.entity.Student;
import com.project.backend.pojo.entity.Teacher;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.UserLoginVO;
import com.project.backend.properties.JwtProperties;
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

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        // 根据用户名查询用户
        User user = userMapper.findByUsername(username);

        // 用户不存在
        if (user == null) {
            throw new BusinessException(MessageConstants.USER_NOT_FOUND);
        }

        // 密码校验（使用 MD5 加密比对）
        String encryptedPassword = DigestUtil.md5Hex(password);
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(MessageConstants.LOGIN_FAILED);
        }

        // 生成 JWT 令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtConstants.CLAIMS_USER_ID, user.getUserId());
        claims.put(JwtConstants.CLAIMS_USERNAME, user.getUsername());
        claims.put(JwtConstants.CLAIMS_ROLE, user.getRole());

        String token = JwtUtils.createJwt(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );

        // 构建响应
        return UserLoginVO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
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

        // 检查用户名是否已存在
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

        log.info("User registered: {}", username);
    }
}
