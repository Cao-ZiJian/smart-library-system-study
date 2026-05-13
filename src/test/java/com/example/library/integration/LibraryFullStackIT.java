package com.example.library.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全栈接口回归（需本机 Docker：MySQL 8 + Redis 7）。
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("智慧图书馆全栈接口回归")
class LibraryFullStackIT extends AbstractLibraryIntegrationTest {

    @Test
    @Order(10)
    @DisplayName("登录成功返回 JWT accessToken")
    void loginSuccess_jwtAccessToken() throws Exception {
        String token = login("user01", "123456");
        assertTrue(token.length() > 100, "应返回 JWT accessToken");
        assertEquals(2, token.chars().filter(c -> c == '.').count(), "JWT 应为三段点分结构");
    }

    @Test
    @Order(11)
    @DisplayName("登录失败：用户不存在 / 密码错误 / 禁用")
    void loginFailures() throws Exception {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> r1 = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>("{\"username\":\"nouser\",\"password\":\"123456\"}", h), String.class);
        assertTrue(objectMapper.readTree(r1.getBody()).get("code").asInt() != 0);

        ResponseEntity<String> r2 = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>("{\"username\":\"user01\",\"password\":\"wrong\"}", h), String.class);
        assertTrue(objectMapper.readTree(r2.getBody()).get("code").asInt() != 0);

        jdbcTemplate.update("UPDATE user SET status = 0 WHERE id = 3");
        ResponseEntity<String> r3 = restTemplate.postForEntity(base() + "/auth/login",
                new HttpEntity<>("{\"username\":\"user01\",\"password\":\"123456\"}", h), String.class);
        assertTrue(objectMapper.readTree(r3.getBody()).get("code").asInt() != 0);
        jdbcTemplate.update("UPDATE user SET status = 1 WHERE id = 3");
    }

    @Test
    @Order(12)
    @DisplayName("未带 token / 无效 token → 401")
    void authMissingOrInvalid() {
        ResponseEntity<String> noAuth = restTemplate.getForEntity(base() + "/book/hot?limit=3", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, noAuth.getStatusCode());

        HttpHeaders bad = new HttpHeaders();
        bad.setBearerAuth("invalid-token-xyz");
        ResponseEntity<String> badTok = restTemplate.exchange(base() + "/book/hot?limit=3", HttpMethod.GET,
                new HttpEntity<>(bad), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, badTok.getStatusCode());
    }

    @Test
    @Order(13)
    @DisplayName("登出后 token 失效；token_version 变化后旧 token 失效")
    void logoutAndTokenVersion() throws Exception {
        String t = login("user01", "123456");
        ResponseEntity<String> ok = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(t)), String.class);
        assertEquals(HttpStatus.OK, ok.getStatusCode());

        HttpHeaders h = bearer(t);
        restTemplate.postForEntity(base() + "/auth/logout", new HttpEntity<>(h), String.class);

        ResponseEntity<String> after = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(h), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, after.getStatusCode());

        String t2 = login("user01", "123456");
        jdbcTemplate.update("UPDATE user SET token_version = token_version + 1 WHERE id = 3");
        ResponseEntity<String> ver = restTemplate.exchange(base() + "/auth/me", HttpMethod.GET,
                new HttpEntity<>(bearer(t2)), String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, ver.getStatusCode());
        jdbcTemplate.update("UPDATE user SET token_version = 0 WHERE id = 3");
    }

    @Test
    @Order(20)
    @DisplayName("权限：普通用户访问 admin → 403；馆员访问 dashboard → 200")
    void roleGuard() throws Exception {
        String userTok = login("user01", "123456");
        ResponseEntity<String> dashUser = getRaw("/admin/dashboard/overview", userTok);
        assertEquals(HttpStatus.FORBIDDEN, dashUser.getStatusCode());

        String libTok = login("librarian", "123456");
        ResponseEntity<String> dashLib = getRaw("/admin/dashboard/overview", libTok);
        assertEquals(HttpStatus.OK, dashLib.getStatusCode());
        JsonNode body = objectMapper.readTree(dashLib.getBody());
        assertEquals(0, body.get("code").asInt());
        assertNotNull(body.get("data").get("bookTotal"));
    }

    @Test
    @Order(30)
    @DisplayName("OSS 上传：集成测试占位密钥下调用失败并返回明确业务提示")
    void ossUpload_withTestCredentials_failsWithClearBusinessMessage() throws Exception {
        String t = login("user01", "123456");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource("x".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "a.jpg";
            }
        });
        body.add("folder", "cover");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(t);
        ResponseEntity<String> resp = restTemplate.postForEntity(base() + "/common/upload",
                new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(resp.getBody());
        assertTrue(root.get("code").asInt() != 0, root.toString());
        String msg = root.get("message").asText();
        assertTrue(msg.contains("阿里云") || msg.contains("OSS") || msg.contains("上传"),
                "期望业务错误信息提及 OSS/上传，实际: " + msg);
    }

    @Test
    @Order(40)
    @DisplayName("图书热门、分类列表（需登录）")
    void bookEndpoints() throws Exception {
        String t = login("user01", "123456");
        JsonNode hot = getJson("/book/hot?limit=2", t);
        assertEquals(0, hot.get("code").asInt());
        JsonNode cats = getJson("/book/categories", t);
        assertEquals(0, cats.get("code").asInt());
    }

    @Test
    @Order(50)
    @DisplayName("借阅全流程 + 重复出借拦截")
    void borrowFlow() throws Exception {
        String u = login("user01", "123456");
        String l = login("librarian", "123456");

        String apply = "{\"bookId\":1,\"remark\":\"it\"}";
        JsonNode a1 = postJson("/user/borrow/apply", u, apply);
        assertEquals(0, a1.get("code").asInt(), a1.toString());

        JsonNode dup = postJson("/user/borrow/apply", u, apply);
        assertTrue(dup.get("code").asInt() != 0, "重复申请应失败");

        JsonNode page = getJson("/user/borrow/page?pageNum=1&pageSize=5", u);
        long orderId = page.get("data").get("records").get(0).get("id").asLong();

        String approve = "{\"orderId\":" + orderId + ",\"approve\":true,\"remark\":\"ok\"}";
        assertEquals(0, postJson("/librarian/borrow/approve", l, approve).get("code").asInt());

        ResponseEntity<String> lend1 = restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderId,
                new HttpEntity<>(bearer(l)), String.class);
        assertEquals(HttpStatus.OK, lend1.getStatusCode());
        assertEquals(0, objectMapper.readTree(lend1.getBody()).get("code").asInt());

        ResponseEntity<String> lend2 = restTemplate.postForEntity(base() + "/librarian/borrow/lend/" + orderId,
                new HttpEntity<>(bearer(l)), String.class);
        JsonNode lend2b = objectMapper.readTree(lend2.getBody());
        assertTrue(lend2b.get("code").asInt() != 0, "重复出借应失败");

        String ret = "{\"orderId\":" + orderId + "}";
        assertEquals(0, postJson("/librarian/borrow/return", l, ret).get("code").asInt());
        JsonNode ret2 = postJson("/librarian/borrow/return", l, ret);
        assertTrue(ret2.get("code").asInt() != 0, "重复归还应失败");
    }

    @Test
    @Order(60)
    @DisplayName("预约冲突：同座位同时间段第二次失败")
    void reservationConflict() throws Exception {
        String u = login("user01", "123456");
        LocalDateTime start = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String json = String.format("{\"seatId\":1,\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                fmt.format(start), fmt.format(end));
        assertEquals(0, postJson("/user/reservation/create", u, json).get("code").asInt());
        JsonNode second = postJson("/user/reservation/create", u, json);
        assertTrue(second.get("code").asInt() != 0, "座位冲突应失败");
    }

    @Test
    @Order(70)
    @DisplayName("Dashboard overview remains available")
    void dashboardOverviewStillAvailable() throws Exception {
        String l = login("librarian", "123456");
        JsonNode ok = getJson("/admin/dashboard/overview", l);
        assertEquals(0, ok.get("code").asInt());
    }

}
