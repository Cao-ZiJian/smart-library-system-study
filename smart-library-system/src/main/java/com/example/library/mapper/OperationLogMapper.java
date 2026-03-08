package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.OperationLog;
import org.springframework.stereotype.Repository;

/**
 * 操作日志 Mapper
 */
@Repository
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
