# Blog 项目文档索引

## 📚 文档导航

这是一个完整的博客系统文档，包含前后端所有核心模块的详细分析。

### 🏗️ 架构概览
- [系统架构](architecture/README.md) - 项目整体架构、技术栈、数据流

### 🔧 后端文档 (Spring Boot)

#### 核心层
- [实体层 (Entity)](backend/entity/README.md) - JPA实体定义、关系映射、数据库设计
- [数据访问层 (Repository)](backend/repository/README.md) - Spring Data JPA、查询方法、性能优化
- [业务逻辑层 (Service)](backend/service/README.md) - 业务规则、事务管理、异常处理
- [控制器层 (Controller)](backend/controller/README.md) - REST API、参数验证、响应格式
- [安全认证系统 (Security)](backend/security/README.md) - JWT、Spring Security、权限控制

### 🎨 前端文档 (Vue 3)

#### 核心库
- [共享库 (@blog/shared)](frontend/shared/README.md) - API客户端、类型定义、工具函数、组合式API

#### 应用
- [前台博客 (@blog/web)](frontend/web/README.md) - 文章浏览、搜索、评论、点赞
- [后台管理 (@blog/admin)](frontend/admin/README.md) - CRUD操作、Markdown编辑、数据管理

## 🚀 快速开始

### 环境要求
- Java 21
- Maven 3.8+
- Node.js 18+
- npm 或 pnpm

### 启动服务

#### 1. 启动后端
```bash
cd backend
./mvnw spring-boot:run
# 端口：8080
```

#### 2. 启动前端（开发模式）
```bash
cd frontend
npm install  # 首次需要安装依赖

# 启动前台
npm run dev:web    # 端口：3000

# 启动后台
npm run dev:admin  # 端口：3001
```

#### 3. 一键启动
```bash
./scripts/start-all.sh
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 后端API | http://localhost:8080 | REST API |
| 前台博客 | http://localhost:3000 | 公开博客 |
| 后台管理 | http://localhost:3001/admin | 管理界面 |

### 默认账号
- 用户名：`admin`
- 密码：`admin123`

## 📖 文档说明

### 后端架构

```
Controller (HTTP请求)
    ↓
Service (业务逻辑)
    ↓
Repository (数据访问)
    ↓
Entity (数据库映射)
    ↓
Database (SQLite)
```

**关键特性**:
- ✅ JWT认证 + Spring Security
- ✅ 分层架构 + 依赖注入
- ✅ 事务管理 + 异常处理
- ✅ Flyway数据库迁移
- ✅ 统一响应格式

### 前端架构

```
组件 (Vue SFC)
    ↓
Pinia Store (状态管理)
    ↓
API Client (Axios + Token)
    ↓
Backend API (REST)
```

**关键特性**:
- ✅ Vue 3 + TypeScript
- ✅ Pinia状态管理
- ✅ 自动Token刷新
- ✅ 类型安全
- ✅ Monorepo共享库

## 🔑 核心概念

### 认证流程
1. 用户登录 → 生成Access Token (24h) + Refresh Token (7d)
2. Token存储 → localStorage
3. API请求 → 自动添加Bearer Token
4. Token过期 → 自动刷新 → 重试请求

### 权限控制
- **PUBLIC**: 无需认证（浏览文章）
- **USER**: 登录用户（评论、点赞）
- **ADMIN**: 管理员（所有管理功能）

### 数据流
```
用户操作 → 组件 → API调用 → 后端 → 数据库 → 响应 → UI更新
```

## 🛠️ 开发指南

### 添加新功能

#### 后端
1. 创建Entity → `src/main/java/com/example/blog/entity/`
2. 创建Repository → `src/main/java/com/example/blog/repository/`
3. 创建Service → `src/main/java/com/example/blog/service/`
4. 创建Controller → `src/main/java/com/example/blog/controller/`

#### 前端
1. 定义类型 → `frontend/packages/shared/src/types/`
2. 创建API → `frontend/packages/shared/src/api/`
3. 创建组件 → `frontend/packages/web/src/views/` 或 `admin/`
4. 更新路由 → `router/index.ts`

### 代码规范

#### Java
- 使用Lombok减少样板代码
- 统一使用`Result<T>`响应格式
- Service层添加`@Transactional`
- Repository使用JPA方法命名约定

#### TypeScript/Vue
- 使用`<script setup>`语法
- 明确类型定义
- 使用Pinia管理状态
- 组件名PascalCase

## 🧪 测试

### 后端测试
```bash
cd backend
./mvnw test
```

### 前端测试
```bash
cd frontend
npm run test  # 如果有测试脚本
```

## 📦 构建部署

### 开发环境
```bash
# 后端
./mvnw spring-boot:run

# 前端
npm run dev:web
npm run dev:admin
```

### 生产环境（单体部署）
```bash
# 1. 构建前端
cd frontend
npm run build

# 2. 复制到后端
cp -r packages/web/dist/* ../backend/src/main/resources/static/
cp -r packages/admin/dist/* ../backend/src/main/resources/static/admin/

# 3. 打包后端
cd ../backend
./mvnw package -DskipTests

# 4. 运行
java -jar target/blog-0.0.1-SNAPSHOT.jar
```

## 🔍 常见问题

### Q: 如何修改JWT密钥？
A: 在`application.yml`中修改`jwt.secret`，使用Base64编码的256位密钥

### Q: 如何添加新的管理员？
A: 在数据库中将User表的role字段设置为'ADMIN'

### Q: 前端跨域问题？
A: 开发环境已配置CORS，生产环境同源部署无需配置

### Q: 如何重置数据库？
A: 删除`blog.db`文件，重启应用会自动执行Flyway迁移

## 📞 支持

如有问题，请查看：
- [GitHub Issues](https://github.com/yourusername/blog/issues)
- [项目README](../README.md)

---

**文档版本**: v1.0
**最后更新**: 2026-01-04
**维护者**: Blog 开发团队