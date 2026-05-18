package com.enterprise.timeout.engine;

import java.util.List;

public interface DolphinSchedulerClient {

    List<TaskInstance> getRunningTasks();

    void killTask(String taskId);
}
