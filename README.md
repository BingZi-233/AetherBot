# AetherBot

AetherBot 是一个基于 Spring Boot 和 OpenAI 构建的智能 QQ 机器人，使用 Shiro 框架进行消息处理，提供 AI 聊天和智能助手功能。

## 功能特点

- **多模型 AI 聊天**：支持多种 OpenAI 模型，可根据需求选择不同模型进行对话
- **会话管理**：自动管理用户会话上下文，保持连贯对话体验
- **用户管理**：支持用户注册、CA代币充值和消费管理
- **权限控制**：管理员和普通用户权限分离，特定命令仅限管理员使用
- **命令系统**：丰富的命令集，支持聊天、查询、管理等多种操作
- **灵活配置**：支持环境变量配置，便于部署不同环境

## 命令列表

### CA币管理
- **@recharge**：充值CA代币（管理员命令）
- **@balance**：查询CA代币余额和交易记录

### 对话功能
- **@chat**：与AI对话，支持指定模型或使用默认模型
- **@setmodel**：设置对话模型，并将其设为默认模型
- **@defaultmodel**：查询当前默认AI模型
- **@end**：结束当前对话会话
- **@models**：查询系统中所有可用的AI模型
- **@search-model**：搜索匹配关键词的AI模型
- **@continuous-chat**：开启或关闭持续对话模式（仅私聊有效）
- **@history**：查询对话历史记录

### 系统功能
- **@help**：查看帮助信息
- **@addmodel**：添加新的AI模型（管理员命令）
- **@modelstatus**：更新模型状态（管理员命令）
- **@shutdown**：安全关闭系统（管理员命令）

## 技术栈

- **Spring Boot 3.4.5**：应用程序框架
- **Spring AI**：AI 集成框架
- **Shiro**：QQ 机器人框架
- **MySQL + JPA**：数据存储和访问
- **Lombok**：减少样板代码
- **Spring Dotenv**：环境变量管理

## 快速开始

### 环境要求
- JDK 21+
- MySQL 数据库
- OpenAI API 密钥

### 配置设置

1. 复制 `.env.example` 为 `.env`，并根据实际情况修改配置：

```properties
# 数据库配置
DATABASE_URL=jdbc:mysql://localhost:3306/aetherbot
DATABASE_USERNAME=root
DATABASE_PASSWORD=password

# OpenAI 配置
OPENAI_API_KEY=your_openai_api_key
OPENAI_BASE_URL=https://api.openai.com

# 其他配置
SERVER_PORT=8080
```

### 构建与运行

```bash
# 开发环境构建
./mvnw clean package

# 运行开发环境
java -jar target/AetherBot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 生产环境安全配置

在生产环境中，我们已经：

1. 禁用了Spring Boot Devtools，减少内存占用并提高安全性
2. 优化了连接池配置，适应生产环境高并发场景
3. 禁用了SQL语句日志显示，提高性能和安全性
4. 配置了日志系统，确保问题可追溯
5. 禁用了Hibernate自动更新数据库结构功能

## 贡献指南

欢迎提交 Pull Request 或 Issue 反馈问题。

## 许可证

本项目使用 [Apache-2.0 license] 许可证。 