package com.enterprise.timeout.engine;

import com.enterprise.timeout.config.GovernanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DolphinSchedulerHttpClient implements DolphinSchedulerClient {

    private final GovernanceProperties properties;
    private final WebClient.Builder webClientBuilder;

    @Override
    public List<TaskInstance> getRunningTasks() {
        try {
            String baseUrl = properties.getDolphinscheduler().getApiUrl();
            WebClient client = webClientBuilder.baseUrl(baseUrl).build();

            Map<String, Object> response = client.get()
                    .uri("/projects/0/task-instances?state=RUNNING_EXECUTION&pageNo=1&pageSize=1000")
                    .header("token", properties.getDolphinscheduler().getToken())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (response == null || !response.containsKey("data")) {
                return Collections.emptyList();
            }

            return parseTaskInstances(response);
        } catch (Exception e) {
            log.error("Failed to fetch running tasks from DolphinScheduler", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void killTask(String taskId) {
        try {
            String baseUrl = properties.getDolphinscheduler().getApiUrl();
            WebClient client = webClientBuilder.baseUrl(baseUrl).build();

            client.post()
                    .uri("/projects/0/executors/execute?processInstanceId=" + taskId + "&executeType=STOP")
                    .header("token", properties.getDolphinscheduler().getToken())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Killed task: {}", taskId);
        } catch (Exception e) {
            log.error("Failed to kill task: {}", taskId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<TaskInstance> parseTaskInstances(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null || !data.containsKey("totalList")) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> taskList = (List<Map<String, Object>>) data.get("totalList");
        return taskList.stream()
                .map(this::mapToTaskInstance)
                .toList();
    }

    private TaskInstance mapToTaskInstance(Map<String, Object> raw) {
        long startTime = raw.containsKey("startTime") ?
                ((Number) raw.getOrDefault("startTime", 0)).longValue() : 0;
        int runningMinutes = startTime > 0 ?
                (int) ((System.currentTimeMillis() - startTime) / 60000) : 0;

        return TaskInstance.builder()
                .taskId(String.valueOf(raw.getOrDefault("id", "")))
                .taskName(String.valueOf(raw.getOrDefault("name", "")))
                .workflowId(String.valueOf(raw.getOrDefault("processInstanceId", "")))
                .workflowName(String.valueOf(raw.getOrDefault("processInstanceName", "")))
                .teamId(String.valueOf(raw.getOrDefault("projectCode", "default")))
                .runningMinutes(runningMinutes)
                .status(String.valueOf(raw.getOrDefault("state", "")))
                .build();
    }
}
