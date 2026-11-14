package com.example.hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny app used across all Docker modules.
 * Every endpoint exists to demonstrate one Docker concept.
 */
@RestController
public class HelloController {

    // Env var / config demo (module 01, 06)
    @Value("${APP_GREETING:Hello from Docker}")
    private String greeting;

    // Keeps memory allocated for the memory-limit demo (module 07)
    private static final List<byte[]> HOLDER = new ArrayList<>();

    @GetMapping("/hello")
    public Map<String, Object> hello() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("message", greeting);
        m.put("hostname", InetAddress.getLocalHost().getHostName()); // = container id (module 08 LB)
        m.put("user", System.getProperty("user.name"));
        m.put("javaVersion", System.getProperty("java.version"));
        m.put("availableProcessors", rt.availableProcessors());      // container-aware (module 07)
        m.put("maxHeapMB", rt.maxMemory() / 1024 / 1024);            // container-aware (module 07)
        return m;
    }

    /** Shows env vars passed with -e / env_file / compose environment. */
    @GetMapping("/env")
    public Map<String, String> env() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : List.of("APP_GREETING", "APP_ENV", "JAVA_TOOL_OPTIONS", "HOSTNAME")) {
            out.put(k, System.getenv(k));
        }
        return out;
    }

    /** Reads a Docker/Swarm secret mounted at /run/secrets/<name> (module 06, 12). Never do this in prod logs! */
    @GetMapping("/secret")
    public Map<String, Object> secret(@RequestParam(defaultValue = "db_password") String name) throws Exception {
        Path p = Path.of("/run/secrets", name);
        boolean exists = Files.exists(p);
        return Map.of("secretFileExists", exists,
                "length", exists ? Files.readString(p).trim().length() : 0);
    }

    /** Writes/reads a file to prove volume persistence (module 03). */
    @GetMapping("/visit")
    public Map<String, Object> visit(@RequestParam(defaultValue = "/data/visits.txt") String file) throws Exception {
        Path p = Path.of(file);
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        long n = Files.exists(p) ? Long.parseLong(Files.readString(p).trim()) : 0;
        Files.writeString(p, String.valueOf(++n));
        return Map.of("visits", n, "file", file);
    }

    /** Burns CPU for N seconds (module 07 cpu limit demo). */
    @GetMapping("/burn")
    public String burn(@RequestParam(defaultValue = "10") int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        Thread[] ts = new Thread[Runtime.getRuntime().availableProcessors()];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> { double x = 0; while (System.currentTimeMillis() < end) x += Math.sqrt(Math.random()); });
            ts[i].start();
        }
        for (Thread t : ts) try { t.join(); } catch (InterruptedException ignored) { }
        return "burned CPU on " + ts.length + " threads for " + seconds + "s";
    }

    /** Allocates N MB on the heap (module 07 memory limit / OOM demo). */
    @GetMapping("/alloc")
    public String alloc(@RequestParam(defaultValue = "50") int mb) {
        for (int i = 0; i < mb; i++) {
            byte[] b = new byte[1024 * 1024];
            java.util.Arrays.fill(b, (byte) 1);
            HOLDER.add(b);
        }
        return "allocated total " + HOLDER.size() + " MB";
    }

    @GetMapping("/free")
    public String free() { HOLDER.clear(); System.gc(); return "freed"; }

    /** Used to test graceful shutdown on SIGTERM (docker stop). */
    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "5") int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
        return "done after " + seconds + "s";
    }
}
