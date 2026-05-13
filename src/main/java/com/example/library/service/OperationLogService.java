package com.example.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.OperationLogPageRequest;
import com.example.library.entity.OperationLog;
import com.example.library.vo.OperationLogVO;

/**
 * 操作日志业务接口
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 分页查询操作日志
     */
    Page<OperationLogVO> pageLogs(OperationLogPageRequest request);
}
