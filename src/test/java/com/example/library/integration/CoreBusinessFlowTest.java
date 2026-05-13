package com.example.library.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Core business flow regression")
class CoreBusinessFlowTest extends AbstractLibraryIntegrationTest {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("Borrow lifecycle: apply, approve, lend, renew, return with stock consistency")
    void borrowLifecycle_keepsStateAndStockConsistent() throws Exception {
        String userToken = login("user01", "123456");
        String librarianToken = login("librarian", "123456");
        Long bookId = 1L;
        Integer beforeStock = availableStock(bookId);

        assertSuccess(postJson("/user/borrow/apply", userToken, "{\"bookId\":1,\"remark\":\"core-flow\"}"));
        Long orderId = latestBorrowOrderId(3L, bookId);
        assertBorrowStatus(orderId, "APPLYING");
        assertEquals(beforeStock, availableStock(bookId), "apply must not decrease stock");

        assertSuccess(postJson("/librarian/borrow/approve", librarianToken,
                "{\"orderId\":" + orderId + ",\"approve\":true,\"remark\":\"ok\"}"));
        assertBorrowStatus(orderId, "APPROVED");

        JsonNode duplicateApprove = postJson("/librarian/borrow/approve", librarianToken,
                "{\"orderId\":" + orderId + ",\"approve\":true,\"remark\":\"again\"}");
        assertFailure(duplicateApprove, "approved order must not be approved again");

        assertSuccess(lend(librarianToken, orderId));
        assertBorrowStatus(orderId, "LENT");
        assertEquals(beforeStock - 1, availableStock(bookId), "lend must decrease stock");

        assertSuccess(postJson("/user/borrow/renew", userToken, "{\"orderId\":" + orderId + "}"));
        Integer renewCount = jdbcTemplate.queryForObject(
                "SELECT renew_count FROM borrow_order WHERE id = ?", Integer.class, orderId);
        assertEquals(1, renewCount);

        assertSuccess(postJson("/librarian/borrow/return", librarianToken, "{\"orderId\":" + orderId + "}"));
        assertBorrowStatus(orderId, "RETURNED");
        assertEquals(beforeStock, availableStock(bookId), "return must restore stock");

        JsonNode duplicateReturn = postJson("/librarian/borrow/return", librarianToken, "{\"orderId\":" + orderId + "}");
        assertFailure(duplicateReturn, "returned order must not be returned again");
    }

    @Test
    @DisplayName("Borrow guards: invalid renew, return before lend, and lend without stock")
    void borrowInvalidStates_areRejected() throws Exception {
        String userToken = login("user01", "123456");
        String librarianToken = login("librarian", "123456");

        assertSuccess(postJson("/user/borrow/apply", userToken, "{\"bookId\":2,\"remark\":\"invalid-renew\"}"));
        Long applyingOrderId = latestBorrowOrderId(3L, 2L);
        assertFailure(postJson("/user/borrow/renew", userToken, "{\"orderId\":" + applyingOrderId + "}"),
                "applying order must not be renewed");
        assertFailure(postJson("/librarian/borrow/return", librarianToken, "{\"orderId\":" + applyingOrderId + "}"),
                "applying order must not be returned");

        assertSuccess(postJson("/librarian/borrow/approve", librarianToken,
                "{\"orderId\":" + applyingOrderId + ",\"approve\":true}"));
        assertFailure(postJson("/librarian/borrow/return", librarianToken, "{\"orderId\":" + applyingOrderId + "}"),
                "approved but not lent order must not be returned");

        assertSuccess(postJson("/user/borrow/apply", userToken, "{\"bookId\":3,\"remark\":\"stock\"}"));
        Long stockOrderId = latestBorrowOrderId(3L, 3L);
        assertSuccess(postJson("/librarian/borrow/approve", librarianToken,
                "{\"orderId\":" + stockOrderId + ",\"approve\":true}"));
        jdbcTemplate.update("UPDATE book SET available_stock = 0 WHERE id = 3");
        assertFailure(lend(librarianToken, stockOrderId), "approved order must not be lent when stock is zero");
        assertBorrowStatus(stockOrderId, "APPROVED");
    }

    @Test
    @DisplayName("Reservation lifecycle: create, sign in, finish, and reject invalid state operations")
    void reservationLifecycle_enforcesStateTransitions() throws Exception {
        String userToken = login("user01", "123456");

        LocalDateTime firstStart = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        Long canceledId = createReservation(userToken, 1L, firstStart, firstStart.plusHours(2));
        assertSuccess(postJson("/user/reservation/cancel", userToken, "{\"reservationId\":" + canceledId + "}"));
        assertReservationStatus(canceledId, "CANCELED");
        assertFailure(postJson("/user/reservation/sign-in", userToken, "{\"reservationId\":" + canceledId + "}"),
                "canceled reservation must not be checked in");

        LocalDateTime secondStart = firstStart.plusDays(1);
        Long activeId = createReservation(userToken, 1L, secondStart, secondStart.plusHours(2));
        assertReservationStatus(activeId, "PENDING_CHECK_IN");

        assertSuccess(postJson("/user/reservation/sign-in", userToken, "{\"reservationId\":" + activeId + "}"));
        assertReservationStatus(activeId, "IN_USE");

        assertSuccess(postJson("/user/reservation/finish", userToken, "{\"reservationId\":" + activeId + "}"));
        assertReservationStatus(activeId, "FINISHED");

        assertFailure(postJson("/user/reservation/finish", userToken, "{\"reservationId\":" + activeId + "}"),
                "finished reservation must not be finished again");
    }

    @Test
    @DisplayName("Reservation conflicts: same seat or same user cannot overlap")
    void reservationConflict_sameSeatOrSameUserIsRejected() throws Exception {
        String userToken = login("user01", "123456");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(13).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        createReservation(userToken, 2L, start, end);
        assertFailure(createReservationResponse(userToken, 2L, start.plusMinutes(30), end.plusMinutes(30)),
                "same seat must not be reserved in overlapping time");
        assertFailure(createReservationResponse(userToken, 3L, start.plusMinutes(30), end.plusMinutes(30)),
                "same user must not reserve another seat in overlapping time");
    }

    @Test
    @DisplayName("Reservation concurrency: only one overlapping request wins for the same seat")
    void reservationConcurrency_sameSeatOnlyOneSucceeds() throws Exception {
        String userToken = login("user01", "123456");
        LocalDateTime start = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        List<JsonNode> results = runConcurrentCreates(6, i -> createReservationResponse(userToken, 4L, start, start.plusHours(2)));

        long successCount = results.stream().filter(CoreBusinessFlowTest::isSuccess).count();
        Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE seat_id = 4 AND status = 'PENDING_CHECK_IN'",
                Long.class);
        assertEquals(1L, successCount, "same seat concurrent create should have exactly one success");
        assertEquals(1L, rowCount);
    }

    @Test
    @DisplayName("Reservation concurrency: only one overlapping request wins for the same user")
    void reservationConcurrency_sameUserOnlyOneSucceeds() throws Exception {
        String userToken = login("user01", "123456");
        LocalDateTime start = LocalDateTime.now().plusDays(4).withHour(15).withMinute(0).withSecond(0).withNano(0);
        List<JsonNode> results = runConcurrentCreates(6,
                i -> createReservationResponse(userToken, 5L + i, start, start.plusHours(2)));

        long successCount = results.stream().filter(CoreBusinessFlowTest::isSuccess).count();
        Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation WHERE user_id = 3 AND status = 'PENDING_CHECK_IN'",
                Long.class);
        assertEquals(1L, successCount, "same user concurrent create should have exactly one success");
        assertEquals(1L, rowCount);
    }

    @Test
    @DisplayName("Auth boundary: login, me, logout blacklist, tokenVersion, and disabled user")
    void authBoundary_invalidatesOldTokens() throws Exception {
        String token = login("user01", "123456");
        ResponseEntity<String> me = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertEquals(HttpStatus.OK, me.getStatusCode());

        restTemplate.postForEntity(base() + "/auth/logout", new HttpEntity<>(bearer(token)), String.class);
        ResponseEntity<String> afterLogout = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(token)), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, afterLogout.getStatusCode());

        String tokenBeforeVersionChange = login("user01", "123456");
        jdbcTemplate.update("UPDATE user SET token_version = token_version + 1 WHERE id = 3");
        ResponseEntity<String> afterVersionChange = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(tokenBeforeVersionChange)), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, afterVersionChange.getStatusCode());

        jdbcTemplate.update("UPDATE user SET token_version = 0, status = 1 WHERE id = 3");
        String tokenBeforeDisable = login("user01", "123456");
        jdbcTemplate.update("UPDATE user SET status = 0 WHERE id = 3");
        ResponseEntity<String> afterDisable = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(tokenBeforeDisable)), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, afterDisable.getStatusCode());
    }

    private JsonNode lend(String token, Long orderId) throws Exception {
        ResponseEntity<String> response = restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderId,
                new HttpEntity<>(bearer(token)), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private Long createReservation(String token, Long seatId, LocalDateTime start, LocalDateTime end) throws Exception {
        assertSuccess(createReservationResponse(token, seatId, start, end));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE user_id = 3 AND seat_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                seatId);
    }

    private JsonNode createReservationResponse(String token, Long seatId, LocalDateTime start, LocalDateTime end) throws Exception {
        String json = String.format("{\"seatId\":%d,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                seatId,
                DATE_TIME_FORMATTER.format(start),
                DATE_TIME_FORMATTER.format(end));
        return postJson("/user/reservation/create", token, json);
    }

    private List<JsonNode> runConcurrentCreates(int threads, ThrowingCreateAction action) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<JsonNode>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            Callable<JsonNode> task = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return action.create(index);
            };
            futures.add(executor.submit(task));
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS), "workers should be ready");
        start.countDown();

        List<JsonNode> results = new ArrayList<>();
        for (Future<JsonNode> future : futures) {
            results.add(future.get(10, TimeUnit.SECONDS));
        }
        executor.shutdownNow();
        return results;
    }

    private Integer availableStock(Long bookId) {
        return jdbcTemplate.queryForObject("SELECT available_stock FROM book WHERE id = ?", Integer.class, bookId);
    }

    private Long latestBorrowOrderId(Long userId, Long bookId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM borrow_order WHERE user_id = ? AND book_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                userId,
                bookId);
    }

    private void assertBorrowStatus(Long orderId, String expectedStatus) {
        String status = jdbcTemplate.queryForObject("SELECT status FROM borrow_order WHERE id = ?", String.class, orderId);
        assertEquals(expectedStatus, status);
    }

    private void assertReservationStatus(Long reservationId, String expectedStatus) {
        String status = jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, reservationId);
        assertEquals(expectedStatus, status);
    }

    private static void assertSuccess(JsonNode response) {
        assertEquals(0, response.get("code").asInt(), response.toString());
    }

    private static void assertFailure(JsonNode response, String message) {
        assertTrue(response.get("code").asInt() != 0, message + ": " + response);
    }

    private static boolean isSuccess(JsonNode response) {
        return response.get("code").asInt() == 0;
    }

    @FunctionalInterface
    private interface ThrowingCreateAction {
        JsonNode create(int index) throws Exception;
    }
}
