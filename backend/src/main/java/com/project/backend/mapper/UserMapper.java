package com.project.backend.mapper;

import com.project.backend.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户 Mapper 接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据用户 ID 查询用户
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    User findById(@Param("userId") Long userId);

    /**
     * 用户注册
     *
     * @param user 用户信息
     */
    void insert(User user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     */
    void update(User user);
}
