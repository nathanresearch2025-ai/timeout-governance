package com.enterprise.timeout.alert.channel;

import com.enterprise.timeout.model.TimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class DingTalkAlertChannel implements AlertChannel {

    private final WebClient webClient = WebClient.create();

    @Override
    public String getName() {
        return "dingtalk";
    }

    @Override
    public boolean send(String recipient, String message, TimeoutEvent event) {
        try {
            Map<String, Object> payload = Map.of(
                    "msgtype", "text",
                    "text", Map.of("content", message)
            );

            webClient.post()
                    .uri(recipient)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("DingTalk alert sent to webhook: {}", recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to send DingTalk alert", e);
            return false;
        }
    }
}
