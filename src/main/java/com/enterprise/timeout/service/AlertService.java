package com.enterprise.timeout.service;

import com.enterprise.timeout.alert.channel.AlertChannel;
import com.enterprise.timeout.alert.suppression.AlertSuppressor;
import com.enterprise.timeout.audit.AuditService;
import com.enterprise.timeout.config.GovernanceProperties;
import com.enterprise.timeout.metrics.GovernanceMetrics;
import com.enterprise.timeout.model.*;
import com.enterprise.timeout.repository.AlertRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final List<AlertChannel> alertChannels;
    private final AlertSuppressor alertSuppressor;
    private final AlertRecordRepository alertRecordRepository;
    private final AuditService auditService;
    private final GovernanceMetrics metrics;
    private final GovernanceProperties properties;

    private Map<String, AlertChannel> getChannelMap() {
        return alertChannels.stream()
                .collect(Collectors.toMap(AlertChannel::getName, Function.identity()));
    }

    public void sendAlert(TimeoutEvent event, TimeoutPolicy policy) {
        String suppressionKey = buildSuppressionKey(event);
        if (alertSuppressor.isSuppressed(suppressionKey)) {
            log.info("Alert suppressed for event: {}", suppressionKey);
            auditService.log(AuditAction.ALERT_SUPPRESSED, "system",
                    "timeout_event", String.valueOf(event.getId()),
                    "Alert suppressed within time window");
            metrics.recordAlertSuppressed(event.getTeamId());
            return;
        }

        String message = buildAlertMessage(event, policy);
        String[] channels = policy.getAlertChannels().split(",");
        Map<String, AlertChannel> channelMap = getChannelMap();

        for (String channelName : channels) {
            AlertChannel channel = channelMap.get(channelName.trim());
            if (channel == null) {
                log.warn("Unknown alert channel: {}", channelName);
                continue;
            }

            sendWithRetry(channel, policy.getEscalationContacts(), message, event);
        }

        alertSuppressor.markSent(suppressionKey);
    }

    private void sendWithRetry(AlertChannel channel, String recipient, String message, TimeoutEvent event) {
        int maxAttempts = properties.getAlert().getRetry().getMaxAttempts();
        int delaySeconds = properties.getAlert().getRetry().getDelaySeconds();

        AlertRecord record = AlertRecord.builder()
                .timeoutEventId(event.getId())
                .channel(channel.getName())
                .recipient(recipient)
                .message(message)
                .status(AlertStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            record.setAttemptCount(attempt);
            boolean success = channel.send(recipient, message, event);

            if (success) {
                record.setStatus(AlertStatus.SENT);
                record.setSentAt(LocalDateTime.now());
                alertRecordRepository.save(record);
                auditService.log(AuditAction.ALERT_SENT, "system",
                        "timeout_event", String.valueOf(event.getId()),
                        String.format("Alert sent via %s to %s", channel.getName(), recipient));
                metrics.recordAlertSent(channel.getName(), event.getTeamId());
                return;
            }

            if (attempt < maxAttempts) {
                try {
                    Thread.sleep(delaySeconds * 1000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        record.setStatus(AlertStatus.FAILED);
        record.setFailureReason("Max retry attempts exceeded");
        alertRecordRepository.save(record);
        auditService.log(AuditAction.ALERT_FAILED, "system",
                "timeout_event", String.valueOf(event.getId()),
                String.format("Alert failed via %s after %d attempts", channel.getName(), maxAttempts));
        metrics.recordAlertFailed(channel.getName(), event.getTeamId());
    }

    private String buildSuppressionKey(TimeoutEvent event) {
        return String.format("%s:%s:%s", event.getTeamId(), event.getWorkflowId(), event.getTaskId());
    }

    private String buildAlertMessage(TimeoutEvent event, TimeoutPolicy policy) {
        return String.format(
                "[TIMEOUT ALERT] Task '%s' in workflow '%s' (Team: %s) has exceeded timeout threshold.\n" +
                "Duration: %d minutes (Limit: %d minutes)\n" +
                "Policy: %s (%s level)\n" +
                "Action: %s\n" +
                "Time: %s",
                event.getTaskName(), event.getWorkflowName(), event.getTeamId(),
                event.getActualDurationMinutes(), event.getTimeoutMinutes(),
                policy.getName(), policy.getLevel(),
                policy.getAction(),
                event.getDetectedAt()
        );
    }
}
