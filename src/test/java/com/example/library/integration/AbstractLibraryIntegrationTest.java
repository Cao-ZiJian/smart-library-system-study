package com.example.library.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 共享 Testcontainers（MySQL + Redis）、数据源与 HTTP 工具方法。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@Sql(scripts = {"/schema.sql", "/integration-test-data.sql"}, config = @SqlConfig(encoding = "UTF-8"), executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractLibraryIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", MYSQL::getJdbcUrl);
        r.add("spring.datasource.username", MYSQL::getUsername);
        r.add("spring.datasource.password", MYSQL::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.redis.host", REDIS::getHost);
        r.add("spring.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected DataSource dataSource;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String base() {
        return "http://127.0.0.1:" + port + "/api";
    }

    protected JsonNode postJson(String path, String token, String jsonBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> resp = restTemplate.exchange(base() + path, HttpMethod.POST, entity, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    protected JsonNode putJson(String path, String token, String jsonBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> resp = restTemplate.exchange(base() + path, HttpMethod.PUT, entity, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    protected JsonNode getJson(String path, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> resp = restTemplate.exchange(base() + path, HttpMethod.GET, entity, String.class);
        return objectMapper.readTree(resp.getBody());
    }

    protected ResponseEntity<String> getRaw(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(base() + path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    protected ResponseEntity<String> putRaw(String path, String token, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(base() + path, HttpMethod.PUT, new HttpEntity<>(jsonBody, headers), String.class);
    }

    protected JsonNode deleteJson(String path, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        ResponseEntity<String> resp = restTemplate.exchange(base() + path, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        return objectMapper.readTree(resp.getBody());
    }

    protected String login(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(base() + "/auth/login", new HttpEntity<>(body, headers), String.class);
        JsonNode root = objectMapper.readTree(resp.getBody());
        assertEquals(0, root.get("code").asInt(), root.toString());
        return root.get("data").asText();
    }

    protected static HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }
}
