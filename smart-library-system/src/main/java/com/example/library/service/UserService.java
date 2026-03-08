package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.UserLoginRequest;
import com.example.library.dto.UserRegisterRequest;
import com.example.library.entity.User;
import com.example.library.vo.UserVO;

/**
 * 用户相关业务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param request 注册参数
     */
    void register(UserRegisterRequest request);

    /**
     * 用户登录
     *
     * @param request 登录参数
     * @return JWT token
     */
    String login(UserLoginRequest request);

    /**
     * 查询当前登录用户信息
     *
     * @param userId 用户ID
     * @return 用户展示对象
     */
    UserVO getCurrentUser(Long userId);
}

