package com.example.library.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.dto.OperationLogPageRequest;
import com.example.library.entity.OperationLog;
import com.example.library.mapper.OperationLogMapper;
import com.example.library.service.OperationLogService;
import com.example.library.vo.OperationLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志业务实现
 */
@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public Page<OperationLogVO> pageLogs(OperationLogPageRequest request) {
        Page<OperationLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<OperationLog> logPage = lambdaQuery()
                .like(StringUtils.hasText(request.getUsername()), OperationLog::getUsername, request.getUsername())
                .like(StringUtils.hasText(request.getOperation()), OperationLog::getOperation, request.getOperation())
                .eq(StringUtils.hasText(request.getResult()), OperationLog::getResult, request.getResult())
                .orderByDesc(OperationLog::getCreateTime)
                .page(page);

        List<OperationLog> records = logPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<OperationLogVO> emptyPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        List<OperationLogVO> voList = records.stream()
                .map(log -> {
                    OperationLogVO vo = new OperationLogVO();
                    BeanUtils.copyProperties(log, vo);
                    return vo;
                })
                .collect(Collectors.toList());
        Page<OperationLogVO> voPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }
}
