package com.enterprise.timeout.alert.channel;

import com.enterprise.timeout.model.TimeoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailAlertChannel implements AlertChannel {

    @Override
    public String getName() {
        return "email";
    }

    @Override
    public boolean send(String recipient, String message, TimeoutEvent event) {
        try {
            // In production, integrate with SMTP or email service (SendGrid, SES, etc.)
            log.info("Sending email alert to {}: {}", recipient, message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", recipient, e);
            return false;
        }
    }
}
