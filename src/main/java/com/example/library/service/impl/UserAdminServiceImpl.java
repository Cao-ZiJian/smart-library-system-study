package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.context.UserContext;
import com.example.library.dto.AdminResetPasswordRequest;
import com.example.library.dto.AdminUserPageRequest;
import com.example.library.dto.AdminUserUpdateRoleRequest;
import com.example.library.dto.AdminUserUpdateStatusRequest;
import com.example.library.entity.User;
import com.example.library.enums.UserRole;
import com.example.library.exception.BusinessException;
import com.example.library.mapper.UserMapper;
import com.example.library.service.UserAdminService;
import com.example.library.vo.AdminUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务实现。
 */
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl extends ServiceImpl<UserMapper, User> implements UserAdminService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<AdminUserVO> pageUsers(AdminUserPageRequest request) {
        UserRole role = parseOptionalRole(request.getRole());
        Page<User> page = new Page<>(request.getPageNo(), request.getPageSize());

        Page<User> userPage = lambdaQuery()
                .and(StringUtils.hasText(request.getKeyword()), wrapper -> wrapper
                        .like(User::getUsername, request.getKeyword())
                        .or()
                        .like(User::getNickname, request.getKeyword()))
                .eq(role != null, User::getRole, role == null ? null : role.code())
                .eq(request.getStatus() != null, User::getStatus, request.getStatus())
                .orderByDesc(User::getCreateTime)
                .page(page);

        return buildUserVOPage(userPage);
    }

    @Override
    public AdminUserVO getUserDetail(Long id) {
        User user = getExistingUser(id);
        return toVO(user);
    }

    @Override
    public void updateStatus(Long id, AdminUserUpdateStatusRequest request) {
        User user = getExistingUser(id);
        rejectSelfOperation(user.getId(), "不能修改自己的状态");

        boolean updated = lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getStatus, request.getStatus())
                .setSql(request.getStatus() == 0, "token_version = token_version + 1")
                .update();
        if (!updated) {
            throw new BusinessException("更新用户状态失败");
        }
    }

    @Override
    public void updateRole(Long id, AdminUserUpdateRoleRequest request) {
        User user = getExistingUser(id);
        rejectSelfOperation(user.getId(), "不能修改自己的角色");

        UserRole newRole = parseRequiredRole(request.getRole());
        if (UserRole.ADMIN.code().equals(user.getRole())
                && newRole != UserRole.ADMIN
                && countAdminUsers() <= 1) {
            throw new BusinessException("不能降级最后一个管理员");
        }

        boolean updated = lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getRole, newRole.code())
                .setSql("token_version = token_version + 1")
                .update();
        if (!updated) {
            throw new BusinessException("更新用户角色失败");
        }
    }

    @Override
    public void kickOut(Long id) {
        User user = getExistingUser(id);
        rejectSelfOperation(user.getId(), "不能踢自己下线");

        boolean updated = lambdaUpdate()
                .eq(User::getId, user.getId())
                .setSql("token_version = token_version + 1")
                .update();
        if (!updated) {
            throw new BusinessException("踢用户下线失败");
        }
    }

    @Override
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        User user = getExistingUser(id);
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());

        boolean updated = lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getPassword, encodedPassword)
                .setSql("token_version = token_version + 1")
                .update();
        if (!updated) {
            throw new BusinessException("重置用户密码失败");
        }
    }

    @Override
    public long countAdminUsers() {
        return lambdaQuery()
                .eq(User::getRole, UserRole.ADMIN.code())
                .count();
    }

    private User getExistingUser(Long id) {
        if (id == null) {
            throw new BusinessException("用户ID不能为空");
        }
        User user = getById(id);
        if (user == null) {
            throw new BusinessException(BusinessException.NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void rejectSelfOperation(Long targetUserId, String message) {
        Long currentUserId = UserContext.getRequiredUserId();
        if (targetUserId != null && targetUserId.equals(currentUserId)) {
            throw new BusinessException(message);
        }
    }

    private UserRole parseRequiredRole(String role) {
        UserRole userRole = UserRole.fromCode(role);
        if (userRole == null) {
            throw new BusinessException("用户角色不合法");
        }
        return userRole;
    }

    private UserRole parseOptionalRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        return parseRequiredRole(role);
    }

    private Page<AdminUserVO> buildUserVOPage(Page<User> userPage) {
        List<User> records = userPage.getRecords();
        Page<AdminUserVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (records == null || records.isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        voPage.setRecords(records.stream()
                .map(this::toVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    private AdminUserVO toVO(User user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
