# Workflow 级别超时策略 — 实施与验证手册

本文档说明：基于 `MockDolphinSchedulerClient` 的工作流级别（Workflow Level）超时策略实现，以及如何使用 `postman/Workflow_Timeout_Policy_Tests.postman_collection.json` 完成端到端验证。

---

## 1. 背景与设计动机

DolphinScheduler 原生的超时告警只支持任务级别（Task）。在企业级使用中，往往需要：

- **工作流级别**：对整条 DAG 设置整体 SLA（例如 "日终结算工作流必须 4 小时内完成"）
- **团队级别**：给每个团队设默认超时兜底
- **任务级别**：对特定慢任务单独放宽

并且在没有真实 DS 集群的环境中（开发、CI、本地验证）也能完整验证治理逻辑。

为此，本项目在原有架构上：

1. 新增 **WORKFLOW 级别策略**的检测、kill、审计闭环
2. 引入 **Mock 模式**，通过 `@ConditionalOnProperty` 在 `mock` 与 `http` 两种 DS 客户端实现间切换
3. 新增专用的 `/api/v1/workflow-policies` REST 接口，支持按 target 查询、按 team 过滤、enable/disable 切换

---

## 2. 涉及的核心代码

### 2.1 数据模型

| 类 | 路径 | 说明 |
|-----|------|------|
| `PolicyLevel` | `model/PolicyLevel.java` | 枚举：TASK / WORKFLOW / TEAM |
| `TimeoutPolicy` | `model/TimeoutPolicy.java` | 策略实体（level、targetId、teamId、timeoutMinutes、action、alertChannels、escalation*） |
| `TimeoutAction` | `model/TimeoutAction.java` | 枚举：ALERT / KILL / ALERT_AND_KILL / ESCALATE |
| `WorkflowInstance` | `engine/WorkflowInstance.java` | 工作流实例值对象（workflowId、teamId、runningMinutes、status...） |

### 2.2 客户端可插拔（Mock vs HTTP）

```java
// MockDolphinSchedulerClient.java
@Component
@ConditionalOnProperty(
    name = "timeout-governance.dolphinscheduler.mode",
    havingValue = "mock",
    matchIfMissing = true)
public class MockDolphinSchedulerClient implements DolphinSchedulerClient { ... }

// DolphinSchedulerHttpClient.java
@Component
@ConditionalOnProperty(
    name = "timeout-governance.dolphinscheduler.mode",
    havingValue = "http")
public class DolphinSchedulerHttpClient implements DolphinSchedulerClient { ... }
```

`matchIfMissing = true` 意味着没显式配置时默认走 mock，本地启动即开即用。要接真实 DS，只需在 `application.yml` 设：

```yaml
timeout-governance:
  dolphinscheduler:
    mode: http
    api-url: http://<DS-host>:12345/dolphinscheduler
    token: <DS-token>
```

### 2.3 Mock 客户端的关键能力

`MockDolphinSchedulerClient` 在内存中维护 4 个 `ConcurrentHashMap`：

```java
runningWorkflows     // workflowId -> WorkflowInstance
runningTasks         // taskId     -> TaskInstance
workflowStartTimes   // workflowId -> LocalDateTime（注入时刻 - startedMinutesAgo）
taskStartTimes       // taskId     -> LocalDateTime
```

`getRunningWorkflows()` / `getRunningTasks()` 每次调用动态计算 `runningMinutes = NOW − startTime`，所以注入时只需传 `startedMinutesAgo=300`，后续每次轮询拿到的 `runningMinutes` 就是当前真实经过分钟数。

`killWorkflow(id)` 直接从 Map 移除，模拟 DS Stop API 的副作用。

### 2.4 注入与触发接口

```
POST   /api/v1/mock-ds/workflows         注入运行中工作流（含 startedMinutesAgo）
POST   /api/v1/mock-ds/tasks             注入运行中任务
GET    /api/v1/mock-ds/workflows         查看当前内存中工作流
DELETE /api/v1/mock-ds                   清空所有 mock 状态
POST   /api/v1/mock-ds/trigger-detection 立即触发一次超时检测（不必等 30 秒定时器）
```

### 2.5 策略匹配三级回退

`TimeoutDetector.checkWorkflowTimeout()` 优先 WORKFLOW 精确匹配，再回退 TEAM 默认：

```java
Optional<TimeoutPolicy> policy = policyProvider.findPolicy(
        PolicyLevel.WORKFLOW, workflow.getWorkflowId(), workflow.getTeamId());
if (policy.isEmpty()) {
    policy = policyProvider.findPolicy(PolicyLevel.TEAM, null, workflow.getTeamId());
}
```

任务级别的 `checkTaskTimeout()` 是 TASK → WORKFLOW → TEAM 三级回退。

### 2.6 工作流策略 REST 接口

`WorkflowPolicyController` 提供专用端点（自动设置 `level = WORKFLOW`）：

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/workflow-policies` | 列出所有 WORKFLOW 策略，支持 `teamId`、`enabled` 过滤 |
| GET | `/api/v1/workflow-policies/target/{id}` | 按 target workflow ID 查询 |
| POST | `/api/v1/workflow-policies` | 创建（level 自动设为 WORKFLOW） |
| PUT | `/api/v1/workflow-policies/{id}` | 更新 |
| DELETE | `/api/v1/workflow-policies/{id}` | 删除 |
| PATCH | `/api/v1/workflow-policies/{id}/toggle` | 切换启用状态 |

通用 `/api/v1/policies` 接口仍可用，且支持 `?level=WORKFLOW` 过滤。

---

## 3. 执行步骤

### 3.1 启动依赖

```bash
# 1) 启动 PostgreSQL 容器
docker run -d --name postgres-timeout -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres postgres:15-alpine

# 2) 创建数据库
docker exec postgres-timeout psql -U postgres \
  -c "CREATE DATABASE timeout_governance;"

# 3) 编译并启动后端（默认 mock 模式）
cd /hsbc-homework
mvn package -DskipTests
nohup java -jar target/dolphinscheduler-timeout-governance-1.0.0-SNAPSHOT.jar \
  > logs/backend.log 2>&1 &

# 4) 启动前端（可选）
cd frontend && nohup npm run dev -- --host 0.0.0.0 \
  > /hsbc-homework/logs/frontend.log 2>&1 &
```

启动后 `DataInitializer` 会自动种入策略数据，包括 4 条 WORKFLOW 级别策略：

| name | targetId | teamId | timeout | action |
|------|----------|--------|---------|--------|
| ETL Daily Workflow | workflow-etl-daily-001 | data-engineering | 60 | ALERT |
| Bank Day-End Settlement Workflow | workflow-bank-settlement-001 | bank-batch | 240 | ALERT_AND_KILL |
| Bank Reconciliation Workflow | workflow-bank-reconciliation-001 | bank-batch | 120 | ALERT_AND_KILL |
| (其他种子策略见 DataInitializer) | ... | ... | ... | ... |

### 3.2 导入 Postman 集合

**桌面版**：Postman → Import → 选择 `postman/Workflow_Timeout_Policy_Tests.postman_collection.json`

**Newman（命令行）**：
```bash
npm install -g newman
newman run postman/Workflow_Timeout_Policy_Tests.postman_collection.json \
  --delay-request 500
```

集合内置变量 `baseUrl = http://1.14.131.61:8090`，本地运行请改为 `http://localhost:8090`：
- 桌面版：右键 collection → Edit → Variables → 修改 `baseUrl`
- Newman：加 `--env-var "baseUrl=http://localhost:8090"`

### 3.3 集合内容（4 个分组共 18 个用例）

**分组 1 — General Policy API（通用策略接口）**
1. List All Policies (Paginated)
2. List Policies - Filter by WORKFLOW Level
3. List Policies - Filter by Team

**分组 2 — Workflow Policy API（专用工作流策略接口）**
4. List All Workflow Policies
5. List Workflow Policies - Filter by Team
6. Create Workflow Policy（创建后保存 ID 到变量 `workflowPolicyId`）
7. Get Workflow Policy by Target ID
8. Update Workflow Policy（用上一步保存的 ID）
9. Toggle Workflow Policy Enabled（启用 → 禁用）
10. Toggle Workflow Policy Enabled (Back to true)
11. Delete Workflow Policy
12. Verify Deletion - Get Deleted Policy Target（应 404）

**分组 3 — Workflow Policy Edge Cases（边界场景）**
13. Create Policy with ALERT action
14. Create Policy with KILL action
15. Create Disabled Workflow Policy（enabled=false）
16. Get Non-existent Target - 404

**分组 4 — Timeout Events（超时事件查询）**
17. List Timeout Events（分页）
18. List Unresolved Timeout Events

### 3.4 一键运行

桌面版：右键 collection → Run collection → 保持顺序、Delay 500ms → Run

命令行：
```bash
newman run postman/Workflow_Timeout_Policy_Tests.postman_collection.json \
  --delay-request 500 --reporter-cli-no-banner
```

**重要**：用例 6→7→8→9→10→11→12 是有依赖的链路（创建 → 查询 → 更新 → 切换 → 删除），必须按顺序执行。`workflowPolicyId` 变量在用例 6 写入，后续用例引用。

---

## 4. 关键用例详解

### 4.1 Create Workflow Policy（用例 6）

请求：
```json
POST /api/v1/workflow-policies
{
  "name": "Nightly Report Workflow",
  "targetId": "workflow-nightly-report-001",
  "teamId": "analytics-team",
  "timeoutMinutes": 90,
  "action": "ALERT_AND_KILL",
  "alertChannels": "email,dingtalk",
  "escalationMinutes": 20,
  "escalationContacts": "analytics-lead@company.com",
  "enabled": true
}
```

测试断言：
- HTTP 201 Created
- 响应中 `level` 自动设为 `WORKFLOW`（由控制器强制）
- `id` 不为空，并把它存入集合变量 `workflowPolicyId`，给后续用例使用

```javascript
pm.expect(jsonData.level).to.equal('WORKFLOW');
pm.expect(jsonData.id).to.not.be.empty;
pm.collectionVariables.set('workflowPolicyId', jsonData.id);
```

### 4.2 Toggle Enabled（用例 9–10）

`PATCH /api/v1/workflow-policies/{id}/toggle` 服务端把 `enabled` 取反并审计。两次连续调用可验证 enabled→disabled→enabled 的循环。

### 4.3 Get Non-existent Target（用例 16）

`GET /api/v1/workflow-policies/target/non-existent-workflow` 验证 404。控制器实现：

```java
return policyService.getByLevelAndTarget(PolicyLevel.WORKFLOW, targetId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
```

---

## 5. 验证结果

### 5.1 Newman 实跑结果

```
┌─────────────────────────┬───────────────────┬───────────────────┐
│                         │          executed │            failed │
├─────────────────────────┼───────────────────┼───────────────────┤
│              iterations │                 1 │                 0 │
│                requests │                18 │                 0 │
│            test-scripts │                18 │                 0 │
│              assertions │                34 │                 0 │
└─────────────────────────┴───────────────────┴───────────────────┘
total run duration: 10.1s
average response time: 30ms [min: 14ms, max: 116ms]
```

34 条断言全部通过，0 失败。

### 5.2 关键断言覆盖

| 断言类型 | 覆盖用例 | 验证目标 |
|----------|----------|----------|
| HTTP 状态 | 全部 | 200 / 201 / 204 / 404 |
| Level 一致 | 4, 6, 13, 14, 15 | 通过 `/workflow-policies` 创建 / 查询的对象 `level` 一定是 `WORKFLOW` |
| 团队过滤 | 5 | 返回结果 `teamId` 全部匹配查询参数 |
| Action 多样性 | 6, 13, 14 | 同时验证 ALERT、KILL、ALERT_AND_KILL 三种 action |
| 启用切换 | 9, 10 | 切换前后 `enabled` 取反 |
| ID 链路 | 6→8→11 | 创建拿到的 ID 能被更新和删除 |
| 删除 → 404 | 12 | 删除后再查 target 应 404 |
| 分页结构 | 1, 17 | 响应包含 `content`、`totalElements` |
| 未解决事件 | 18 | 返回数组中所有元素 `resolvedAt == null` |

### 5.3 数据库侧旁证

执行完整集合后，可以用以下查询确认审计链路完整：

```sql
-- 创建/更新/删除策略的审计
SELECT action, target_id, detail, created_at
FROM audit_log
WHERE target_type = 'policy'
ORDER BY created_at DESC LIMIT 10;
```

应能看到 POLICY_CREATED / POLICY_UPDATED / POLICY_DELETED 三类条目，证明 `PolicyService.create/update/delete` 正确触发了 `AuditService.log`。

---

## 6. 常见问题

**Q: Newman 跑的时候用例 7 拿不到刚创建的策略？**
A: 集合变量 `workflowPolicyId` 是 collection-level 的，Postman 桌面版和 Newman 都会持久化到下一次运行。如果你 import 后第一次跑，变量是空字符串，但用例 6 会立即写入，对用例 7 没有影响。如果出现拿不到的情况，确认 `--delay-request` 不为 0，或者顺序运行而非并行。

**Q: Mock 模式下定时器是否会污染测试？**
A: `TimeoutDetector` 默认每 30 秒检测一次。这个集合只测策略 CRUD，不依赖检测，定时器即使触发也不会影响断言。要做超时检测的端到端验证，请使用 `Bank_Batch_Timeout_Scenario.postman_collection.json`。

**Q: 怎么切到真实 DolphinScheduler？**
A: 见 2.2 节。把 mode 改为 `http` 并配置 token，重启后端即可。Postman 集合本身完全不变 — 它只测治理平台自身的 API，不直接调用 DS。

**Q: `level` 字段我手动传 TASK，会怎样？**
A: 通用接口 `/api/v1/policies` 会按你传的 level 创建。专用接口 `/api/v1/workflow-policies` 在控制器里强制覆盖为 WORKFLOW（见 `WorkflowPolicyController`），这是设计上的语义保护。

---

## 7. 文件清单

| 文件 | 说明 |
|------|------|
| `postman/Workflow_Timeout_Policy_Tests.postman_collection.json` | 本文档对应的 Postman 集合（18 用例） |
| `postman/Bank_Batch_Timeout_Scenario.postman_collection.json` | 银行批处理超时端到端场景（11 用例，含 mock 注入 + 检测触发） |
| `src/main/java/.../engine/MockDolphinSchedulerClient.java` | Mock DS 客户端，内存模拟运行中工作流 |
| `src/main/java/.../engine/DolphinSchedulerHttpClient.java` | 真实 HTTP DS 客户端 |
| `src/main/java/.../controller/WorkflowPolicyController.java` | 工作流策略专用 REST 接口 |
| `src/main/java/.../controller/MockDolphinSchedulerController.java` | Mock 注入 / 触发检测接口 |
| `src/main/java/.../engine/TimeoutDetector.java` | 超时检测主逻辑（含三级回退匹配） |
| `src/main/java/.../config/DataInitializer.java` | 启动种子数据 |

