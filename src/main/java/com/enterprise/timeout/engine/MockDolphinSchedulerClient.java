package com.enterprise.timeout.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ConditionalOnProperty(name = "timeout-governance.dolphinscheduler.mode", havingValue = "mock", matchIfMissing = true)
public class MockDolphinSchedulerClient implements DolphinSchedulerClient {

    private final Map<String, WorkflowInstance> runningWorkflows = new ConcurrentHashMap<>();
    private final Map<String, TaskInstance> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> workflowStartTimes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> taskStartTimes = new ConcurrentHashMap<>();

    @Override
    public List<TaskInstance> getRunningTasks() {
        List<TaskInstance> result = new ArrayList<>();
        for (TaskInstance t : runningTasks.values()) {
            LocalDateTime startTime = taskStartTimes.get(t.getTaskId());
            int runningMinutes = startTime == null ? t.getRunningMinutes()
                    : (int) ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
            t.setRunningMinutes(runningMinutes);
            result.add(t);
        }
        return result;
    }

    @Override
    public List<WorkflowInstance> getRunningWorkflows() {
        List<WorkflowInstance> result = new ArrayList<>();
        for (WorkflowInstance w : runningWorkflows.values()) {
            LocalDateTime startTime = workflowStartTimes.get(w.getWorkflowId());
            int runningMinutes = startTime == null ? w.getRunningMinutes()
                    : (int) ChronoUnit.MINUTES.between(startTime, LocalDateTime.now());
            w.setRunningMinutes(runningMinutes);
            result.add(w);
        }
        return result;
    }

    @Override
    public void killTask(String taskId) {
        TaskInstance removed = runningTasks.remove(taskId);
        taskStartTimes.remove(taskId);
        log.info("[MOCK] Killed task: {} ({})", taskId, removed != null ? removed.getTaskName() : "unknown");
    }

    @Override
    public void killWorkflow(String workflowId) {
        WorkflowInstance removed = runningWorkflows.remove(workflowId);
        workflowStartTimes.remove(workflowId);
        log.info("[MOCK] Killed workflow: {} ({})", workflowId, removed != null ? removed.getWorkflowName() : "unknown");
    }

    public WorkflowInstance addRunningWorkflow(WorkflowInstance workflow, Integer startedMinutesAgo) {
        if (startedMinutesAgo != null) {
            workflowStartTimes.put(workflow.getWorkflowId(),
                    LocalDateTime.now().minusMinutes(startedMinutesAgo));
            workflow.setRunningMinutes(startedMinutesAgo);
        } else {
            workflowStartTimes.put(workflow.getWorkflowId(), LocalDateTime.now());
            workflow.setRunningMinutes(0);
        }
        runningWorkflows.put(workflow.getWorkflowId(), workflow);
        log.info("[MOCK] Injected running workflow: {} ({}), started {} min ago",
                workflow.getWorkflowId(), workflow.getWorkflowName(), startedMinutesAgo);
        return workflow;
    }

    public TaskInstance addRunningTask(TaskInstance task, Integer startedMinutesAgo) {
        if (startedMinutesAgo != null) {
            taskStartTimes.put(task.getTaskId(),
                    LocalDateTime.now().minusMinutes(startedMinutesAgo));
            task.setRunningMinutes(startedMinutesAgo);
        } else {
            taskStartTimes.put(task.getTaskId(), LocalDateTime.now());
            task.setRunningMinutes(0);
        }
        runningTasks.put(task.getTaskId(), task);
        log.info("[MOCK] Injected running task: {} ({}), started {} min ago",
                task.getTaskId(), task.getTaskName(), startedMinutesAgo);
        return task;
    }

    public void clear() {
        runningWorkflows.clear();
        runningTasks.clear();
        workflowStartTimes.clear();
        taskStartTimes.clear();
        log.info("[MOCK] Cleared all running workflows and tasks");
    }

    public Map<String, WorkflowInstance> getRawWorkflows() {
        return runningWorkflows;
    }

    public Map<String, TaskInstance> getRawTasks() {
        return runningTasks;
    }
}
