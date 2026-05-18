package com.enterprise.timeout.model;

public enum AuditAction {
    TIMEOUT_DETECTED,
    ALERT_SENT,
    ALERT_FAILED,
    ALERT_SUPPRESSED,
    ESCALATION_TRIGGERED,
    POLICY_CREATED,
    POLICY_UPDATED,
    POLICY_DELETED,
    TASK_KILLED
}
