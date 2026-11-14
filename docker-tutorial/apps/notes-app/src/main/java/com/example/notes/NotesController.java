package com.example.notes;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** PostgreSQL (persistent data) + Redis (counters) = a classic 3-container stack. */
@RestController
public class NotesController {

    public record NoteRequest(String text) { }

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    // Overridden by a Compose "config" file in module 06
    @Value("${app.banner:default banner (no external config mounted)}")
    private String banner;

    public NotesController(JdbcTemplate jdbc, StringRedisTemplate redis) {
        this.jdbc = jdbc;
        this.redis = redis;
    }

    @PostMapping("/notes")
    public Map<String, Object> add(@RequestBody NoteRequest req) {
        jdbc.update("insert into notes(text) values (?)", req.text());
        Long total = redis.opsForValue().increment("notes:created");
        return Map.of("status", "created", "totalCreatedSinceRedisStart", total);
    }

    @GetMapping("/notes")
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("select id, text, created_at from notes order by id desc limit 50");
    }

    @GetMapping("/visits")
    public Map<String, Object> visits() throws Exception {
        Long v = redis.opsForValue().increment("visits");
        return Map.of("visits", v, "servedBy", InetAddress.getLocalHost().getHostName());
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("banner", banner);
        m.put("dbUrl", System.getenv("DB_URL"));
        return m;
    }
}
