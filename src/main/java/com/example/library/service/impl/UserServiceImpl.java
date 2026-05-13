package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.UserLoginRequest;
import com.example.library.dto.UserRegisterRequest;
import com.example.library.entity.User;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.service.UserService;
import com.example.library.session.SessionManager;
import com.example.library.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户相关业务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final SessionManager sessionManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(UserRegisterRequest request) {
        // 用户名唯一性校验
        long count = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .count();
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);

        try {
            if (!save(user)) {
                throw new BusinessException("注册失败，请稍后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("用户名已存在");
        }
    }

    @Override
    public String login(UserLoginRequest request) {
        User user = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .one();
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 登录成功后统一交给 SessionManager 创建 JWT + Redis Session
        return sessionManager.createSession(user);
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setAvatarUrl(avatarUrl);
        if (!updateById(user)) {
            throw new BusinessException("更新头像失败");
        }
    }
}
