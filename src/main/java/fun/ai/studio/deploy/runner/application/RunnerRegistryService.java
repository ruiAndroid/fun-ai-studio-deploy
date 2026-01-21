package fun.ai.studio.deploy.runner.application;

import fun.ai.studio.deploy.runner.config.RunnerRegistryProperties;
import fun.ai.studio.deploy.runner.interfaces.dto.RunnerSummary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量 Runner 在线状态登记：
 * - runnerId -> lastSeenAt
 *
 * 数据存内存即可：部署控制面重启后会重新积累；对“在线状态面板”足够。
 */
@Service
public class RunnerRegistryService {

    private final RunnerRegistryProperties props;
    private final Map<String, Instant> lastSeen = new ConcurrentHashMap<>();

    public RunnerRegistryService(RunnerRegistryProperties props) {
        this.props = props;
    }

    public void touch(String runnerId) {
        if (!StringUtils.hasText(runnerId)) return;
        lastSeen.put(runnerId.trim(), Instant.now());
    }

    public List<RunnerSummary> list() {
        Instant now = Instant.now();
        return lastSeen.entrySet().stream()
                .map(e -> RunnerSummary.from(e.getKey(), e.getValue(), props == null ? null : props.heartbeatStaleDuration(), now))
                .sorted(Comparator.comparing(RunnerSummary::getHealth, Comparator.nullsLast(String::compareTo))
                        .thenComparing(RunnerSummary::getRunnerId, Comparator.nullsLast(String::compareTo)))
                .toList();
    }
}


