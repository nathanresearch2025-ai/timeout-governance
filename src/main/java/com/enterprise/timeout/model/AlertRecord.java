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
@Table(name = "alert_record")
public class AlertRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long timeoutEventId;
    private String channel;
    private String recipient;
    private String message;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private int attemptCount;
    private String failureReason;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
