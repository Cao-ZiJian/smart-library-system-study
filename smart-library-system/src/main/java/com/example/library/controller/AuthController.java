package com.example.library.controller;

import com.example.library.dto.UserLoginRequest;
import com.example.library.dto.UserRegisterRequest;
import com.example.library.result.Result;
import com.example.library.service.UserService;
import com.example.library.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证与用户相关接口
 */
@Api(tags = "认证与用户模块")
@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     * 登录成功返回 JWT token
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginRequest request) {
        String token = userService.login(request);
        return Result.success(token);
    }

    /**
     * 获取当前登录用户信息
     *
     * 登录拦截器会将 currentUserId 设置到请求属性中
     */
    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/me")
    public Result<UserVO> currentUser(@RequestAttribute("currentUserId") Long currentUserId) {
        UserVO userVO = userService.getCurrentUser(currentUserId);
        return Result.success(userVO);
    }
}

