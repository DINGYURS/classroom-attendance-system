package com.project.backend.controller;

import com.project.backend.pojo.dto.StudentUpdateDTO;
import com.project.backend.pojo.dto.TeacherUpdateDTO;
import com.project.backend.pojo.dto.UserLoginDTO;
import com.project.backend.pojo.dto.UserRegisterDTO;
import com.project.backend.pojo.result.Result;
import com.project.backend.pojo.vo.UserLoginVO;
import com.project.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口", description = "用户登录、注册、资料修改相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT 令牌")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录: {}", userLoginDTO.getUsername());
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "用户注册(教师/学生)")
    public Result<Void> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("用户注册: {}", userRegisterDTO.getUsername());
        userService.register(userRegisterDTO);
        return Result.success();
    }

    /**
     * 修改教师信息
     */
    @PutMapping("/teacher/profile")
    @Operation(summary = "修改教师信息", description = "教师可修改姓名、工号、密码")
    public Result<Void> updateTeacherInfo(@RequestBody TeacherUpdateDTO updateDTO) {
        log.info("修改教师信息");
        userService.updateTeacherInfo(updateDTO);
        return Result.success();
    }

    /**
     * 修改学生信息
     */
    @PutMapping("/student/profile")
    @Operation(summary = "修改学生信息", description = "学生可修改姓名、密码、头像地址")
    public Result<Void> updateStudentInfo(@RequestBody StudentUpdateDTO updateDTO) {
        log.info("修改学生信息");
        userService.updateStudentInfo(updateDTO);
        return Result.success();
    }
}
