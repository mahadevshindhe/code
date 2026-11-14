package com.example.hello;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
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
 * Small app used across all Kubernetes modules.
 * Each endpoint exists to demonstrate one Kubernetes concept.
 */
@RestController
public class HelloController {

    @Value("${APP_GREETING:Hello from Kubernetes}")
    private String greeting;

    @Value("${APP_VERSION:v1}")
    private String version;

    private static final List<byte[]> HOLDER = new ArrayList<>();
    private final ApplicationEventPublisher publisher;

    public HelloController(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /** Pod name (HOSTNAME) shows which replica answered - Services, Ingress, HPA modules. */
    @GetMapping("/hello")
    public Map<String, Object> hello() throws Exception {
        Runtime rt = Runtime.getRuntime();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("message", greeting);
        m.put("version", version);
        m.put("pod", InetAddress.getLocalHost().getHostName());
        m.put("node", System.getenv("NODE_NAME"));             // injected via Downward API
        m.put("namespace", System.getenv("POD_NAMESPACE"));    // injected via Downward API
        m.put("podIp", System.getenv("POD_IP"));               // injected via Downward API
        m.put("availableProcessors", rt.availableProcessors());
        m.put("maxHeapMB", rt.maxMemory() / 1024 / 1024);
        return m;
    }

    @GetMapping("/env")
    public Map<String, String> env() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String k : List.of("APP_GREETING", "APP_VERSION", "APP_ENV", "FEATURE_FLAG", "JAVA_TOOL_OPTIONS", "HOSTNAME")) {
            out.put(k, System.getenv(k));
        }
        return out;
    }

    /** Returns the content of a mounted ConfigMap/Secret file (only /etc/** and /config/** allowed). Demo only! */
    @GetMapping("/file")
    public String file(@RequestParam String path) throws Exception {
        Path p = Path.of(path).normalize();
        if (!(p.startsWith("/etc/") || p.startsWith("/config/"))) return "path not allowed";
        return Files.exists(p) ? Files.readString(p) : "file not found: " + path;
    }

    /** Reads only the LENGTH of a secret file (never print secrets). */
    @GetMapping("/secret")
    public Map<String, Object> secret(@RequestParam(defaultValue = "/etc/secret/db_password") String path) throws Exception {
        Path p = Path.of(path);
        boolean exists = Files.exists(p);
        return Map.of("secretFileExists", exists, "length", exists ? Files.readString(p).trim().length() : 0);
    }

    /** Counter persisted on a mounted volume (PVC module). */
    @GetMapping("/visit")
    public Map<String, Object> visit(@RequestParam(defaultValue = "/data/visits.txt") String file) throws Exception {
        Path p = Path.of(file);
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        long n = Files.exists(p) ? Long.parseLong(Files.readString(p).trim()) : 0;
        Files.writeString(p, String.valueOf(++n));
        return Map.of("visits", n, "file", file);
    }

    /** CPU load for HPA demo. */
    @GetMapping("/burn")
    public String burn(@RequestParam(defaultValue = "10") int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        Thread[] ts = new Thread[Math.max(1, Runtime.getRuntime().availableProcessors())];
        for (int i = 0; i < ts.length; i++) {
            ts[i] = new Thread(() -> { double x = 0; while (System.currentTimeMillis() < end) x += Math.sqrt(Math.random()); });
            ts[i].start();
        }
        for (Thread t : ts) try { t.join(); } catch (InterruptedException ignored) { }
        return "burned CPU for " + seconds + "s";
    }

    /** Memory pressure -> OOMKilled demo (resources module). */
    @GetMapping("/alloc")
    public String alloc(@RequestParam(defaultValue = "50") int mb) {
        for (int i = 0; i < mb; i++) {
            byte[] b = new byte[1024 * 1024];
            java.util.Arrays.fill(b, (byte) 1);
            HOLDER.add(b);
        }
        return "allocated total " + HOLDER.size() + " MB";
    }

    @GetMapping("/slow")
    public String slow(@RequestParam(defaultValue = "5") int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
        return "done after " + seconds + "s";
    }

    // ---- Probe demos: flip the app's own health state ----
    @GetMapping("/admin/unready")
    public String unready() {
        AvailabilityChangeEvent.publish(publisher, this, ReadinessState.REFUSING_TRAFFIC);
        return "readiness -> REFUSING_TRAFFIC (pod removed from Service endpoints)";
    }

    @GetMapping("/admin/ready")
    public String ready() {
        AvailabilityChangeEvent.publish(publisher, this, ReadinessState.ACCEPTING_TRAFFIC);
        return "readiness -> ACCEPTING_TRAFFIC";
    }

    @GetMapping("/admin/unhealthy")
    public String unhealthy() {
        AvailabilityChangeEvent.publish(publisher, this, LivenessState.BROKEN);
        return "liveness -> BROKEN (kubelet will restart the container)";
    }
}
