package com.example.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.dto.AdminResetPasswordRequest;
import com.example.library.dto.AdminUserPageRequest;
import com.example.library.dto.AdminUserUpdateRoleRequest;
import com.example.library.dto.AdminUserUpdateStatusRequest;
import com.example.library.vo.AdminUserVO;

/**
 * 管理端用户管理服务。
 */
public interface UserAdminService {

    Page<AdminUserVO> pageUsers(AdminUserPageRequest request);

    AdminUserVO getUserDetail(Long id);

    void updateStatus(Long id, AdminUserUpdateStatusRequest request);

    void updateRole(Long id, AdminUserUpdateRoleRequest request);

    void kickOut(Long id);

    void resetPassword(Long id, AdminResetPasswordRequest request);

    long countAdminUsers();
}
