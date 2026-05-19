package com.enterprise.timeout.engine;

import java.util.List;

public interface DolphinSchedulerClient {

    List<TaskInstance> getRunningTasks();

    List<WorkflowInstance> getRunningWorkflows();

    void killTask(String taskId);

    void killWorkflow(String workflowId);
}
