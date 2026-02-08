# 技术架构设计文档 (Technical Design Document)

| **文档版本** | **V1.0**                             |
| ------------ | ------------------------------------ |
| **项目名称** | 基于人脸检测与识别的课堂快速点名系统 |
| **编写日期** | 2026-01-18                           |
| **适用阶段** | 系统开发与详细设计                   |

## 一、技术栈选择 (Technology Stack)

基于“前后端分离 + 算法微服务”架构，兼顾开发效率、性能与毕设答辩要求。

### 1.1 前端 (Web & Mobile H5)

- **核心框架**: **Vue 3** (使用 Composition API + `<script setup>` 语法糖)。
- **构建工具**: **Vite** (秒级启动，开发体验优于 Webpack)。
- **UI 组件库**:
  -  **Element Plus** (适配 PC 端，利用其栅格系统 `el-col` 实现移动端响应式布局)。
  -  **TailwindCSS** (原子化 CSS 工具库，快速实现自定义样式，补充 Element Plus 样式灵活性)。
- **网络请求**: **Axios** (封装拦截器，处理 JWT Token 和全局异常)。
- **数据可视化**: **ECharts** (用于展示出勤率、缺勤趋势等报表)。
- **关键工具库**:
  - `compressorjs`: **客户端图片压缩**，在上传前将大图压缩至 500KB 以内，解决校园网带宽瓶颈。
  - `vue-router`: 路由管理 (History 模式)。
  - `pinia`: 状态管理 (存储用户信息、全局 Loading 状态)。

### 1.2 后端 (Business Service)

- **开发语言**: **Java 21** (适配 Spring Boot 3.x)。
- **核心框架**: **Spring Boot 3.5.9**。
- **ORM 框架**: **MyBatis** (简化 CRUD 操作，提供分页插件)。
- **工具库**: **Hutool** (提供 AES 加密、文件操作、日期处理等工具类)。
- **鉴权安全**: JWT (JSON Web Token) + Interceptor (拦截器实现登录校验)。

### 1.3 算法服务 (Algorithm Microservice)

- **开发语言**: **Python 3.11**。
- **Web 框架**: **FastAPI** (高性能，原生支持异步，适合算法 API)。
- **计算机视觉**:
  - **YOLOv11** (`ultralytics`): 用于人脸检测 (Face Detection) 和人脸裁剪。
  - **InsightFace**: 用于人脸特征提取 (Feature Embedding, 512维)。
- **依赖库**: `numpy`, `opencv-python-headless`, `minio` (Python 客户端)。

### 1.4 数据存储与基础设施

- **关系型数据库**: **MySQL 8.0** (利用原生 **JSON** 类型存储人脸特征向量)。
- **对象存储**: **MinIO** (本地部署的 S3 兼容存储，用于存放原始合照、学生头像)。
- **部署环境**: **Docker** + **Docker Compose** (一键编排所有服务)。
- **网关/代理**: **Nginx** (反向代理，解决跨域，统一入口)。

## 二、项目结构 (Project Structure)

建议采用 **Monorepo (单体仓库)** 风格管理，根目录下包含不同模块的文件夹，方便毕设演示和文件管理。

以下仅为示例并非实际项目结构。

```
classroom-attendance-system/
├── docker-compose.yml          # [核心] 服务编排文件
├── nginx.conf                  # [核心] Nginx 反向代理配置
├── README.md                   # 项目说明
├── .gitignore
│
├── frontend/                   # [前端] Vue3 项目
│   ├── src/
│   │   ├── api/                # Axios 接口封装
│   │   ├── components/         # 公共组件 (如上传按钮)
│   │   ├── views/              # 页面文件 (Student/, Teacher/)
│   │   └── utils/              # 工具类 (compressor.js 封装)
│   ├── Dockerfile
│   └── vite.config.ts          # 代理配置
│
├── backend/                    # [后端] Spring Boot 项目
│   ├── src/main/java/com/grad/attendance/
│   │   ├── config/             # 配置类 (MyBatis, WebMvc)
│   │   ├── controller/         # 控制层
│   │   ├── entity/             # 实体类
│   │   ├── mapper/             # DAO 层
│   │   ├── service/            # 业务逻辑层 (含特征比对逻辑)
│   │   └── utils/              # 工具类 (AESUtil, MinIOUtil)
│   ├── Dockerfile
│   └── pom.xml
│
├── algo_service/               # [算法] Python 项目
│   ├── app.py                  # FastAPI 启动入口
│   ├── core/
│   │   ├── engine.py           # 封装 YOLO + InsightFace 调用
│   │   └── downloader.py       # MinIO 下载逻辑
│   ├── weights/                # 模型权重文件 (.pt, .onnx)
│   ├── requirements.txt
│   └── Dockerfile
│
└── data/                       # [运维] 挂载的数据目录 (MySQL, MinIO)
```

## 三、 数据模型 (Data Model)

```mysql
-- =======================================================
-- 1. 用户基础表 (Base User)
-- 作用：存储登录信息与公共属性
-- 变更：移除 Admin 角色，仅保留 Teacher 和 Student
-- =======================================================
CREATE TABLE `user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) NOT NULL COMMENT '账号(学号/工号)',
  `password` varchar(128) NOT NULL COMMENT '加密密码',
  `real_name` varchar(64) NOT NULL COMMENT '真实姓名',
  
  -- [修改点] 角色仅包含: 1-Teacher, 2-Student
  `role` tinyint NOT NULL DEFAULT 2 COMMENT '角色: 1-Teacher, 2-Student',
  
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像地址(MinIO)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础表';

-- =======================================================
-- 2. 学生信息扩展表 (Student Extension)
-- 作用：存储学生特有属性及敏感人脸数据 (1:1 关联 sys_user)
-- =======================================================
CREATE TABLE `student` (
  `user_id` bigint NOT NULL COMMENT '关联sys_user.user_id',
  `student_number` varchar(32) NOT NULL COMMENT '学号',
  `admin_class` varchar(64) NOT NULL COMMENT '行政班级(如: 计科225)',
  `gender` tinyint DEFAULT NULL COMMENT '性别: 1-男, 2-女',
  
  -- [核心设计点] 使用 TEXT 存储 AES 加密后的 Base64 字符串
  `feature_vector` text COMMENT '人脸特征值(AES加密密文)',
  
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- =======================================================
-- 3. 教师信息扩展表 (Teacher Extension)
-- 作用：存储教师特有属性 (1:1 关联 sys_user)
-- 变更：移除 department 字段
-- =======================================================
CREATE TABLE `teacher` (
  `user_id` bigint NOT NULL COMMENT '关联sys_user.user_id',
  `job_number` varchar(32) NOT NULL COMMENT '工号',
  
  -- [修改点] 已移除 department (所属院系) 字段
  
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师信息表';

-- =======================================================
-- 4. 课程/教学班表 (Course/Section)
-- 作用：教师创建的课堂容器，采用私有制 (属于单个教师)
-- =======================================================
CREATE TABLE `course` (
  `course_id` bigint NOT NULL AUTO_INCREMENT,
  `course_name` varchar(128) NOT NULL COMMENT '课程名称',
  `teacher_id` bigint NOT NULL COMMENT '所属教师ID',
  `semester` varchar(32) DEFAULT NULL COMMENT '学期(如: 2025-2026-1)',
  `description` varchar(512) DEFAULT NULL COMMENT '课程描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`course_id`),
  KEY `idx_teacher` (`teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- =======================================================
-- 5. 课程-学生关联表 (Course Enrollment)
-- 作用：选课名单/花名册，通过 Excel 导入生成
-- =======================================================
CREATE TABLE `course_student` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `join_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_student` (`course_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程学生关联表';

-- =======================================================
-- 6. 考勤会话表 (Attendance Session)
-- 作用：一次具体的点名任务（原子性），包含多张照片
-- =======================================================
CREATE TABLE `attendance_session` (
  `session_id` bigint NOT NULL AUTO_INCREMENT,
  `course_id` bigint NOT NULL COMMENT '所属课程',
  `source_images` json NOT NULL COMMENT '原始合照URL列表(JSON Array)',
  `total_student` int NOT NULL DEFAULT 0 COMMENT '应到人数',
  `actual_student` int NOT NULL DEFAULT 0 COMMENT '实到人数',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '点名时间',
  PRIMARY KEY (`session_id`),
  KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤会话表';

-- =======================================================
-- 7. 考勤明细记录表 (Attendance Record)
-- 作用：全量快照模式，存储每位学生的出勤状态与算法元数据
-- =======================================================
CREATE TABLE `attendance_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL COMMENT '关联会话',
  `student_id` bigint NOT NULL COMMENT '关联学生',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '考勤状态: 0-缺勤, 1-已到, 2-迟到, 3-请假',
  
  -- 算法元数据
  `similarity_score` decimal(5,4) DEFAULT NULL COMMENT '识别相似度(如 0.8521)',
  `face_location` varchar(64) DEFAULT NULL COMMENT '人脸坐标(如 [x,y,w,h])',
  `update_type` tinyint DEFAULT 1 COMMENT '修改类型: 1-算法自动, 2-人工修正',
  
  PRIMARY KEY (`record_id`),
  UNIQUE KEY `uk_session_student` (`session_id`, `student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤明细表';
```



## 四、关键技术点与难点 (Key Technical Points)

### 4.1 隐私安全：特征向量加密存储

- **问题**：人脸特征属于生物敏感信息，不能明文存储。
- **方案**：
  1. Python 提取特征返回明文 `List<double>`。
  2. Java 端在存入 MySQL 前，将 List 序列化为 JSON 字符串。
  3. 使用 **Hutool 的 AES 工具类** 对字符串进行加密，存入 `feature_vector` 字段。
  4. 比对时，Java 读取密文 -> 解密 -> 还原为 List -> 计算余弦相似度。

### 4.2 性能优化：大图传输与处理

- **前端**：使用 `compressorjs` 在浏览器端将图片压缩至 1600px 宽度（约 300KB），避免上传 10MB+ 原图导致 Nginx 413 错误或超时。
- **后端**：采用 **MinIO URL 传递方案**。
  - Java 接收前端图片 -> 上传至 MinIO -> 获取图片 URL。
  - Java 仅将 **URL** 发送给 Python 算法服务。
  - Python 根据 URL 自行下载图片进行推理。
  - *优势*：减少 Java 与 Python 之间的大文件 IO 传输。

### 4.3 架构解耦：Java 主导比对

- **逻辑**：
  - **Python (Worker)**：只负责“看”。输入图片，输出 N 个人脸的坐标和特征向量。**不连接数据库**。
  - **Java (Manager)**：负责“认”。获取 Python 的结果，从 MySQL 拉取全班学生的特征库，在 Java 内存中进行 $O(M \times N)$ 的余弦相似度计算。
- **优势**：算法服务无状态，易于扩展；业务逻辑集中在 Java，符合毕设技术栈要求。

### 4.4 移动端适配：响应式设计

- **方案**：不单独开发移动端页面。利用 Element Plus 的 Grid 布局。
- **交互**：在移动端 H5 页面使用 `<input type="file" capture="environment">` 唤起后置摄像头，实现类似原生 App 的拍照体验。