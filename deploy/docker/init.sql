-- DolphinScheduler Timeout Governance - PostgreSQL Schema
-- Database: timeout_governance

CREATE DATABASE timeout_governance;

\c timeout_governance;

-- Timeout Policy Table
CREATE TABLE timeout_policy (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(255) NOT NULL,
    level VARCHAR(20) NOT NULL,
    target_id VARCHAR(255) DEFAULT '',
    team_id VARCHAR(255) NOT NULL,
    timeout_minutes INTEGER NOT NULL,
    action VARCHAR(20) NOT NULL,
    alert_channels VARCHAR(500),
    escalation_minutes INTEGER DEFAULT 30,
    escalation_contacts VARCHAR(500),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Timeout Event Table
CREATE TABLE timeout_event (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(255),
    workflow_name VARCHAR(255),
    task_id VARCHAR(255),
    task_name VARCHAR(255),
    team_id VARCHAR(255),
    policy_level VARCHAR(20),
    timeout_minutes INTEGER,
    actual_duration_minutes INTEGER,
    action_taken VARCHAR(20),
    detected_at TIMESTAMP,
    resolved_at TIMESTAMP,
    escalated BOOLEAN DEFAULT false
);

-- Alert Record Table
CREATE TABLE alert_record (
    id BIGSERIAL PRIMARY KEY,
    timeout_event_id BIGINT REFERENCES timeout_event(id),
    channel VARCHAR(50),
    recipient VARCHAR(500),
    message TEXT,
    status VARCHAR(20),
    attempt_count INTEGER DEFAULT 0,
    failure_reason VARCHAR(500),
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Audit Log Table
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(255),
    target_type VARCHAR(100),
    target_id VARCHAR(255),
    detail TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_policy_team_id ON timeout_policy(team_id);
CREATE INDEX idx_policy_level ON timeout_policy(level);
CREATE INDEX idx_policy_enabled ON timeout_policy(enabled);

CREATE INDEX idx_event_team_id ON timeout_event(team_id);
CREATE INDEX idx_event_workflow_id ON timeout_event(workflow_id);
CREATE INDEX idx_event_detected_at ON timeout_event(detected_at);
CREATE INDEX idx_event_resolved_escalated ON timeout_event(resolved_at, escalated);

CREATE INDEX idx_alert_event_id ON alert_record(timeout_event_id);
CREATE INDEX idx_alert_status ON alert_record(status);

CREATE INDEX idx_audit_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);

-- Sample Data
INSERT INTO timeout_policy (name, level, target_id, team_id, timeout_minutes, action, alert_channels, escalation_minutes, escalation_contacts, enabled) VALUES
('Data Engineering Team Default', 'TEAM', '', 'data-engineering', 120, 'ALERT', 'email,dingtalk', 30, 'team-lead@company.com', true),
('Daily ETL Workflow', 'WORKFLOW', 'workflow-etl-daily-001', 'data-engineering', 60, 'ALERT_AND_KILL', 'dingtalk,email', 15, 'oncall@company.com', true),
('ML Training Pipeline', 'WORKFLOW', 'workflow-ml-train-001', 'ml-platform', 240, 'ALERT', 'wechat,email', 60, 'ml-lead@company.com', true),
('Critical Export Task', 'TASK', 'task-export-critical-001', 'data-engineering', 30, 'ALERT', 'whatsapp,email', 10, 'manager@company.com', false);

INSERT INTO timeout_event (workflow_id, workflow_name, task_id, task_name, team_id, policy_level, timeout_minutes, actual_duration_minutes, action_taken, detected_at, resolved_at, escalated) VALUES
('workflow-etl-daily-001', 'Daily ETL Pipeline', 'task-001', 'Extract Customer Data', 'data-engineering', 'WORKFLOW', 60, 85, 'ALERT_AND_KILL', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', false),
('workflow-ml-train-001', 'ML Training Pipeline', 'task-002', 'Model Training Step', 'ml-platform', 'WORKFLOW', 240, 300, 'ALERT', NOW() - INTERVAL '45 minutes', NULL, false),
('workflow-etl-daily-001', 'Daily ETL Pipeline', 'task-003', 'Load to Warehouse', 'data-engineering', 'TASK', 30, 42, 'ALERT', NOW() - INTERVAL '10 minutes', NULL, true);
