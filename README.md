# 智研平台 (Zhiyan Platform) - 高校科研团队协作与成果管理平台后端

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42.7.3-blue.svg)
![License](https://img.shields.io/badge/License-EPL%202.0-green.svg)

**致力于成为高校科研团队的首选数字化协作中枢**

[功能特性](#-核心功能) • [快速开始](#-快速开始) • [技术栈](#️-技术栈) • [项目结构](#-项目结构) • [文档](#-文档)

</div>

---

## 📖 项目简介

**智研平台**是一个专为高校科研团队设计的协作与成果管理平台，旨在通过技术手段提升科研创新效率，让知识的生产、沉淀与传承更加高效、有序。

### 项目愿景

成为高校科研团队的首选数字化协作中枢，通过一体化、规范化、智能化的解决方案，解决科研团队在协作、知识管理和成果沉淀方面的核心痛点。

### 核心价值

- **对于科研人员**：提升协作效率，在一个平台内完成沟通、任务管理、文档编写和文件共享
- **对于团队与实验室**：降低管理成本，保障知识传承，增强团队竞争力
- **技术前瞻性**：采用微服务架构，支持高可扩展性、技术灵活性和高可用性

---

## ✨ 核心功能

### 🔐 用户与权限服务
- **用户注册/登录**：支持邮箱验证码注册、多种登录方式、Remember Me 功能
- **第三方账号登录**：集成 OAuth2.0，支持 GitHub、Google 等主流社交账号快速登录
- **账号绑定管理**：支持多账号绑定与解绑
- **个人资料精细化管理**：
  - 研究方向标签体系（支持层级结构）
  - 学术成果关联
  - 个性化隐私设置
- **组织与院系管理**：支持多组织层级与用户多身份关联，适配高校行政架构
- **RBAC 权限系统**：基于角色的访问控制，精细化权限管理

### 📊 项目与团队管理服务
- **项目全生命周期管理**：创建、编辑、状态流转、归档/删除
- **任务管理与协作**：
  - 任务创建、分配、状态跟踪
  - 任务提交详情与审核流程
  - 可视化看板视图
- **项目进度可视化**：
  - 项目仪表盘（Dashboard）
  - 关键指标统计（任务完成率、成员贡献度等）
  - 里程碑时间线
  - 成果统计面板
- **精细化团队管理**：成员邀请、角色分配、权限控制
- **项目广场**：公开项目展示、成员申请与审批
- **用户操作日志**：关键操作记录、查询与导出

### 📚 数据与模型仓库服务
- **多类型成果管理**：支持论文、专利、数据集、模型文件、实验报告等
- **成果状态流转与评审**：起草中 → 评审中 → 已发布
- **统一文件存储与管理**：
  - 大文件断点续传
  - 批量上传/下载
  - 多类型文件预览（PDF、图片、Excel、.ipynb、.mat 等）
- **Wiki 协同编辑**：多人在线协同编辑、实时感知、版本控制
- **全局智能化搜索**：基于 Elasticsearch 的全文检索
- **AI 赋能助手**：
  - 论文模式与研究赋能模式
  - 基于知识库的 RAG 问答
  - 自动摘要与标签生成

### 🤖 AI 实验分析助手
- **RAG 功能深度扩展**：支持长回合对话中的文件上下文管理
- **实验数据解析**：自动解析 `.csv`、`.xlsx`、`.mat`、`.ipynb` 等数据文件
- **实验设计优化建议**：样本量合理性检查、变量控制、对照组设置等
- **实验报告生成**：基于实验数据自动生成结构化报告

### 📬 消息与通知服务
- **全平台业务场景通知**：任务相关、项目相关、成果相关、系统相关
- **站内信中心**：统一消息管理、筛选、搜索、批量操作
- **智能提醒机制**：分级提醒、未读提示

### 📈 我的活动（个人工作中心）
- **集中任务审核**：聚合所有项目中的待审核任务
- **个人工作仪表盘**：项目总览、任务总览、工作负载可视化
- **个人操作日志展示**：记录任务完成、成果提交、评论互动等

---

## 🛠️ 技术栈

### 核心框架
- **Java 21**：现代 Java 特性支持
- **Spring Boot 4.0.0**：微服务基础框架
- **Spring Security**：安全认证与授权
- **Spring Data JPA**：数据持久化（Hibernate 7.18）
- **MyBatis Plus 3.5.3.1**：增强的 MyBatis 框架

### 数据库与存储
- **PostgreSQL**：主数据库，使用 Schema 进行逻辑隔离
- **Redis**：缓存与会话存储
- **腾讯云 COS**：对象存储服务（兼容 S3 API）

### 搜索与AI
- **Elasticsearch**：全文搜索引擎
- **Dify AI**：AI 应用开发平台集成
- **Baidu AI SDK**：OCR 等功能

### 消息与通信
- **WebSocket**：实时通信
- **SSE (Server-Sent Events)**：服务器推送事件
- **Spring Mail**：邮件服务

### 工具库
- **Hutool**：Java 工具类库
- **Lombok**：减少样板代码
- **MapStruct**：Bean 映射
- **Knife4j**：API 文档（Swagger）
- **Jackson**：JSON 处理
- **Apache POI**：Office 文档处理
- **iText**：PDF 处理
- **ZXing**：二维码生成

### 其他
- **IP2Region**：IP 地址定位
- **Thumbnailator**：图片处理
- **Caffeine**：本地缓存
- **Java Diff Utils**：差异对比

---

## 📁 项目结构

```
zhiyan-backend/
├── src/main/java/hbnu/project/zhiyanbackend/
│   ├── activelog/          # 操作日志模块
│   │   ├── annotation/     # 日志注解
│   │   ├── aspect/         # AOP 切面
│   │   ├── controller/     # 日志控制器
│   │   ├── service/        # 日志服务
│   │   └── repository/     # 数据访问层
│   │
│   ├── ai/                 # AI 服务模块
│   │   ├── aiassistant/    # AI 助手
│   │   ├── aipowered/      # AI 赋能
│   │   └── model/          # 数据模型
│   │
│   ├── auth/               # 用户认证与权限模块
│   │   ├── controller/     # 认证控制器
│   │   ├── service/        # 认证服务
│   │   ├── oauth/          # OAuth2.0 实现
│   │   ├── repository/     # 数据访问层
│   │   └── model/          # 数据模型（DTO、Entity）
│   │
│   ├── knowledge/          # 知识库模块（成果管理）
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── model/
│   │
│   ├── message/            # 消息与通知模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── getui/          # 个推集成
│   │   └── unipush/        # UniPush 集成
│   │
│   ├── projects/           # 项目管理模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── model/
│   │
│   ├── tasks/              # 任务管理模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   └── model/
│   │
│   ├── wiki/               # Wiki 协同编辑模块
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── websocket/      # WebSocket 实时协作
│   │   └── model/
│   │
│   ├── oss/                # 对象存储模块
│   │   ├── controller/
│   │   ├── service/
│   │   └── utils/
│   │
│   ├── ocr/                # OCR 模块
│   │   ├── controller/
│   │   ├── service/
│   │   └── repository/
│   │
│   ├── sse/                # Server-Sent Events 模块
│   │   ├── controller/
│   │   ├── service/
│   │   └── core/
│   │
│   ├── security/           # 安全模块
│   │   ├── config/         # 安全配置
│   │   ├── filter/         # 过滤器
│   │   ├── interceptor/    # 拦截器
│   │   ├── encrypt/        # 加密工具
│   │   └── xss/            # XSS 防护
│   │
│   ├── basic/              # 基础模块
│   │   ├── config/         # 基础配置
│   │   ├── constants/      # 常量定义
│   │   ├── domain/         # 基础领域模型
│   │   ├── exception/      # 异常处理
│   │   ├── serializer/     # 序列化器
│   │   └── utils/          # 工具类
│   │
│   ├── redis/              # Redis 模块
│   │   ├── config/
│   │   ├── service/
│   │   └── utils/
│   │
│   └── ZhiyanBackendApplication.java  # 启动类
│
├── src/main/resources/
│   ├── application.yml              # 主配置文件
│   ├── application-cloud.yml        # 云服务器配置
│   ├── application-prod.yml         # 生产环境配置
│   └── META-INF/                    # 自动配置元数据
│
├── sql/                             # 数据库脚本
│   ├── 新PostgreSQL设计/           # PostgreSQL Schema 设计
│   │   ├── zhiyanauth.sql          # 认证模块 Schema
│   │   ├── zhiyanproject.sql       # 项目模块 Schema
│   │   ├── zhiyantasks.sql         # 任务模块 Schema
│   │   ├── zhiyanwiki.sql          # Wiki 模块 Schema
│   │   └── message_tables.sql      # 消息模块表
│   └── 原设计/                     # 旧版设计（MySQL/MongoDB）
│
├── docx/                            # 项目文档
│   ├── 项目设计/
│   │   ├── 产品设计文档.md          # 产品设计文档
│   │   └── 第二月版本产品设计文档.md # 第二版功能规划
│   └── workflow/                    # 工作流配置
│
├── bin/                             # 脚本文件
│   ├── BackendCICD.sh              # 后端 CI/CD 脚本
│   └── env_setup.sh                # 环境设置脚本
│
├── pom.xml                          # Maven 依赖配置
└── README.md                        # 项目说明文档
```

---

## 🚀 快速开始

### 环境要求

- **JDK 21+**
- **Maven 3.6+**
- **PostgreSQL 12+**
- **Redis 6+**
- **腾讯云 COS**（或兼容 S3 的对象存储）

### 克隆项目

```bash
git clone https://codeup.aliyun.com/6708806c1c80af57f902d17f/zhiyan_backend_NewStruct.git
cd zhiyan-backend
```

### 数据库初始化

1. 创建 PostgreSQL 数据库：
```sql
CREATE DATABASE zhiyanplatform;
```

2. 执行 Schema 初始化脚本（按顺序执行）：
```bash
# 执行 sql/新PostgreSQL设计/ 目录下的 SQL 脚本
psql -U postgres -d zhiyanplatform -f sql/新PostgreSQL设计/zhiyanauth.sql
psql -U postgres -d zhiyanplatform -f sql/新PostgreSQL设计/zhiyanproject.sql
psql -U postgres -d zhiyanplatform -f sql/新PostgreSQL设计/zhiyantasks.sql
psql -U postgres -d zhiyanplatform -f sql/新PostgreSQL设计/zhiyanwiki.sql
psql -U postgres -d zhiyanplatform -f sql/新PostgreSQL设计/message_tables.sql
```

### 配置应用

1. 复制配置文件并修改：
```bash
cp src/main/resources/application-cloud.yml src/main/resources/application-local.yml
```

2. 修改 `application-local.yml` 中的配置：
   - 数据库连接信息
   - Redis 连接信息
   - 邮件服务器配置
   - 对象存储配置
   - AI 服务配置

### 运行项目

```bash
# 编译项目
mvn clean package

# 运行项目
mvn spring-boot:run

# 或使用 jar 包运行
java -jar target/zhiyan-backend-0.0.1.jar
```

### 访问 API 文档

启动成功后，访问 Swagger 文档：
```
http://localhost:9006/doc.html
```

---

## 📝 配置说明

### 数据库配置

项目使用 PostgreSQL 的 Schema 进行逻辑隔离，主要 Schema 包括：
- `zhiyanauth`：认证与权限模块
- `zhiyanproject`：项目管理模块
- `zhiyantasks`：任务管理模块
- `zhiyanwiki`：Wiki 模块
- `zhiyanmessage`：消息模块

### 环境变量

主要配置项（生产环境请使用环境变量或配置中心）：
- `SPRING_DATASOURCE_URL`：数据库连接 URL
- `SPRING_DATASOURCE_USERNAME`：数据库用户名
- `SPRING_DATASOURCE_PASSWORD`：数据库密码
- `SPRING_DATA_REDIS_HOST`：Redis 主机地址
- `SPRING_DATA_REDIS_PASSWORD`：Redis 密码
- `TENCENT_COS_SECRET_ID`：腾讯云 COS Secret ID
- `TENCENT_COS_SECRET_KEY`：腾讯云 COS Secret Key

### 邮件配置

项目使用 163 邮箱作为邮件服务，配置在 `application-cloud.yml` 中。如需更换，修改 `spring.mail` 相关配置。

---

## 📚 文档

### 产品设计文档

- [产品设计文档](docx/项目设计/产品设计文档.md)：完整的产品功能设计、用户故事、技术选型说明
- [第二月版本产品设计文档](docx/项目设计/第二月版本产品设计文档.md)：第二版功能扩展规划

### 数据库设计

- [PostgreSQL Schema 设计](sql/新PostgreSQL设计/)：数据库表结构设计脚本

### API 文档

项目集成了 Knife4j (Swagger)，启动后可通过以下地址访问：
- Swagger UI: `http://localhost:9006/doc.html`
- API JSON: `http://localhost:9006/v3/api-docs`

---

## 🔧 开发指南

### 代码规范

- 遵循 Java 编码规范
- 使用 Lombok 减少样板代码
- 使用 MapStruct 进行 Bean 转换
- Service 层统一返回 `R<T>` 响应格式

### 模块说明

#### 认证模块 (`auth`)
- 用户注册、登录、密码重置
- JWT Token 生成与验证
- OAuth2.0 第三方登录
- RBAC 权限管理

#### 项目管理模块 (`projects`)
- 项目 CRUD 操作
- 成员管理
- 项目广场

#### 任务管理模块 (`tasks`)
- 任务创建、分配、状态管理
- 任务审核流程
- 任务详情提交

#### 知识库模块 (`knowledge`)
- 成果管理（论文、专利、数据集等）
- 文件上传、下载、预览
- 成果状态流转

#### Wiki 模块 (`wiki`)
- Markdown 文档编辑
- 多人在线协同编辑（WebSocket）
- 版本控制与差异对比

#### AI 模块 (`ai`)
- AI 助手问答（RAG）
- 实验数据分析
- 报告生成

#### 消息模块 (`message`)
- 站内信管理
- 通知推送
- 消息筛选与搜索

### 测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify
```

---

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 贡献规范

- 提交信息请使用中文，清晰描述改动内容
- 代码需通过编译和测试
- 新功能请补充相应的文档
- 遵循现有代码风格

---

## 📄 许可证

本项目采用 [Eclipse Public License 2.0](LICENSE) 许可证。

---

## 👥 团队

项目由高校科研团队开发，致力于提升科研协作效率。

---

## 🔗 相关链接

- **代码仓库**：[阿里云 Codeup](https://codeup.aliyun.com/6708806c1c80af57f902d17f/zhiyan_backend_NewStruct.git)
- **项目文档**：见 `docx/项目设计/` 目录
- **问题反馈**：请提交 Issue

---

## 📞 联系我们

如有任何问题或建议，欢迎通过以下方式联系：
- 提交 Issue
- 发送邮件

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**

Made with ❤️ by Zhiyan Platform Team

</div>
