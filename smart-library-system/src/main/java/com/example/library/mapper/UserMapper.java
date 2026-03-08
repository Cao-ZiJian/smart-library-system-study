package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.User;
import org.springframework.stereotype.Repository;

/**
 * 用户 Mapper
 */
@Repository
public interface UserMapper extends BaseMapper<User> {
}

