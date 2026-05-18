package com.enterprise.timeout.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "timeout_event")
public class TimeoutEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String workflowId;
    private String workflowName;
    private String taskId;
    private String taskName;
    private String teamId;

    @Enumerated(EnumType.STRING)
    private PolicyLevel policyLevel;

    private int timeoutMinutes;
    private int actualDurationMinutes;

    @Enumerated(EnumType.STRING)
    private TimeoutAction actionTaken;

    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private boolean escalated;
}
