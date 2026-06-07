package com.example.library.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AdminUserManagementTest extends AbstractLibraryIntegrationTest {

    @Test
    void adminCanPageAndQueryUserWithoutPassword() throws Exception {
        String adminToken = login("admin", "123456");

        JsonNode page = getJson("/admin/users/page?pageNo=1&pageSize=10&keyword=user", adminToken);

        assertEquals(0, page.get("code").asInt(), page.toString());
        JsonNode first = page.get("data").get("records").get(0);
        assertEquals("user01", first.get("username").asText());
        assertFalse(first.has("password"));

        JsonNode detail = getJson("/admin/users/3", adminToken);
        assertEquals(0, detail.get("code").asInt(), detail.toString());
        assertEquals("user01", detail.get("data").get("username").asText());
        assertFalse(detail.get("data").has("password"));
    }

    @Test
    void nonAdminAccessIsRejected() throws Exception {
        String userToken = login("user01", "123456");

        ResponseEntity<String> response = getRaw("/admin/users/page?pageNo=1&pageSize=10", userToken);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void disablingUserInvalidatesOldToken() throws Exception {
        String adminToken = login("admin", "123456");
        String userToken = login("user01", "123456");

        assertEquals(0, patchJson("/admin/users/3/status", adminToken, "{\"status\":0}").get("code").asInt());

        ResponseEntity<String> afterDisable = getRaw("/auth/me", userToken);
        assertEquals(HttpStatus.FORBIDDEN, afterDisable.getStatusCode());
    }

    @Test
    void kickingUserInvalidatesOldToken() throws Exception {
        String adminToken = login("admin", "123456");
        String userToken = login("user01", "123456");

        assertEquals(0, postJson("/admin/users/3/kick", adminToken, "{}").get("code").asInt());

        ResponseEntity<String> afterKick = getRaw("/auth/me", userToken);
        assertEquals(HttpStatus.UNAUTHORIZED, afterKick.getStatusCode());
    }

    @Test
    void changingRoleInvalidatesOldToken() throws Exception {
        String adminToken = login("admin", "123456");
        String librarianToken = login("librarian", "123456");

        assertEquals(0, patchJson("/admin/users/2/role", adminToken, "{\"role\":\"USER\"}").get("code").asInt());

        ResponseEntity<String> afterRoleChange = getRaw("/auth/me", librarianToken);
        assertEquals(HttpStatus.UNAUTHORIZED, afterRoleChange.getStatusCode());
    }

    @Test
    void cannotDisableSelf() throws Exception {
        String adminToken = login("admin", "123456");

        JsonNode response = patchJson("/admin/users/1/status", adminToken, "{\"status\":0}");

        assertEquals(400, response.get("code").asInt(), response.toString());
    }

    @Test
    void cannotKickSelf() throws Exception {
        String adminToken = login("admin", "123456");

        JsonNode response = postJson("/admin/users/1/kick", adminToken, "{}");

        assertEquals(400, response.get("code").asInt(), response.toString());
    }

    @Test
    void cannotDowngradeLastAdmin() throws Exception {
        jdbcTemplate.update("INSERT INTO user " +
                        "(id, username, password, nickname, role, status, token_version, create_time, update_time) " +
                        "SELECT 4, 'admin2', password, 'admin2', 'ADMIN', 1, 0, NOW(), NOW() FROM user WHERE id = 1");
        String admin2Token = login("admin2", "123456");

        assertEquals(0, patchJson("/admin/users/1/role", admin2Token, "{\"role\":\"LIBRARIAN\"}").get("code").asInt());
        JsonNode response = patchJson("/admin/users/4/role", admin2Token, "{\"role\":\"LIBRARIAN\"}");

        assertEquals(400, response.get("code").asInt(), response.toString());
    }
}
