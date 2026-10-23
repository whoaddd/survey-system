# 问卷系统 Survey System

一个支持图文问卷发布、匿名答卷、AI 辅助评分的全栈问卷平台。

发布者可以登录后创建图文混排的问卷（文本/单选/多选/评分/图片五类题型），发布后生成分享链接；答卷者无需登录即可答题；发布者可在结果页查看答卷统计，并调用 AI 对文本答案和选项分布做辅助分析。

> **AI 能力定位**：所有 AI 输出均为"建议"，写入独立的 `ai_evaluation` 表，**不覆盖**用户原始打分，不直接影响业务数据。

---

## 功能特性

### 发布端

- 账号注册与登录（JWT 鉴权）
- 问卷创建、草稿保存、发布、关闭、复制为新草稿、删除
- 五种题型：文本、单选、多选、评分、图片
- 富文本题干（支持插入图片）
- 图片题（展示题目图片，答卷者文字作答）
- 图片直传 MinIO（预签名 URL，不经过业务服务内存）
- 问卷快照（发布后编辑不影响已收集答卷）
- 分享链接生成与复制

### 答卷端

- **无需登录**，打开分享链接即可答题
- 匿名身份（`respondent_key`）+ 唯一索引防重复提交
- 提交回执码（`receipt_code`）
- IP 限流
- 移动端适配（提交按钮固定底部）
- 必填校验 + 自动滚动定位

### 结果页

- 答卷总数、评分题均分、全部评分题总均分
- 评分题柱状图、单选/多选饼图（ECharts）
- 文本题答案列表
- **AI 辅助评分**：对文本答案做质量分析，对选择题做分布倾向分析

### AI 能力

- **AI 生成题目**：输入主题和数量，返回结构化题目草案，**需人工确认后保存**
- **AI 辅助评分**：
  - 文本/图片题：分析答案的相关性、完整性、洞察力
  - 选择题：分析选项分布的倾向性和集中度
  - 结果幂等写入 `ai_evaluation`，重复评分会覆盖旧值
  - 完整的审计记录（`model_name`、`prompt_version`、`response_id`）

---

## 技术栈

| 层            | 技术                 | 版本               |
| ------------- | -------------------- | ------------------ |
| **基础框架**  | Spring Boot          | 3.2.5              |
|               | Spring Cloud         | 2023.0.1           |
|               | Spring Cloud Alibaba | 2023.0.1.0         |
|               | Spring AI            | 1.0.0              |
| **数据库**    | MySQL                | 8.0                |
|               | MyBatis-Plus         | 3.5.7              |
| **缓存/限流** | Redis                | 7.x                |
| **对象存储**  | MinIO                | 2020-07-13（旧版） |
| **注册中心**  | Nacos                | 3.2.4              |
| **前端**      | Vue                  | 3.5                |
|               | Vite                 | 5.x                |
|               | Element Plus         | 2.x                |
|               | ECharts              | 5.x                |
|               | wangEditor           | 5.x                |
| **运行时**    | JDK                  | 21                 |
|               | Node                 | 20                 |

---

## 系统架构

```
浏览器
  │
  ├─ /admin/**        发布端（Vue 路由守卫，需 JWT）
  ├─ /s/{shareCode}   答卷端（公开）
  └─ /login
        │
        ▼
  ┌─────────────────┐
  │   web (Nginx)   │  :80
  │  静态资源 + /api │
  └────────┬────────┘
           ▼
  ┌─────────────────┐
  │ gateway (:8000) │  JWT 校验 + 白名单 + 路由
  └────────┬────────┘
           │
  ┌────────┼────────┬─────────┬──────────┐
  ▼        ▼        ▼         ▼          ▼
user    survey   response   file      ai-orch
:9001   :9002    :9003      :9005     :9004
  │        │        │         │          │
  ▼        ▼        ▼         ▼          ▼
user_db  survey_db response_db  MinIO   ai_evaluation
         (只读)   (response_db)          (response_db)
            └────────┬────────┘
                     ▼
              MySQL(:3307) / Redis / Nacos(:8848)
```

---

## 快速开始

### 环境要求

| 工具           | 版本   | 说明             |
| -------------- | ------ | ---------------- |
| JDK            | 21     | 后端编译与运行   |
| Maven          | 3.9+   | 依赖管理         |
| Node.js        | 20 LTS | 前端构建         |
| Docker Desktop | 最新   | 基础设施容器     |
| IDEA           | 2023+  | 后端开发（推荐） |

### 步骤一：克隆项目

```bash
git clone https://github.com/whoaddd/survey-system.git
cd survey-system
```

### 步骤二：配置环境变量

在项目根目录创建 `.env`：

```ini
MYSQL_ROOT_PASSWORD=Survey@2026
MYSQL_DATABASE=survey_db
REDIS_PASSWORD=Redis@2026
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=Minio@2026
NACOS_AUTH_IDENTITY_KEY=survey-nacos
NACOS_AUTH_IDENTITY_VALUE=survey-nacos-secret
```

### 步骤三：启动基础设施

```bash
docker compose up -d
docker compose ps
```

四个容器（mysql / redis / minio / nacos）应全部为 `healthy`。

| 服务         | 宿主机端口 | 访问地址                      |
| ------------ | ---------- | ----------------------------- |
| MySQL        | 3307       | `jdbc:mysql://localhost:3307` |
| Redis        | 6379       | `localhost:6379`              |
| MinIO        | 9000       | `http://localhost:9000`       |
| Nacos 控制台 | 8080       | `http://localhost:8080`       |

**验证**：

```bash
# MySQL
docker exec -it survey-mysql mysql -uroot -pSurvey@2026 -e "SHOW DATABASES;"

# Redis
docker exec -it survey-redis redis-cli -a Redis@2026 ping

# Nacos
# 浏览器打开 http://localhost:8080，用 nacos/nacos 登录
```

### 步骤四：初始化数据库

首次启动 MySQL 时，容器会自动执行 `deploy/mysql/init/` 下的 SQL 脚本。**如果数据库已存在旧数据，需要手动确认表结构**：

```bash
docker exec -it survey-mysql mysql -uroot -pSurvey@2026 -e "
  USE user_db; SHOW TABLES;
  USE survey_db; SHOW TABLES;
  USE response_db; SHOW TABLES;
"
```

**预期表清单**：

| 库            | 表                                                           |
| ------------- | ------------------------------------------------------------ |
| `user_db`     | `user`                                                       |
| `survey_db`   | `survey`、`question`、`question_option`                      |
| `response_db` | `response`、`response_answer`、`attachment_ref`、`ai_evaluation` |

**首次使用需要插入测试账号**：

```bash
# 在 IDEA 中运行以下代码生成 BCrypt 哈希
# new BCryptPasswordEncoder().encode("Publisher@2026")

docker exec -it survey-mysql mysql -uroot -pSurvey@2026 -e "
  USE user_db;
  INSERT INTO \`user\` (username, password_hash, role, status)
  VALUES ('publisher01', '\$2a\$10\$...粘贴生成的哈希...', 'PUBLISHER', 1);
"
```

或直接通过前端**注册页**创建账号（无需手动插数据）。

### 步骤五：启动后端服务

**在 IDEA 中依次启动 6 个 Spring Boot 服务**：

| 服务               | 端口 | 启动类                       |
| ------------------ | ---- | ---------------------------- |
| `user-service`     | 9001 | `UserServiceApplication`     |
| `survey-service`   | 9002 | `SurveyServiceApplication`   |
| `response-service` | 9003 | `ResponseServiceApplication` |
| `ai-orchestrator`  | 9004 | `AiOrchestratorApplication`  |
| `file-service`     | 9005 | `FileServiceApplication`     |
| `gateway`          | 8000 | `GatewayApplication`         |

**启动顺序**：先启动 5 个业务服务，最后启动 `gateway`（gateway 依赖其他服务的注册信息）。

**IDEA 运行配置建议**：

- 每个服务 VM options 加 `-Xmx256m`（6 个服务总堆约 1.5G，Windows 可承受）
- 用 `Services` 窗口（`Alt+8`）统一管理启动/停止

**验证**：

```bash
# Nacos 控制台 http://localhost:8080 → 服务列表 → 6 个服务在线
curl http://localhost:8000/actuator/health       # {"status":"UP"}
curl http://localhost:9001/actuator/health       # user-service
curl http://localhost:9002/actuator/health       # survey-service
```

### 步骤六：启动前端

**开发模式**（推荐开发调试时使用）：

```bash
cd frontend
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`。

**生产模式**（Docker 托管）：

```bash
docker compose build web
docker compose up -d web
```

浏览器打开 `http://localhost`。

### 步骤七：验证全链路

1. 打开 `http://localhost`，点击"注册"tab，创建账号
2. 登录后创建问卷，添加题目
3. 保存草稿 → 发布 → 复制分享链接
4. **无痕窗口**打开分享链接，答题提交
5. 发布者在结果页查看统计

---

## 项目结构

```
survey-system/
├── docker-compose.yml              # 容器编排
├── .env                            # 环境变量（不进 Git）
├── deploy/
│   └── mysql/init/
│       ├── 01-schema.sql           # 建库建表
│       └── 02-seed.sql             # 种子数据（可选）
├── backend/
│   ├── pom.xml                     # 父 POM，统一版本管理
│   ├── common/                     # 公共模块（ApiResponse/ErrorCode/异常）
│   ├── gateway/                    # API 网关
│   ├── user-service/               # 用户服务
│   ├── survey-service/             # 问卷服务
│   ├── response-service/           # 答卷服务
│   ├── file-service/               # 文件服务
│   └── ai-orchestrator/            # AI 编排服务
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── src/
│   │   ├── api/                    # 接口封装
│   │   ├── components/
│   │   │   ├── answer/             # 答卷端组件（QuestionRenderer）
│   │   │   ├── editor/             # 编辑器组件（QuestionEditor/RichEditor）
│   │   │   └── layout/             # AdminLayout
│   │   ├── router/
│   │   ├── stores/                 # Pinia（auth/surveyDraft）
│   │   ├── types/                  # TS 类型（与后端 DTO 对齐）
│   │   ├── utils/                  # request/upload/respondent
│   │   └── views/
│   │       ├── auth/               # 登录/注册
│   │       ├── admin/              # 列表/编辑器/预览/结果
│   │       └── public/             # 答题/完成
└── docs/
    └── 原理介绍.md
```

---

## 环境变量说明

| 变量                        | 默认值                | 说明                        |
| --------------------------- | --------------------- | --------------------------- |
| `MYSQL_ROOT_PASSWORD`       | `Survey@2026`         | MySQL root 密码             |
| `MYSQL_DATABASE`            | `survey_db`           | 主库名（其他库由 DDL 创建） |
| `REDIS_PASSWORD`            | `Redis@2026`          | Redis 密码                  |
| `MINIO_ROOT_USER`           | `minioadmin`          | MinIO 账号                  |
| `MINIO_ROOT_PASSWORD`       | `Minio@2026`          | MinIO 密码                  |
| `NACOS_AUTH_IDENTITY_KEY`   | `survey-nacos`        | Nacos 服务端标识            |
| `NACOS_AUTH_IDENTITY_VALUE` | `survey-nacos-secret` | Nacos 服务端密钥            |

**AI 相关**（`ai-orchestrator` 启动前配置）：

请替换自己的api和接口

| 变量          | 说明                                              |
| ------------- | ------------------------------------------------- |
| `AI_API_KEY`  | OpenAI 或兼容接口的 API Key                       |
| `AI_BASE_URL` | 默认 `https://api.openai.com`，可改为国内兼容接口 |

**IDEA 中配置方式**：`Run → Edit Configurations → Environment variables`。

---

## 端口清单

| 服务             | 端口         | 协议 |
| ---------------- | ------------ | ---- |
| **前端 Nginx**   | 80           | HTTP |
| **Gateway**      | 8000         | HTTP |
| user-service     | 9001         | HTTP |
| survey-service   | 9002         | HTTP |
| response-service | 9003         | HTTP |
| ai-orchestrator  | 9004         | HTTP |
| file-service     | 9005         | HTTP |
| MySQL            | 3307         | JDBC |
| Redis            | 6379         | RESP |
| MinIO API        | 9000         | S3   |
| MinIO 控制台     | 9000（旧版） | HTTP |
| Nacos Server     | 8848         | HTTP |
| Nacos gRPC       | 9848         | gRPC |
| Nacos 控制台     | 8080         | HTTP |
| Vite Dev         | 5173         | HTTP |

---

## 常用命令

```bash
# 启动全部基础设施
docker compose up -d

# 重启某个容器
docker compose restart mysql

# 查看容器日志
docker compose logs -f nacos

# 清空数据库并重建（⚠️ 会丢失数据）
docker compose down -v
docker compose up -d

# 后端全量编译
cd backend && mvn clean compile -DskipTests

# 前端类型检查
cd frontend && npx vue-tsc --noEmit

# 前端生产构建
cd frontend && npm run build

# Docker 重建前端
docker compose build web && docker compose up -d web
```

---

## 常见问题

### 1. Nacos 启动失败：`NACOS_AUTH_TOKEN must be set`

**原因**：Nacos 3.x 默认开启鉴权。

**解法**：`docker-compose.yml` 中设置 `NACOS_AUTH_ENABLE: "false"`（开发环境），或提供合法的 Base64 Token。

### 2. MinIO 镜像拉取失败

**原因**：MinIO 官方已从 Docker Hub 下线。

**解法**：使用 `rancher/minio-minio:RELEASE.2020-07-13` 或社区镜像 `pgsty/minio:latest`。

### 3. 服务启动报 `Missing request attribute 'userId'`

**原因**：JWT 拦截器未注册或被错误排除。

**解法**：检查 `WebConfig` 的 `excludePathPatterns`，**注册/登录接口要排除，其他接口不能排除**。

### 4. 公开接口 401

**原因**：Gateway 白名单或服务拦截器只有一层放行。

**解法**：**两层都要放行**——`AuthGlobalFilter.WHITE_LIST` 和 `WebConfig.excludePathPatterns`。

### 5. AI 评分超时 `timeout of 15000ms exceeded`

**原因**：前端 Axios 默认 15 秒超时，AI 调用通常 10–30 秒。

**解法**：`api/ai.ts` 中给 AI 接口单独设置 `timeout: 60000`。

### 6. AI 评分报 `Duplicate entry`

**原因**：`ai_evaluation` 有唯一索引，重复评分触发冲突。

**解法**：`AiEvaluationService` 改为幂等——**存在则 update，不存在则 insert**。

### 7. Spring AI 启动报 `NoClassDefFoundError: reactor/netty/...`

**原因**：Spring AI 2.x 与 Spring Boot 3.x 不兼容。

**解法**：**锁定 Spring AI 1.0.0**。

### 8. 图片 5 分钟后消失

**原因**：预签名 URL 5 分钟过期。

**解法**：前端 `refreshHtmlImages` 用 DOM 解析刷新图片 src（S4 已实现）。

---

## 致谢

本项目基于 Spring Cloud、Vue 3、Element Plus、Spring AI、MinIO 等开源项目构建。
