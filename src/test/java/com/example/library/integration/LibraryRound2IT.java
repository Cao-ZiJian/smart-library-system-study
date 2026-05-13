package com.example.library.integration;

import com.example.library.constant.RedisKeyConstants;
import com.example.library.task.BorrowScheduleTask;
import com.example.library.task.ReservationScheduleTask;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 第二轮：头像、分类、图书、自习室/座位、借阅/预约分支、Dashboard、定时任务幂等等。
 */
@DisplayName("智慧图书馆全栈接口回归（第二轮）")
class LibraryRound2IT extends AbstractLibraryIntegrationTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private BorrowScheduleTask borrowScheduleTask;

    @Autowired
    private ReservationScheduleTask reservationScheduleTask;

    private static final DateTimeFormatter RES_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("头像：未登录 PUT /auth/avatar → 401；登录后可更新任意非空 URL（仅长度校验，不校验协议/域名）")
    void avatarUpdate_authAndValidation() throws Exception {
        ResponseEntity<String> noAuth = putRaw("/auth/avatar", null, "{\"avatarUrl\":\"https://cdn.example.com/u/1.png\"}");
        assertEquals(HttpStatus.UNAUTHORIZED, noAuth.getStatusCode());

        String t = login("user01", "123456");
        assertEquals(0, putJson("/auth/avatar", t, "{\"avatarUrl\":\"https://cdn.example.com/avatar/3/x.png\"}").get("code").asInt());
        JsonNode me = getJson("/auth/me", t);
        assertEquals("https://cdn.example.com/avatar/3/x.png", me.get("data").get("avatarUrl").asText());

        ResponseEntity<String> bad400 = putRaw("/auth/avatar", t, "{\"avatarUrl\":\"\"}");
        assertEquals(HttpStatus.BAD_REQUEST, bad400.getStatusCode());

        // 当前设计：不校验 URL 形态，仅 @NotBlank + @Size(max=2048)
        assertEquals(0, putJson("/auth/avatar", t, "{\"avatarUrl\":\"javascript:alert(1)\"}").get("code").asInt());
    }

    @Test
    @DisplayName("图书分类：增删改查、重名、被引用不可删")
    void bookCategoryCrud() throws Exception {
        String lib = login("librarian", "123456");
        assertEquals(0, postJson("/admin/book/category/add", lib,
                "{\"name\":\"IT分类A\",\"sort\":99,\"status\":1}").get("code").asInt());
        JsonNode dup = postJson("/admin/book/category/add", lib, "{\"name\":\"IT分类A\",\"sort\":1,\"status\":1}");
        assertTrue(dup.get("code").asInt() != 0, "重名新增应失败");

        JsonNode list = getJson("/admin/book/category/list", lib);
        assertEquals(0, list.get("code").asInt());
        assertTrue(list.get("data").toString().contains("IT分类A"));

        long newId = jdbcTemplate.queryForObject(
                "SELECT id FROM book_category WHERE name = 'IT分类A' LIMIT 1", Long.class);
        assertEquals(0, postJson("/admin/book/category/update", lib,
                "{\"id\":" + newId + ",\"name\":\"IT分类A2\",\"sort\":98,\"status\":1}").get("code").asInt());

        JsonNode dupUp = postJson("/admin/book/category/update", lib,
                "{\"id\":" + newId + ",\"name\":\"计算机\",\"sort\":1,\"status\":1}");
        assertTrue(dupUp.get("code").asInt() != 0, "改名为已存在分类应失败");

        JsonNode delRef = deleteJson("/admin/book/category/1", lib);
        assertTrue(delRef.get("code").asInt() != 0, "有图书引用时不可删");

        assertEquals(0, deleteJson("/admin/book/category/" + newId, lib).get("code").asInt());
    }

    @Test
    @DisplayName("图书：新增/修改/分页/详情/上下架、coverUrl、详情缓存失效")
    void bookAdminAndUserDetailCache() throws Exception {
        String lib = login("librarian", "123456");
        String u = login("user01", "123456");
        String addBody = "{\"categoryId\":1,\"title\":\"IT封面书\",\"author\":\"T\",\"isbn\":\"\",\"publisher\":\"\",\"publishYear\":2024,"
                + "\"coverUrl\":\"https://cdn.example.com/cover/3/it-book.jpg\",\"description\":\"d\",\"totalStock\":4,\"status\":1}";
        assertEquals(0, postJson("/admin/book/add", lib, addBody).get("code").asInt());

        long bookId = jdbcTemplate.queryForObject("SELECT id FROM book WHERE title='IT封面书' LIMIT 1", Long.class);

        JsonNode page = getJson("/admin/book/page?pageNum=1&pageSize=5&keyword=IT封面", lib);
        assertEquals(0, page.get("code").asInt());
        assertTrue(page.get("data").get("total").asLong() >= 1);

        JsonNode admDetail = getJson("/admin/book/" + bookId, lib);
        assertEquals(0, admDetail.get("code").asInt());
        assertEquals("https://cdn.example.com/cover/3/it-book.jpg", admDetail.get("data").get("coverUrl").asText());

        JsonNode userDetail1 = getJson("/book/" + bookId, u);
        assertEquals(0, userDetail1.get("code").asInt());
        assertEquals("IT封面书", userDetail1.get("data").get("title").asText());
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.bookDetail(bookId))));

        String upd = "{\"id\":" + bookId + ",\"categoryId\":1,\"title\":\"IT封面书改名\",\"author\":\"T\",\"isbn\":\"\",\"publisher\":\"\","
                + "\"publishYear\":2024,\"coverUrl\":\"https://cdn.example.com/cover/3/it2.jpg\",\"description\":\"d\",\"totalStock\":4,\"status\":1}";
        assertEquals(0, postJson("/admin/book/update", lib, upd).get("code").asInt());

        JsonNode userDetail2 = getJson("/book/" + bookId, u);
        assertEquals("IT封面书改名", userDetail2.get("data").get("title").asText());
        assertEquals("https://cdn.example.com/cover/3/it2.jpg", userDetail2.get("data").get("coverUrl").asText());

        ResponseEntity<String> st = restTemplate.postForEntity(base() + "/admin/book/" + bookId + "/status?status=0",
                new HttpEntity<>(bearer(lib)), String.class);
        assertEquals(HttpStatus.OK, st.getStatusCode());
        assertEquals(0, objectMapper.readTree(st.getBody()).get("code").asInt());
    }

    @Test
    @DisplayName("自习室：增改删列表、imageUrl；普通用户不可管理端")
    void studyRoomCrudAndPermission() throws Exception {
        String lib = login("librarian", "123456");
        String user = login("user01", "123456");

        JsonNode forbidden = postJson("/admin/study-room/add", user, "{\"name\":\"X\",\"capacity\":1}");
        assertEquals(403, forbidden.get("code").asInt());

        String add = "{\"name\":\"IT空自习室\",\"location\":\"L1\",\"capacity\":8,\"openTime\":\"08:00-20:00\","
                + "\"imageUrl\":\"https://img.example.com/room/it.png\",\"status\":1}";
        assertEquals(0, postJson("/admin/study-room/add", lib, add).get("code").asInt());
        long rid = jdbcTemplate.queryForObject("SELECT id FROM study_room WHERE name='IT空自习室' LIMIT 1", Long.class);

        JsonNode list = getJson("/admin/study-room/list", lib);
        assertEquals(0, list.get("code").asInt());
        assertTrue(list.get("data").toString().contains("https://img.example.com/room/it.png"));

        String upd = "{\"id\":" + rid + ",\"name\":\"IT空自习室改\",\"location\":\"L2\",\"capacity\":9,\"openTime\":\"09:00-21:00\","
                + "\"imageUrl\":\"https://img.example.com/room/it2.png\",\"status\":1}";
        assertEquals(0, postJson("/admin/study-room/update", lib, upd).get("code").asInt());

        assertEquals(0, deleteJson("/admin/study-room/" + rid, lib).get("code").asInt());

        JsonNode userList = getJson("/study-room/list", user);
        assertEquals(0, userList.get("code").asInt());
    }

    @Test
    @DisplayName("座位：增改删列表、同室座位号重复；用户不可管理端")
    void seatCrudAndDuplicate() throws Exception {
        String lib = login("librarian", "123456");
        String user = login("user01", "123456");

        JsonNode forb = postJson("/admin/seat/add", user, "{\"studyRoomId\":1,\"seatNumber\":\"Z-99\"}");
        assertEquals(403, forb.get("code").asInt());

        assertEquals(0, postJson("/admin/seat/add", lib, "{\"studyRoomId\":1,\"seatNumber\":\"Z-IT-01\",\"status\":1}").get("code").asInt());
        long sid = jdbcTemplate.queryForObject("SELECT id FROM seat WHERE seat_number='Z-IT-01' LIMIT 1", Long.class);

        JsonNode dup = postJson("/admin/seat/add", lib, "{\"studyRoomId\":1,\"seatNumber\":\"Z-IT-01\",\"status\":1}");
        assertTrue(dup.get("code").asInt() != 0);

        JsonNode seats = getJson("/admin/seat/list-by-room?studyRoomId=1", lib);
        assertEquals(0, seats.get("code").asInt());
        assertTrue(seats.get("data").toString().contains("Z-IT-01"));

        assertEquals(0, postJson("/admin/seat/update", lib,
                "{\"id\":" + sid + ",\"studyRoomId\":1,\"seatNumber\":\"Z-IT-02\",\"status\":1}").get("code").asInt());
        assertEquals(0, deleteJson("/admin/seat/" + sid, lib).get("code").asInt());
    }

    @Test
    @DisplayName("借阅：续借成功/非法状态失败、库存不足、馆员分页、重复 lend/return")
    void borrowBranches() throws Exception {
        String u = login("user01", "123456");
        String l = login("librarian", "123456");

        assertEquals(0, postJson("/user/borrow/apply", u, "{\"bookId\":2,\"remark\":\"r\"}").get("code").asInt());
        long orderApplying = jdbcTemplate.queryForObject(
                "SELECT id FROM borrow_order WHERE user_id=3 AND book_id=2 ORDER BY id DESC LIMIT 1", Long.class);
        JsonNode renewBad = postJson("/user/borrow/renew", u, "{\"orderId\":" + orderApplying + "}");
        assertTrue(renewBad.get("code").asInt() != 0, "非在借状态不可续借");

        assertEquals(0, postJson("/librarian/borrow/approve", l,
                "{\"orderId\":" + orderApplying + ",\"approve\":true}").get("code").asInt());
        assertEquals(0, getJson("/librarian/borrow/page?pageNum=1&pageSize=10", l).get("code").asInt());

        jdbcTemplate.update("UPDATE book SET available_stock = 0 WHERE id = 2");
        JsonNode lendFail = objectMapper.readTree(restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderApplying,
                new HttpEntity<>(bearer(l)), String.class).getBody());
        assertTrue(lendFail.get("code").asInt() != 0, "库存为 0 不可出借");
        jdbcTemplate.update("UPDATE book SET available_stock = total_stock WHERE id = 2");

        assertEquals(0, objectMapper.readTree(restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderApplying,
                new HttpEntity<>(bearer(l)), String.class).getBody()).get("code").asInt());

        assertEquals(0, postJson("/user/borrow/renew", u, "{\"orderId\":" + orderApplying + "}").get("code").asInt());
        JsonNode renewTwice = postJson("/user/borrow/renew", u, "{\"orderId\":" + orderApplying + "}");
        assertTrue(renewTwice.get("code").asInt() != 0, "超过续借次数");

        JsonNode lendDup = objectMapper.readTree(restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderApplying,
                new HttpEntity<>(bearer(l)), String.class).getBody());
        assertTrue(lendDup.get("code").asInt() != 0, "已出借不可再次 lend");

        assertEquals(0, postJson("/librarian/borrow/return", l, "{\"orderId\":" + orderApplying + "}").get("code").asInt());
        JsonNode retDup = postJson("/librarian/borrow/return", l, "{\"orderId\":" + orderApplying + "}");
        assertTrue(retDup.get("code").asInt() != 0);

        jdbcTemplate.update("UPDATE book SET available_stock = 0 WHERE id = 4");
        JsonNode applyNoStock = postJson("/user/borrow/apply", u, "{\"bookId\":4,\"remark\":\"x\"}");
        assertTrue(applyNoStock.get("code").asInt() != 0, "库存不足不可申请");
    }

    @Test
    @DisplayName("预约：取消、签到、完成、非法状态、分页、创建后锁已释放")
    void reservationLifecycleAndLocks() throws Exception {
        String u = login("user01", "123456");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        String json = String.format("{\"seatId\":2,\"startTime\":\"%s\",\"endTime\":\"%s\"}", RES_FMT.format(start), RES_FMT.format(end));
        assertEquals(0, postJson("/user/reservation/create", u, json).get("code").asInt());

        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.lockReservationSeat(2L))));
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeyConstants.lockReservationUser(3L))));

        assertEquals(0, getJson("/user/reservation/page?pageNum=1&pageSize=5", u).get("code").asInt());
        long resId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE user_id=3 AND seat_id=2 AND status='PENDING_CHECK_IN' ORDER BY id DESC LIMIT 1",
                Long.class);

        assertEquals(0, postJson("/user/reservation/cancel", u, "{\"reservationId\":" + resId + "}").get("code").asInt());
        JsonNode cancelTwice = postJson("/user/reservation/cancel", u, "{\"reservationId\":" + resId + "}");
        assertTrue(cancelTwice.get("code").asInt() != 0);

        assertEquals(0, postJson("/user/reservation/create", u, json).get("code").asInt());
        resId = jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE user_id=3 AND seat_id=2 AND status='PENDING_CHECK_IN' ORDER BY id DESC LIMIT 1",
                Long.class);

        assertEquals(0, postJson("/user/reservation/sign-in", u, "{\"reservationId\":" + resId + "}").get("code").asInt());
        JsonNode signBad = postJson("/user/reservation/sign-in", u, "{\"reservationId\":" + resId + "}");
        assertTrue(signBad.get("code").asInt() != 0);

        assertEquals(0, postJson("/user/reservation/finish", u, "{\"reservationId\":" + resId + "}").get("code").asInt());
        JsonNode finishBad = postJson("/user/reservation/finish", u, "{\"reservationId\":" + resId + "}");
        assertTrue(finishBad.get("code").asInt() != 0);

        JsonNode cancelInUse = postJson("/user/reservation/cancel", u, "{\"reservationId\":" + resId + "}");
        assertTrue(cancelInUse.get("code").asInt() != 0);
    }

    @Test
    @DisplayName("Dashboard：overview 全字段与热门/利用率结构；清空预约与借阅后仍无 NPE")
    void dashboardOverviewFields() throws Exception {
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM borrow_order");

        String lib = login("librarian", "123456");
        JsonNode root = getJson("/admin/dashboard/overview", lib);
        assertEquals(0, root.get("code").asInt());
        JsonNode d = root.get("data");
        assertNotNull(d.get("bookTotal"));
        assertNotNull(d.get("bookOnShelfCount"));
        assertNotNull(d.get("bookBorrowableCount"));
        assertNotNull(d.get("userTotal"));
        assertNotNull(d.get("todayBorrowApplyCount"));
        assertNotNull(d.get("todayLendCount"));
        assertNotNull(d.get("todayReturnCount"));
        assertNotNull(d.get("todayReservationCount"));
        assertNotNull(d.get("currentOverdueCount"));
        assertNotNull(d.get("seatInUseCount"));
        assertTrue(d.get("hotBooksTop5").isArray());
        assertTrue(d.get("studyRoomUtilizationTop5").isArray());

        if (d.get("hotBooksTop5").size() > 0) {
            JsonNode h = d.get("hotBooksTop5").get(0);
            assertTrue(h.hasNonNull("bookId") && h.hasNonNull("title"));
            assertTrue(h.has("borrowCount"));
        }
        if (d.get("studyRoomUtilizationTop5").size() > 0) {
            JsonNode s = d.get("studyRoomUtilizationTop5").get(0);
            assertTrue(s.hasNonNull("studyRoomId") && s.hasNonNull("studyRoomName"));
            assertTrue(s.has("capacity") && s.has("occupiedSeats") && s.has("utilizationRate"));
        }
    }

    @Test
    @DisplayName("定时任务：借阅逾期、预约过期、使用中自动结束 — 各执行两次幂等")
    void scheduledTasksIdempotent() {
        jdbcTemplate.update("DELETE FROM borrow_order");
        jdbcTemplate.update("DELETE FROM reservation");

        jdbcTemplate.update(
                "INSERT INTO borrow_order (user_id, book_id, status, apply_time, lend_time, due_time, renew_count) "
                        + "VALUES (3, 1, 'LENT', NOW(), NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), 0)");

        borrowScheduleTask.markOverdueBorrowOrders();
        borrowScheduleTask.markOverdueBorrowOrders();
        String st = jdbcTemplate.queryForObject(
                "SELECT status FROM borrow_order WHERE user_id=3 AND book_id=1 ORDER BY id DESC LIMIT 1", String.class);
        assertEquals("OVERDUE", st);

        jdbcTemplate.update(
                "INSERT INTO reservation (user_id, seat_id, start_time, end_time, status) VALUES "
                        + "(3, 3, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'PENDING_CHECK_IN')");
        reservationScheduleTask.expireNotCheckedInReservations();
        reservationScheduleTask.expireNotCheckedInReservations();
        String rs = jdbcTemplate.queryForObject(
                "SELECT status FROM reservation WHERE user_id=3 AND seat_id=3 ORDER BY id DESC LIMIT 1", String.class);
        assertEquals("EXPIRED", rs);

        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update(
                "INSERT INTO reservation (user_id, seat_id, start_time, end_time, status, check_in_time) VALUES "
                        + "(3, 4, DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 'IN_USE', NOW())");
        reservationScheduleTask.autoFinishExpiredInUse();
        reservationScheduleTask.autoFinishExpiredInUse();
        String rs2 = jdbcTemplate.queryForObject(
                "SELECT status FROM reservation WHERE user_id=3 AND seat_id=4 ORDER BY id DESC LIMIT 1", String.class);
        assertEquals("FINISHED", rs2);
    }
}
