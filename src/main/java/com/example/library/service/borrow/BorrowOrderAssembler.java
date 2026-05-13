package com.example.library.service.borrow;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowOrder;
import com.example.library.entity.User;
import com.example.library.service.BookService;
import com.example.library.service.UserService;
import com.example.library.vo.BorrowOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 借阅记录 VO 组装
 */
@Component
@RequiredArgsConstructor
public class BorrowOrderAssembler {

    private final UserService userService;
    private final BookService bookService;

    public Page<BorrowOrderVO> buildOrderVOPage(Page<BorrowOrder> orderPage) {
        List<BorrowOrder> records = orderPage.getRecords();
        if (records == null || records.isEmpty()) {
            Page<BorrowOrderVO> emptyPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        Set<Long> userIds = records.stream()
                .map(BorrowOrder::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Set<Long> bookIds = records.stream()
                .map(BorrowOrder::getBookId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        final Map<Long, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (left, right) -> left));

        final Map<Long, Book> bookMap = bookIds.isEmpty()
                ? Collections.emptyMap()
                : bookService.listByIds(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b, (left, right) -> left));

        List<BorrowOrderVO> voRecords = records.stream()
                .map(order -> toVO(order, userMap.get(order.getUserId()), bookMap.get(order.getBookId())))
                .collect(Collectors.toList());

        Page<BorrowOrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(voRecords);
        return voPage;
    }

    private BorrowOrderVO toVO(BorrowOrder order, User user, Book book) {
        BorrowOrderVO vo = new BorrowOrderVO();
        BeanUtils.copyProperties(order, vo);
        if (user != null) {
            vo.setUsername(user.getUsername());
        }
        if (book != null) {
            vo.setTitle(book.getTitle());
        }
        return vo;
    }
}
