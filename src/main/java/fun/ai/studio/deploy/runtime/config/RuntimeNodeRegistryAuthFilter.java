package fun.ai.studio.deploy.runtime.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * runtime-node 心跳鉴权（不依赖用户体系）：可选 IP 白名单 + 共享密钥 Header。
 *
 * 仅保护：/internal/runtime-nodes/heartbeat
 */
public class RuntimeNodeRegistryAuthFilter extends OncePerRequestFilter {
    private static final String PATH = "/internal/runtime-nodes/heartbeat";
    private static final String HDR = "X-RT-Node-Token";

    private final RuntimeNodeRegistryProperties props;

    public RuntimeNodeRegistryAuthFilter(RuntimeNodeRegistryProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request == null) return true;
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri) || !PATH.equals(uri)) return true;
        return props != null && !props.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (props == null || !props.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 可选 IP 白名单：为空则不校验
        List<String> allowed = props.getAllowedIps();
        if (allowed != null && !allowed.isEmpty()) {
            String remoteIp = request.getRemoteAddr();
            String forwardedIp = clientIp(request);
            if (!isAllowedIp(remoteIp, allowed) && !isAllowedIp(forwardedIp, allowed)) {
                deny(response, 403, "runtime-node heartbeat forbidden: ip not allowed");
                return;
            }
        }

        String expected = props.getSharedSecret();
        if (!StringUtils.hasText(expected) || "CHANGE_ME_STRONG_SECRET".equals(expected)) {
            deny(response, 500, "runtime-node registry secret not configured");
            return;
        }

        String got = request.getHeader(HDR);
        if (!expected.equals(got)) {
            deny(response, 401, "runtime-node heartbeat unauthorized");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedIp(String ip, List<String> allowed) {
        if (!StringUtils.hasText(ip)) return false;
        for (String a : allowed) {
            if (!StringUtils.hasText(a)) continue;
            if (ip.equals(a.trim())) return true;
        }
        return false;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String first = xff.split(",")[0].trim();
            if (StringUtils.hasText(first)) return first;
        }
        String xri = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xri)) return xri.trim();
        return request.getRemoteAddr();
    }

    private void deny(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        resp.setContentType(MediaType.TEXT_PLAIN_VALUE);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.getWriter().write(msg == null ? "" : msg);
    }
}


