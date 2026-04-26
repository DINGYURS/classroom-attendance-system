package com.project.backend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.core.util.IdUtil;
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
import com.project.backend.pojo.vo.CaptchaVO;
import com.project.backend.pojo.vo.UserLoginVO;
import com.project.backend.properties.JwtProperties;
import com.project.backend.service.MinioService;
import com.project.backend.service.UserService;
import com.project.backend.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String CAPTCHA_PREFIX = "login:captcha:";
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final long CAPTCHA_TTL_MINUTES = 5;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CaptchaVO generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        captcha.setGenerator(new RandomGenerator(CAPTCHA_CHARS, 4));
        captcha.createCode();

        String captchaKey = IdUtil.simpleUUID();
        stringRedisTemplate.opsForValue().set(
                CAPTCHA_PREFIX + captchaKey,
                captcha.getCode().toLowerCase(),
                CAPTCHA_TTL_MINUTES,
                TimeUnit.MINUTES
        );

        return CaptchaVO.builder()
                .captchaKey(captchaKey)
                .captchaImage(captcha.getImageBase64Data())
                .build();
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();
        validateCaptcha(userLoginDTO.getCaptchaKey(), userLoginDTO.getCaptchaCode());

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

        long tokenTtl = Boolean.TRUE.equals(userLoginDTO.getRememberMe()) && jwtProperties.getRememberTtl() != null
                ? jwtProperties.getRememberTtl()
                : jwtProperties.getAdminTtl();

        String token = JwtUtils.createJwt(
                jwtProperties.getAdminSecretKey(),
                tokenTtl,
                claims
        );

        String avatarUrl = null;
        Student student = null;
        if (RoleConstants.ROLE_STUDENT.equals(user.getRole())) {
            student = studentMapper.findByUserId(user.getUserId());
            if (student != null && student.getAvatarObjectKey() != null) {
                try {
                    avatarUrl = minioService.getFileUrl(student.getAvatarObjectKey());
                } catch (Exception e) {
                    log.warn("获取学生头像失败，跳过头像回填以保证登录流程继续: userId={}, objectKey={}",
                            user.getUserId(), student.getAvatarObjectKey(), e);
                }
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

    private void validateCaptcha(String captchaKey, String captchaCode) {
        if (!StringUtils.hasText(captchaKey) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(MessageConstants.CAPTCHA_ERROR);
        }

        String redisKey = CAPTCHA_PREFIX + captchaKey;
        String cacheCode = stringRedisTemplate.opsForValue().get(redisKey);
        stringRedisTemplate.delete(redisKey);
        if (!StringUtils.hasText(cacheCode) || !cacheCode.equals(captchaCode.trim().toLowerCase())) {
            throw new BusinessException(MessageConstants.CAPTCHA_ERROR);
        }
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
