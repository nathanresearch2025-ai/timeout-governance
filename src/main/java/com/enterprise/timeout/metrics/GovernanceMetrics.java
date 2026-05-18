package com.enterprise.timeout.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class GovernanceMetrics {

    private final MeterRegistry registry;

    public GovernanceMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordTimeout(String teamId, String policyLevel) {
        Counter.builder("timeout.events.total")
                .description("Total timeout events detected")
                .tag("team", teamId)
                .tag("policy_level", policyLevel)
                .register(registry)
                .increment();
    }

    public void recordAlertSent(String channel, String teamId) {
        Counter.builder("alert.sent.total")
                .description("Total alerts sent successfully")
                .tag("channel", channel)
                .tag("team", teamId)
                .register(registry)
                .increment();
    }

    public void recordAlertFailed(String channel, String teamId) {
        Counter.builder("alert.failed.total")
                .description("Total alerts that failed to send")
                .tag("channel", channel)
                .tag("team", teamId)
                .register(registry)
                .increment();
    }

    public void recordAlertSuppressed(String teamId) {
        Counter.builder("alert.suppressed.total")
                .description("Total alerts suppressed")
                .tag("team", teamId)
                .register(registry)
                .increment();
    }

    public void recordEscalation(String teamId) {
        Counter.builder("escalation.triggered.total")
                .description("Total escalations triggered")
                .tag("team", teamId)
                .register(registry)
                .increment();
    }

    public void recordSlaBreached(String workflowId, String teamId) {
        Counter.builder("sla.breached.total")
                .description("Total SLA breaches")
                .tag("workflow", workflowId)
                .tag("team", teamId)
                .register(registry)
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordDetectionDuration(Timer.Sample sample) {
        sample.stop(Timer.builder("timeout.detection.duration")
                .description("Time taken for timeout detection cycle")
                .register(registry));
    }
}
