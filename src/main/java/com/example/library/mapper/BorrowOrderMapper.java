package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.library.entity.BorrowOrder;
import org.springframework.stereotype.Repository;

/**
 * 借阅订单 Mapper
 */
@Repository
public interface BorrowOrderMapper extends BaseMapper<BorrowOrder> {
}

