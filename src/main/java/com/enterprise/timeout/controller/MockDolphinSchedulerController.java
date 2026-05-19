package com.enterprise.timeout.controller;

import com.enterprise.timeout.engine.MockDolphinSchedulerClient;
import com.enterprise.timeout.engine.TaskInstance;
import com.enterprise.timeout.engine.TimeoutDetector;
import com.enterprise.timeout.engine.WorkflowInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mock-ds")
@RequiredArgsConstructor
public class MockDolphinSchedulerController {

    private final MockDolphinSchedulerClient mockClient;
    private final TimeoutDetector timeoutDetector;

    @PostMapping("/workflows")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowInstance injectWorkflow(@RequestBody InjectWorkflowRequest req) {
        WorkflowInstance workflow = WorkflowInstance.builder()
                .workflowId(req.workflowId)
                .workflowName(req.workflowName)
                .teamId(req.teamId == null ? "default" : req.teamId)
                .status(req.status == null ? "RUNNING_EXECUTION" : req.status)
                .totalTasks(req.totalTasks == null ? 0 : req.totalTasks)
                .completedTasks(req.completedTasks == null ? 0 : req.completedTasks)
                .build();
        return mockClient.addRunningWorkflow(workflow, req.startedMinutesAgo);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskInstance injectTask(@RequestBody InjectTaskRequest req) {
        TaskInstance task = TaskInstance.builder()
                .taskId(req.taskId)
                .taskName(req.taskName)
                .workflowId(req.workflowId)
                .workflowName(req.workflowName)
                .teamId(req.teamId == null ? "default" : req.teamId)
                .status(req.status == null ? "RUNNING_EXECUTION" : req.status)
                .build();
        return mockClient.addRunningTask(task, req.startedMinutesAgo);
    }

    @GetMapping("/workflows")
    public Map<String, WorkflowInstance> listWorkflows() {
        return mockClient.getRawWorkflows();
    }

    @GetMapping("/tasks")
    public Map<String, TaskInstance> listTasks() {
        return mockClient.getRawTasks();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll() {
        mockClient.clear();
    }

    @PostMapping("/trigger-detection")
    public ResponseEntity<String> triggerDetection() {
        timeoutDetector.runDetectionNow();
        return ResponseEntity.ok("Detection triggered");
    }

    public static class InjectWorkflowRequest {
        public String workflowId;
        public String workflowName;
        public String teamId;
        public String status;
        public Integer totalTasks;
        public Integer completedTasks;
        public Integer startedMinutesAgo;
    }

    public static class InjectTaskRequest {
        public String taskId;
        public String taskName;
        public String workflowId;
        public String workflowName;
        public String teamId;
        public String status;
        public Integer startedMinutesAgo;
    }
}
