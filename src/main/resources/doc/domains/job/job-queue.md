# Job 任务队列（Runner 轮询版）

本文档描述 `fun-ai-studio-deploy` 当前阶段的 Job 模块设计：**控制面创建 Job，Runner 通过 HTTP 轮询领取并执行，执行中续租，完成后回传结果**。

## 0. 控制面 / Runner / 用户应用（Runtime）关系说明

一句话记忆：

- **控制面（Deploy 服务）**：负责“决定做什么”——创建任务、记录状态、鉴权/审计、把任务分配给 Runner
- **Runner（执行面）**：负责“把任务做完”——领取任务后执行构建/部署动作，并回传结果
- **用户应用（Runtime 容器）**：最终对外提供服务的应用进程/容器——用户访问的 `https://{domain}/apps/{appId}/...` 指向它

要点：

- Runner **不是** 网关，也 **不是** 用户应用；Runner 只是把用户应用“部署出来/更新掉”的执行者
- 控制面 **不直接执行用户代码**，只做编排与记录（降低风险、便于隔离）

典型端到端流程（Docker 阶段）：

1. 用户在平台点“部署”，API 调用 Deploy 控制面：创建 `PENDING` Job（payload 带 `appId/repoUrl/commit/env` 等）
2. Runner 轮询 `POST /deploy/jobs/claim` 领取任务，控制面原子改为 `RUNNING` 并写入 `runnerId + leaseExpireAt`
3. Runner 在部署服务器上执行：拉代码/构建镜像/推镜像/启动或更新 Runtime 容器（用户应用容器）
4. 执行中 Runner 周期性 heartbeat 续租；完成后 `report` 回传 `SUCCEEDED/FAILED`
5. 用户通过公网入口（网关）访问 `https://{domain}/apps/{appId}/...`，请求转发到对应的 Runtime 容器

## 1. 目标与边界

- **目标**：让 Job 成为“可被 Runner 执行”的任务队列，先跑通闭环（创建 → 领取 → 执行 → 回传）。
- **非目标（后续扩展）**：制品库/镜像仓库对接、K8s 发布编排、回滚/灰度、任务日志持久化、租约过期回收策略。

## 2. 强模块化分层（必须遵守）

Job 模块按四层拆分，依赖方向固定：

- **interfaces** → **application** → **domain**
- **infrastructure** 实现 application 的接口（可替换），但不反向依赖 interfaces

包路径（代码位置）：

- `fun.ai.studio.deploy.job.domain.*`
- `fun.ai.studio.deploy.job.application.*`
- `fun.ai.studio.deploy.job.infrastructure.*`
- `fun.ai.studio.deploy.job.interfaces.*`

## 3. 核心领域模型

### 3.1 JobStatus（状态机）

当前状态：`PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED`

流转规则（简化）：

- `PENDING -> RUNNING | CANCELLED`
- `RUNNING -> SUCCEEDED | FAILED | CANCELLED`
- 终态（`SUCCEEDED/FAILED/CANCELLED`）不允许再流转

> 说明：`RUNNING` 只能通过 **领取（claim）** 进入，避免“接口误用把任务强行改成 RUNNING”。

### 3.2 Job 的 Runner 执行字段

- `runnerId`：当前领取该任务的 runner 标识（仅 RUNNING 期有效）
- `leaseExpireAt`：租约到期时间；runner 必须在到期前 heartbeat 续租（回收策略后续补）

## 4. 仓储抽象与 InMemory 实现

### 4.1 Repository 抽象

application 层定义 `JobRepository`，核心点：

- `claimNext(runnerId, leaseDuration)`：**原子领取**一个 `PENDING` Job，返回 `RUNNING` Job；无任务返回 empty

### 4.2 InMemoryJobRepository

当前提供 `InMemoryJobRepository`（用于本地开发/单测），其 `claimNext` 通过同步锁保证同一时刻不会被多个 Runner 抢到同一个 Job。

> 后续落地 MySQL/Mongo 时，需要用“事务 + 条件更新（WHERE status=PENDING）”保证原子性。

## 5. 对外接口（Deploy 控制面）

统一前缀：`/deploy/jobs`

### 5.0 内部鉴权（API -> Deploy）

为避免 Deploy 控制面接口被直接暴露/滥用，Deploy 支持对“控制面调用接口”开启内部鉴权：

- Header：`X-DEPLOY-SECRET: <sharedSecret>`
- Deploy 配置：`deploy.proxy-auth.enabled=true`、`deploy.proxy-auth.shared-secret=...`
- 约束：**仅保护**创建/查询/列表/控制类接口；Runner 的 `claim/heartbeat/report` 默认放行

### 5.1 创建 Job（控制面/后台）

- `POST /deploy/jobs`
- body：`CreateJobRequest`
  - `type`：JobType（目前：`BUILD_AND_DEPLOY`）
  - `payload`：扩展字段 Map（如：`appId/repoUrl/commit/env`）
  
> 当启用 `deploy.proxy-auth.enabled=true` 时，调用方（API）必须带 `X-DEPLOY-SECRET`。

### 5.2 查询 / 列表

- `GET /deploy/jobs/{jobId}`
- `GET /deploy/jobs?limit=50`

> 当启用 `deploy.proxy-auth.enabled=true` 时，调用方（API）必须带 `X-DEPLOY-SECRET`。

### 5.3 Runner 轮询领取（claim）

- `POST /deploy/jobs/claim`
- body：`ClaimJobRequest`
  - `runnerId`（必填）
  - `leaseSeconds`（默认 30）
- 返回：
  - 有任务：`data` 为 `JobResponse`
  - 无任务：`data=null`（仍返回 code=200）

补充（A 方案）：当 payload 中包含 `appId` 时，控制面会在 claim 响应中附带 `runtimeNode`（agentBaseUrl/gatewayBaseUrl），Runner 可直接调用对应 Runtime-Agent，无需再做二次 resolve。

### 5.4 Runner 心跳续租（heartbeat）

- `POST /deploy/jobs/{jobId}/heartbeat`
- body：`HeartbeatJobRequest`
  - `runnerId`（必填，必须与 Job 中 runnerId 一致）
  - `extendSeconds`（默认 30）

### 5.5 Runner 回传结果（report）

- `POST /deploy/jobs/{jobId}/report`
- body：`ReportJobRequest`
  - `runnerId`（必填，RUNNING 时必须与 Job 中 runnerId 一致）
  - `status`（必填）：建议仅使用 `SUCCEEDED/FAILED/CANCELLED`
  - `errorMessage`（可选）：当 `FAILED` 时可传

## 6. Runner 侧建议流程（最小闭环）

循环：

1. `POST /deploy/jobs/claim`（拿不到任务则 sleep/backoff）
2. 拿到任务后开始执行
3. 执行中每隔 \(T\) 秒调用 heartbeat（\(T < leaseSeconds\)，例如 lease=30s，T=10s）
4. 执行完成后调用 report：`SUCCEEDED` 或 `FAILED`

## 7. 错误码语义（Result.code）

- `404`：Job 不存在（`NotFoundException`）
- `409`：状态冲突/runnerId 不匹配/非法流转（`ConflictException`）
- `500`：未捕获异常

## 8. 后续演进建议（不破坏模块化）

- **领取过滤/队列**：按 `type`、标签、环境等条件领取（避免不同 runner 误领）
- **租约过期回收策略**：过期后回到 `PENDING` 重试或转 `FAILED`（可配置）
- **幂等与重试**：report/heartbeat 的幂等 key；runner 网络抖动重试策略
- **持久化实现**：新增 `MySqlJobRepository`/`MongoJobRepository` 放在 infrastructure
- **审计与日志**：Job 事件表（created/claimed/heartbeat/reported），便于排障


