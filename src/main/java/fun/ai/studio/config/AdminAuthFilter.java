package fun.ai.studio.config;

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
 * Deploy 控制面 /admin/** 管理接口鉴权：
 * - 来源 IP 在白名单内（可选）
 * - Header 携带 X-Admin-Token
 */
public class AdminAuthFilter extends OncePerRequestFilter {
    private static final String PREFIX = "/admin/";
    private static final String HDR = "X-Admin-Token";

    private final AdminSecurityProperties props;

    public AdminAuthFilter(AdminSecurityProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (request == null) return true;
        String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri) || !uri.startsWith(PREFIX)) return true;
        return props != null && !props.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (props == null || !props.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 可选 IP 白名单（为空则不校验）
        List<String> allowed = props.getAllowedIps();
        if (allowed != null && !allowed.isEmpty()) {
            String remoteIp = request.getRemoteAddr();
            String forwardedIp = clientIp(request);
            if (!isAllowedIp(remoteIp, allowed) && !isAllowedIp(forwardedIp, allowed)) {
                deny(response, 403, "admin forbidden: ip not allowed");
                return;
            }
        }

        String expected = props.getToken();
        if (!StringUtils.hasText(expected) || "CHANGE_ME_STRONG_ADMIN_TOKEN".equals(expected)) {
            deny(response, 500, "admin token not configured");
            return;
        }

        String got = request.getHeader(HDR);
        if (!expected.equals(got)) {
            deny(response, 401, "admin unauthorized");
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


