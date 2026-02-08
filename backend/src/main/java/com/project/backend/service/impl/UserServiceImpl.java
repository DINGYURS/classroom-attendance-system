package com.project.backend.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.project.backend.constant.JwtConstants;
import com.project.backend.constant.MessageConstants;
import com.project.backend.exception.BusinessException;
import com.project.backend.mapper.UserMapper;
import com.project.backend.pojo.dto.UserLoginDTO;
import com.project.backend.pojo.entity.User;
import com.project.backend.pojo.vo.UserLoginVO;
import com.project.backend.properties.JwtProperties;
import com.project.backend.service.UserService;
import com.project.backend.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
