package fun.ai.studio.deploy.runtime.client;

import fun.ai.studio.common.ConflictException;
import fun.ai.studio.deploy.runtime.client.config.RuntimeAgentClientProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.util.Map;

/**
 * Deploy 控制面 -> runtime-agent 的最小 HTTP client。
 */
@Component
public class RuntimeAgentClient {

    private final RuntimeAgentClientProperties props;
    private final RestTemplate rest;

    public RuntimeAgentClient(RuntimeAgentClientProperties props) {
        this.props = props;
        this.rest = new RestTemplate(buildFactory(props));
    }

    public Map stopApp(String agentBaseUrl, String appId) {
        if (!StringUtils.hasText(agentBaseUrl)) throw new IllegalArgumentException("agentBaseUrl 不能为空");
        if (!StringUtils.hasText(appId)) throw new IllegalArgumentException("appId 不能为空");

        String token = props == null ? null : props.getToken();
        if (!StringUtils.hasText(token) || "CHANGE_ME".equals(token)) {
            throw new ConflictException("deploy.runtime-agent.token 未配置，无法调用 runtime-agent stop");
        }

        String url = agentBaseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        url = url + "/agent/apps/stop";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Runtime-Token", token);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("appId", appId), headers);
        ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.POST, entity, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new ConflictException("runtime-agent stop failed: http " + resp.getStatusCode().value());
        }
        Map body = resp.getBody();
        return body == null ? Map.of("appId", appId, "status", "UNKNOWN") : body;
    }

    private static SimpleClientHttpRequestFactory buildFactory(RuntimeAgentClientProperties props) {
        int timeoutSec = props == null ? 10 : props.getTimeoutSeconds();
        int ms = Math.max(1, timeoutSec) * 1000;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(ms);
        f.setReadTimeout(ms);
        return f;
    }
}


