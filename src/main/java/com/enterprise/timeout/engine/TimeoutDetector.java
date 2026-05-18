package com.enterprise.timeout.engine;

import com.enterprise.timeout.config.GovernanceProperties;
import com.enterprise.timeout.model.*;
import com.enterprise.timeout.repository.TimeoutEventRepository;
import com.enterprise.timeout.service.AlertService;
import com.enterprise.timeout.audit.AuditService;
import com.enterprise.timeout.metrics.GovernanceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeoutDetector {

    private final PolicyProvider policyProvider;
    private final DolphinSchedulerClient dsClient;
    private final AlertService alertService;
    private final AuditService auditService;
    private final GovernanceMetrics metrics;
    private final TimeoutEventRepository timeoutEventRepository;
    private final GovernanceProperties properties;

    @Scheduled(fixedDelay = 30000)
    public void detectTimeouts() {
        if (properties.getDolphinscheduler().getToken().isBlank()) {
            return;
        }
        List<TaskInstance> runningTasks = dsClient.getRunningTasks();
        log.debug("Checking {} running tasks for timeout", runningTasks.size());

        for (TaskInstance task : runningTasks) {
            checkTaskTimeout(task);
        }
    }

    private void checkTaskTimeout(TaskInstance task) {
        Optional<TimeoutPolicy> policy = resolvePolicy(task);
        if (policy.isEmpty()) {
            return;
        }

        TimeoutPolicy p = policy.get();
        int runningMinutes = task.getRunningMinutes();

        if (runningMinutes > p.getTimeoutMinutes()) {
            handleTimeout(task, p, runningMinutes);
        }
    }

    private Optional<TimeoutPolicy> resolvePolicy(TaskInstance task) {
        Optional<TimeoutPolicy> taskPolicy = policyProvider.findPolicy(
                PolicyLevel.TASK, task.getTaskId(), task.getTeamId());
        if (taskPolicy.isPresent()) return taskPolicy;

        Optional<TimeoutPolicy> workflowPolicy = policyProvider.findPolicy(
                PolicyLevel.WORKFLOW, task.getWorkflowId(), task.getTeamId());
        if (workflowPolicy.isPresent()) return workflowPolicy;

        return policyProvider.findPolicy(PolicyLevel.TEAM, null, task.getTeamId());
    }

    private void handleTimeout(TaskInstance task, TimeoutPolicy policy, int actualMinutes) {
        TimeoutEvent event = TimeoutEvent.builder()
                .workflowId(task.getWorkflowId())
                .workflowName(task.getWorkflowName())
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .teamId(task.getTeamId())
                .policyLevel(policy.getLevel())
                .timeoutMinutes(policy.getTimeoutMinutes())
                .actualDurationMinutes(actualMinutes)
                .actionTaken(policy.getAction())
                .detectedAt(LocalDateTime.now())
                .escalated(false)
                .build();

        timeoutEventRepository.save(event);
        metrics.recordTimeout(task.getTeamId(), policy.getLevel().name());

        auditService.log(AuditAction.TIMEOUT_DETECTED, "system",
                "task", task.getTaskId(),
                String.format("Task '%s' exceeded timeout: %d/%d minutes",
                        task.getTaskName(), actualMinutes, policy.getTimeoutMinutes()));

        if (policy.getAction() == TimeoutAction.ALERT || policy.getAction() == TimeoutAction.ALERT_AND_KILL) {
            alertService.sendAlert(event, policy);
        }

        if (policy.getAction() == TimeoutAction.KILL || policy.getAction() == TimeoutAction.ALERT_AND_KILL) {
            dsClient.killTask(task.getTaskId());
            auditService.log(AuditAction.TASK_KILLED, "system",
                    "task", task.getTaskId(), "Task killed due to timeout policy");
        }
    }
}
