package com.project.backend.service;

import com.project.backend.pojo.dto.UserLoginDTO;
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
}
