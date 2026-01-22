# Runtime 节点注册表与选址（Deploy 控制面）

本文档描述 `fun-ai-studio-deploy` 的 Runtime 节点管理与选址能力：

- Runtime 节点（数据面）：每台节点跑 `runtime-agent` + `Docker` + `Traefik/Nginx`，承载用户应用容器
- Deploy 控制面：维护 runtime 节点注册表、健康判断、以及 `appId -> runtimeNode` 的粘性落点

## 1. 端口与安全边界

- Deploy 控制面（本服务）：对外不建议开放公网；至少应通过安全组限制来源
- Runtime 节点心跳：`POST /internal/runtime-nodes/heartbeat`
  - Header：`X-RT-Node-Token: <secret>`
- 运维接口：`/admin/runtime-nodes/**`
  - Header：`X-Admin-Token: <adminToken>`

## 2. 配置项（Deploy 控制面）

`application.properties`：

- `deploy.admin.enabled=true`
- `deploy.admin.token=CHANGE_ME_STRONG_ADMIN_TOKEN`
- `deploy.admin.allowed-ips=`（可选）

Runtime 注册表/心跳：

- `deploy.runtime-node-registry.enabled=true`
- `deploy.runtime-node-registry.shared-secret=CHANGE_ME_STRONG_SECRET`
- `deploy.runtime-node-registry.allowed-ips=`（可选）
- `deploy.runtime-node-registry.heartbeat-stale-seconds=60`

落库（建议生产开启，避免 Deploy 重启丢数据）：

- `deploy.runtime.persistence.enabled=true`
- `spring.datasource.url=...`
- `spring.datasource.username=...`
- `spring.datasource.password=...`
- （建议）`spring.jpa.hibernate.ddl-auto=update`

### 2.1 落库表（命名风格对齐 API 的 fun_ai_workspace_*）

Deploy 会维护两张表：

- `fun_ai_deploy_runtime_node`：runtime 节点注册表
  - `id`（主键，long）
  - `name`（唯一）
  - `agent_base_url` / `gateway_base_url`
  - `enabled`（0/1）
  - `weight`
  - `last_heartbeat_at`（datetime）
  - `create_time` / `update_time`（datetime）
- `fun_ai_deploy_runtime_placement`：`app_id -> node_id` 粘性落点
  - `app_id`（主键）
  - `node_id`
  - `last_active_at`（bigint，epoch ms）
  - `create_time` / `update_time`（datetime）

以及一张 “last-known” 表（对齐 API 的 `fun_ai_workspace_run` 用途）：

- `fun_ai_deploy_app_run`：应用最后一次部署观测（last-known）
  - `app_id`（主键）
  - `node_id`（当前落点）
  - `last_job_id` / `last_job_status`
  - `last_error`
  - `last_deployed_at`（epoch ms；仅 SUCCEEDED 时刷新）
  - `last_active_at`（epoch ms；每次 report 刷新）
  - `create_time` / `update_time`（datetime）

## 3. 节点心跳协议

请求：

- `POST /internal/runtime-nodes/heartbeat`
- body：
  - `nodeName`：节点名（唯一）
  - `agentBaseUrl`：runtime-agent 基址（Runner 调用）
  - `gatewayBaseUrl`：网关基址（对外访问入口）

行为：

- 按 `nodeName` upsert 节点
- 刷新 `lastHeartbeatAt`

## 4. 选址策略（appId -> node）

- 粘性落点：`ensurePlacement(appId)`，首次会选择一个健康节点并保存映射
- 一致性哈希：对 `appId.hashCode()` 做 `mod N`（节点按 id 排序）
- 仅选择：
  - `enabled=true`
  - `agentBaseUrl/gatewayBaseUrl` 非空
  - 心跳新鲜（未超过 stale 阈值）

> 当前默认实现为 InMemory（不依赖 DB）；生产建议开启 DB 落库，避免 Deploy 重启丢节点/落点数据：  
> - `deploy.runtime.persistence.enabled=true`  
> - 配置 `spring.datasource.url/username/password`（建议 MySQL）

## 5. 运维接口（admin）

- `GET /admin/runtime-nodes/list`
- `POST /admin/runtime-nodes/upsert`
- `POST /admin/runtime-nodes/set-enabled?name=...&enabled=true|false`
- `GET /admin/runtime-nodes/placements?nodeId=...&offset=0&limit=200`
- `POST /admin/runtime-nodes/reassign`（body：`appId`、`targetNodeId`）
- `POST /admin/runtime-nodes/drain`（body：`sourceNodeId`、`targetNodeId`、`limit`）

说明：

- reassign/drain 只改 “appId -> nodeId” 路由落点；是否触发容器重建由 Runner/Runtime-Agent 执行链路决定。


