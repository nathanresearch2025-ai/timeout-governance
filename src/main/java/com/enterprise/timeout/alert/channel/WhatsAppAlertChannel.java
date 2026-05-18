package com.enterprise.timeout.alert.channel;

import com.enterprise.timeout.model.TimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class WhatsAppAlertChannel implements AlertChannel {

    private final WebClient webClient = WebClient.create();

    @Override
    public String getName() {
        return "whatsapp";
    }

    @Override
    public boolean send(String recipient, String message, TimeoutEvent event) {
        try {
            Map<String, Object> payload = Map.of(
                    "messaging_product", "whatsapp",
                    "to", recipient,
                    "type", "text",
                    "text", Map.of("body", message)
            );

            webClient.post()
                    .uri("https://graph.facebook.com/v17.0/PHONE_NUMBER_ID/messages")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("WhatsApp alert sent to: {}", recipient);
            return true;
        } catch (Exception e) {
            log.error("Failed to send WhatsApp alert", e);
            return false;
        }
    }
}
