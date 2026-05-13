package com.example.library.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.BookAddRequest;
import com.example.library.dto.BookPageRequest;
import com.example.library.dto.BookUpdateRequest;
import com.example.library.entity.Book;
import com.example.library.vo.BookVO;

import java.util.List;

/**
 * 图书业务接口
 */
public interface BookService extends IService<Book> {

    /**
     * 新增图书
     *
     * @param request 请求参数
     */
    void addBook(BookAddRequest request);

    /**
     * 修改图书
     *
     * @param request 请求参数
     */
    void updateBook(BookUpdateRequest request);

    /**
     * 分页查询图书（管理端）
     *
     * @param request 查询参数
     * @return 图书分页数据
     */
    Page<BookVO> pageBooks(BookPageRequest request);

    /**
     * 分页查询可借阅图书（用户端，仅上架）
     *
     * @param request 查询参数
     * @return 图书分页数据
     */
    Page<BookVO> pageAvailableBooks(BookPageRequest request);

    /**
     * 查询图书详情
     *
     * @param id 图书ID
     * @return 图书详情
     */
    BookVO getBookDetail(Long id);

    /**
     * 修改图书上下架状态
     *
     * @param id     图书ID
     * @param status 状态：1 上架 0 下架
     */
    void changeBookStatus(Long id, Integer status);

    /**
     * 查询热门图书（按借阅次数倒序）
     *
     * @param limit 返回数量
     * @return 热门图书列表
     */
    List<BookVO> listHotBooks(int limit);
}

