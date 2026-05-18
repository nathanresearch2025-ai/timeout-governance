export type PolicyLevel = 'WORKFLOW' | 'TASK' | 'TEAM';

export type TimeoutAction = 'ALERT' | 'KILL' | 'ALERT_AND_KILL' | 'ESCALATE';

export interface TimeoutPolicy {
  id?: string;
  name: string;
  level: PolicyLevel;
  targetId: string;
  teamId: string;
  timeoutMinutes: number;
  action: TimeoutAction;
  alertChannels: string;
  escalationMinutes: number;
  escalationContacts: string;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface TimeoutEvent {
  id: number;
  workflowId: string;
  workflowName: string;
  taskId: string;
  taskName: string;
  teamId: string;
  policyLevel: PolicyLevel;
  timeoutMinutes: number;
  actualDurationMinutes: number;
  actionTaken: TimeoutAction;
  detectedAt: string;
  resolvedAt: string | null;
  escalated: boolean;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
