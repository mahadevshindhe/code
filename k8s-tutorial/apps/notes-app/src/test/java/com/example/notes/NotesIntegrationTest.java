package com.example.notes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Module 16: real PostgreSQL + Redis started in Docker for the duration of the test.
 * Requires a running Docker daemon.  Run:  mvn test
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotesIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    TestRestTemplate rest;

    @Test
    void createAndListNotes() {
        rest.postForEntity("/notes", Map.of("text", "hello testcontainers"), Map.class);
        String body = rest.getForObject("/notes", String.class);
        assertThat(body).contains("hello testcontainers");
    }

    @Test
    void visitsCounterIncrements() {
        Map<?, ?> first = rest.getForObject("/visits", Map.class);
        Map<?, ?> second = rest.getForObject("/visits", Map.class);
        assertThat(((Number) second.get("visits")).longValue())
                .isGreaterThan(((Number) first.get("visits")).longValue());
    }
}
