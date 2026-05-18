package com.enterprise.timeout.alert.escalation;

import com.enterprise.timeout.audit.AuditService;
import com.enterprise.timeout.config.GovernanceProperties;
import com.enterprise.timeout.metrics.GovernanceMetrics;
import com.enterprise.timeout.model.AuditAction;
import com.enterprise.timeout.model.TimeoutEvent;
import com.enterprise.timeout.repository.TimeoutEventRepository;
import com.enterprise.timeout.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationService {

    private final TimeoutEventRepository timeoutEventRepository;
    private final GovernanceProperties properties;
    private final AuditService auditService;
    private final GovernanceMetrics metrics;

    @Scheduled(fixedDelay = 60000)
    public void checkEscalations() {
        List<TimeoutEvent> unresolvedEvents = timeoutEventRepository.findByResolvedAtIsNullAndEscalatedFalse();
        int waitMinutes = properties.getEscalation().getDefaultWaitMinutes();

        for (TimeoutEvent event : unresolvedEvents) {
            if (shouldEscalate(event, waitMinutes)) {
                escalate(event);
            }
        }
    }

    private boolean shouldEscalate(TimeoutEvent event, int waitMinutes) {
        return event.getDetectedAt().plusMinutes(waitMinutes).isBefore(LocalDateTime.now());
    }

    private void escalate(TimeoutEvent event) {
        event.setEscalated(true);
        timeoutEventRepository.save(event);

        auditService.log(AuditAction.ESCALATION_TRIGGERED, "system",
                "timeout_event", String.valueOf(event.getId()),
                String.format("Escalation triggered for task '%s' - unresolved for extended period",
                        event.getTaskName()));

        metrics.recordEscalation(event.getTeamId());
        log.warn("Escalation triggered for timeout event: {} (task: {})", event.getId(), event.getTaskName());
    }
}
