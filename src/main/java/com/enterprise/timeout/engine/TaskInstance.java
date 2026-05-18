package com.enterprise.timeout.engine;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskInstance {

    private String taskId;
    private String taskName;
    private String workflowId;
    private String workflowName;
    private String teamId;
    private int runningMinutes;
    private String status;
}
