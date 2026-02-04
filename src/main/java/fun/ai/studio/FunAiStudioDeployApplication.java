package fun.ai.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class FunAiStudioDeployApplication {
    public static void main(String[] args) {
        // Ensure consistent datetime behavior regardless of host/container OS timezone.
        // Deploy writes create_time/update_time via LocalDateTime.now() in JPA entities.
        // Default: Asia/Shanghai (can override via FUNAI_TZ or -Duser.timezone).
        String tz = System.getProperty("user.timezone");
        if (tz == null || tz.isBlank()) {
            tz = System.getenv("FUNAI_TZ");
        }
        if (tz == null || tz.isBlank()) {
            tz = "Asia/Shanghai";
        }
        TimeZone.setDefault(TimeZone.getTimeZone(tz));
        System.setProperty("user.timezone", tz);
        SpringApplication.run(FunAiStudioDeployApplication.class, args);
    }
}


