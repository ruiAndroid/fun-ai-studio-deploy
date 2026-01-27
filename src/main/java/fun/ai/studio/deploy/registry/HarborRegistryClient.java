package fun.ai.studio.deploy.registry;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal Harbor API client for artifact deletion (best-effort).
 *
 * Uses Harbor V2 API:
 *  DELETE /api/v2.0/projects/{project}/repositories/{repo}/artifacts/{reference}
 *
 * Notes:
 * - Requires Harbor project "Allow deletion" enabled.
 * - Works with robot account basic auth: username like "robot$xxx", password is token.
 */
@Component
@ConditionalOnProperty(prefix = "deploy.registry.cleanup", name = "enabled", havingValue = "true")
public class HarborRegistryClient {

    private static final Logger log = LoggerFactory.getLogger(HarborRegistryClient.class);

    private final RegistryCleanupProperties props;
    private final RestTemplate rest = new RestTemplate();

    public HarborRegistryClient(RegistryCleanupProperties props) {
        this.props = props;
    }

    public boolean isEnabled() {
        return props != null
                && props.isEnabled()
                && StringUtils.hasText(props.getBaseUrl())
                && StringUtils.hasText(props.getProject())
                && StringUtils.hasText(props.getUsername())
                && StringUtils.hasText(props.getPassword())
                && !"CHANGE_ME".equals(props.getPassword());
    }

    public boolean deleteArtifact(String repository, String reference) {
        if (!isEnabled()) return false;
        if (!StringUtils.hasText(repository) || !StringUtils.hasText(reference)) return false;

        String base = props.getBaseUrl().trim();
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "http://" + base;
        }
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);

        String projectRaw = props.getProject().trim();
        // Harbor API already has project in path; repository should NOT include "{project}/" prefix.
        String repoRaw = repository.trim();
        if (repoRaw.startsWith(projectRaw + "/")) {
            repoRaw = repoRaw.substring(projectRaw.length() + 1);
        }

        // Harbor expects repository URL-encoded; "/" must become "%2F" if present.
        String repoEnc = urlEncodePath(repoRaw);
        String refEnc = urlEncodePath(reference.trim());
        String project = urlEncodePath(projectRaw);

        String url = base + "/api/v2.0/projects/" + project + "/repositories/" + repoEnc + "/artifacts/" + refEnc;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", basicAuth(props.getUsername().trim(), props.getPassword().trim()));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = rest.exchange(url, HttpMethod.DELETE, entity, String.class);
            // 200 OK (deleted), 404 (not found) both treated as success for idempotency.
            int code = resp.getStatusCode().value();
            return code == 200 || code == 202 || code == 404;
        } catch (HttpStatusCodeException e) {
            int code = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            // 404 treated as success for idempotency; others are real failures.
            if (code == 404) return true;
            log.warn("harbor delete failed: http={} repo={} ref={} url={} body={}",
                    code, repoRaw, reference, url, body);
            return false;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static String basicAuth(String user, String pass) {
        String raw = user + ":" + pass;
        String b64 = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + b64;
    }

    /**
     * Minimal encoding for path segments used by Harbor API: encode space and '%' and keep others.
     * We also encode "/" to "%2F" to allow nested repository names.
     */
    private static String urlEncodePath(String s) {
        String out = s.replace("%", "%25").replace(" ", "%20").replace("/", "%2F");
        out = out.replace("#", "%23").replace("?", "%3F");
        return out;
    }
}


