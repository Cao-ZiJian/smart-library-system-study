package com.example.library.controller;

import com.example.library.context.UserContext;
import com.example.library.dto.UserAvatarUpdateRequest;
import com.example.library.dto.UserLoginRequest;
import com.example.library.dto.UserRegisterRequest;
import com.example.library.result.Result;
import com.example.library.service.UserService;
import com.example.library.session.SessionManager;
import com.example.library.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 登录认证与当前用户接口
 */
@Api(tags = "核心-登录认证")
@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SessionManager sessionManager;

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 用户注册
     */
    @ApiOperation("注册普通用户：密码使用 BCrypt 加密保存")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     * 登录成功返回 JWT accessToken，前端使用 Authorization: Bearer {token}
     */
    @ApiOperation("登录并创建 Redis Session：返回 JWT accessToken")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginRequest request) {
        String token = userService.login(request);
        return Result.success(token);
    }

    /**
     * 退出登录：清除 Redis 会话（需携带当前 Bearer token）
     */
    @ApiOperation("退出登录：删除 Redis Session 并将当前 jti 加入黑名单")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith(BEARER_PREFIX)) {
            String accessToken = auth.substring(BEARER_PREFIX.length()).trim();
            sessionManager.removeSession(accessToken);
        }
        return Result.success();
    }

    /**
     * 获取当前登录用户信息
     */
    @ApiOperation("获取当前登录用户：基于认证拦截器写入的 UserContext")
    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        Long currentUserId = UserContext.getRequiredUserId();
        UserVO userVO = userService.getCurrentUser(currentUserId);
        return Result.success(userVO);
    }

    /**
     * 更新头像：请先调用 /common/upload?folder=avatar 获取 url
     */
    @ApiOperation("维护当前用户头像地址：头像图片可先通过基础上传接口获取 URL")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@Valid @RequestBody UserAvatarUpdateRequest request) {
        Long currentUserId = UserContext.getRequiredUserId();
        userService.updateAvatar(currentUserId, request.getAvatarUrl());
        return Result.success();
    }
}























