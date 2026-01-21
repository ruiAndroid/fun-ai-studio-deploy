package fun.ai.studio.deploy.runner.interfaces.dto;

import lombok.Data;

import java.time.Duration;
import java.time.Instant;

@Data
public class RunnerSummary {
    private String runnerId;
    private Long lastSeenAtMs;
    private String health; // HEALTHY / STALE

    public static RunnerSummary from(String runnerId, Instant lastSeenAt, Duration stale, Instant now) {
        RunnerSummary s = new RunnerSummary();
        s.setRunnerId(runnerId);
        if (lastSeenAt != null) {
            s.setLastSeenAtMs(lastSeenAt.toEpochMilli());
        }
        if (lastSeenAt == null) {
            s.setHealth("STALE");
            return s;
        }
        Duration d = stale == null ? Duration.ofSeconds(60) : stale;
        Instant n = now == null ? Instant.now() : now;
        s.setHealth(lastSeenAt.isAfter(n.minus(d)) ? "HEALTHY" : "STALE");
        return s;
    }
}


