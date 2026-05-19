package com.enterprise.timeout.config;

import com.enterprise.timeout.model.*;
import com.enterprise.timeout.repository.TimeoutEventRepository;
import com.enterprise.timeout.repository.TimeoutPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TimeoutPolicyRepository policyRepository;
    private final TimeoutEventRepository eventRepository;

    @Override
    public void run(String... args) {
        if (policyRepository.count() > 0) return;

        policyRepository.saveAll(List.of(
                TimeoutPolicy.builder()
                        .name("Data Engineering Team Default")
                        .level(PolicyLevel.TEAM)
                        .targetId("")
                        .teamId("data-engineering")
                        .timeoutMinutes(120)
                        .action(TimeoutAction.ALERT)
                        .alertChannels("email,dingtalk")
                        .escalationMinutes(30)
                        .escalationContacts("team-lead@company.com")
                        .enabled(true)
                        .build(),
                TimeoutPolicy.builder()
                        .name("Daily ETL Workflow")
                        .level(PolicyLevel.WORKFLOW)
                        .targetId("workflow-etl-daily-001")
                        .teamId("data-engineering")
                        .timeoutMinutes(60)
                        .action(TimeoutAction.ALERT_AND_KILL)
                        .alertChannels("dingtalk,email")
                        .escalationMinutes(15)
                        .escalationContacts("oncall@company.com")
                        .enabled(true)
                        .build(),
                TimeoutPolicy.builder()
                        .name("ML Training Pipeline")
                        .level(PolicyLevel.WORKFLOW)
                        .targetId("workflow-ml-train-001")
                        .teamId("ml-platform")
                        .timeoutMinutes(240)
                        .action(TimeoutAction.ALERT)
                        .alertChannels("wechat,email")
                        .escalationMinutes(60)
                        .escalationContacts("ml-lead@company.com")
                        .enabled(true)
                        .build(),
                TimeoutPolicy.builder()
                        .name("Critical Export Task")
                        .level(PolicyLevel.TASK)
                        .targetId("task-export-critical-001")
                        .teamId("data-engineering")
                        .timeoutMinutes(30)
                        .action(TimeoutAction.ALERT)
                        .alertChannels("whatsapp,email")
                        .escalationMinutes(10)
                        .escalationContacts("manager@company.com")
                        .enabled(false)
                        .build(),
                TimeoutPolicy.builder()
                        .name("Bank Day-End Settlement Workflow")
                        .level(PolicyLevel.WORKFLOW)
                        .targetId("workflow-bank-settlement-001")
                        .teamId("bank-batch")
                        .timeoutMinutes(240)
                        .action(TimeoutAction.ALERT_AND_KILL)
                        .alertChannels("dingtalk,email,wechat")
                        .escalationMinutes(15)
                        .escalationContacts("oncall-leader@bank.com,batch-director@bank.com")
                        .enabled(true)
                        .build(),
                TimeoutPolicy.builder()
                        .name("Bank Reconciliation Workflow")
                        .level(PolicyLevel.WORKFLOW)
                        .targetId("workflow-bank-reconciliation-001")
                        .teamId("bank-batch")
                        .timeoutMinutes(120)
                        .action(TimeoutAction.ALERT_AND_KILL)
                        .alertChannels("dingtalk,email")
                        .escalationMinutes(20)
                        .escalationContacts("oncall-leader@bank.com")
                        .enabled(true)
                        .build(),
                TimeoutPolicy.builder()
                        .name("Bank Batch Team Default")
                        .level(PolicyLevel.TEAM)
                        .targetId("")
                        .teamId("bank-batch")
                        .timeoutMinutes(180)
                        .action(TimeoutAction.ALERT)
                        .alertChannels("dingtalk,email")
                        .escalationMinutes(30)
                        .escalationContacts("oncall-leader@bank.com")
                        .enabled(true)
                        .build()
        ));

        eventRepository.saveAll(List.of(
                TimeoutEvent.builder()
                        .workflowId("workflow-etl-daily-001")
                        .workflowName("Daily ETL Pipeline")
                        .taskId("task-001")
                        .taskName("Extract Customer Data")
                        .teamId("data-engineering")
                        .policyLevel(PolicyLevel.WORKFLOW)
                        .timeoutMinutes(60)
                        .actualDurationMinutes(85)
                        .actionTaken(TimeoutAction.ALERT_AND_KILL)
                        .detectedAt(LocalDateTime.now().minusHours(2))
                        .resolvedAt(LocalDateTime.now().minusHours(1))
                        .escalated(false)
                        .build(),
                TimeoutEvent.builder()
                        .workflowId("workflow-ml-train-001")
                        .workflowName("ML Training Pipeline")
                        .taskId("task-002")
                        .taskName("Model Training Step")
                        .teamId("ml-platform")
                        .policyLevel(PolicyLevel.WORKFLOW)
                        .timeoutMinutes(240)
                        .actualDurationMinutes(300)
                        .actionTaken(TimeoutAction.ALERT)
                        .detectedAt(LocalDateTime.now().minusMinutes(45))
                        .escalated(false)
                        .build(),
                TimeoutEvent.builder()
                        .workflowId("workflow-etl-daily-001")
                        .workflowName("Daily ETL Pipeline")
                        .taskId("task-003")
                        .taskName("Load to Warehouse")
                        .teamId("data-engineering")
                        .policyLevel(PolicyLevel.TASK)
                        .timeoutMinutes(30)
                        .actualDurationMinutes(42)
                        .actionTaken(TimeoutAction.ALERT)
                        .detectedAt(LocalDateTime.now().minusMinutes(10))
                        .escalated(true)
                        .build()
        ));
    }
}
