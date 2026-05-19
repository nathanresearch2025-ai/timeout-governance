package com.enterprise.timeout.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "timeout-governance")
public class GovernanceProperties {

    private AlertConfig alert = new AlertConfig();
    private EscalationConfig escalation = new EscalationConfig();
    private DolphinSchedulerConfig dolphinscheduler = new DolphinSchedulerConfig();

    @Data
    public static class AlertConfig {
        private RetryConfig retry = new RetryConfig();
        private SuppressionConfig suppression = new SuppressionConfig();
    }

    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private int delaySeconds = 30;
    }

    @Data
    public static class SuppressionConfig {
        private int defaultWindowMinutes = 15;
    }

    @Data
    public static class EscalationConfig {
        private int defaultWaitMinutes = 30;
    }

    @Data
    public static class DolphinSchedulerConfig {
        private String mode = "mock";
        private String apiUrl = "http://localhost:12345/dolphinscheduler";
        private String token = "";
    }
}
