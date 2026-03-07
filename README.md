<div align="center">
  <h1>🎓 Classroom Attendance System (课堂智能考勤系统)</h1>
  <p>基于计算机视觉的现代化、智能化课堂考勤解决方案</p>

  <p>
    <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java" />
    <img src="https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg" alt="Spring Boot" />
    <img src="https://img.shields.io/badge/Vue.js-3.5-blue.svg" alt="Vue" />
    <img src="https://img.shields.io/badge/Python-3.11-yellow.svg" alt="Python" />
    <img src="https://img.shields.io/badge/YOLO-Ultralytics-blueviolet.svg" alt="YOLO" />
  </p>
</div>

## 📖 项目简介
本项目是一个基于**人脸识别**的现代化课堂考勤系统，致力于解决传统人工点名耗时长、易代签等问题。项目采用了微服务架构思想，将业务逻辑与深度学习算法解耦，使得系统拥有更强的扩展性与稳定性。

系统通过摄像头捕获教室内的高清图像，利用最新的 AI 模型对学生进行精准的人脸检测和身份比对，最后生成直观的可视化考勤数据。

---

## 🛠️ 技术栈核心架构

系统划分为三大核心模块：前端交互、后端服务、算法分析引擎。

### 🎨 前端端 (Frontend)
提供教师、管理员、学生的可视化交互界面与数据看板。
- **核心框架**: Vue 3, Vite, TypeScript
- **UI 库**: Element Plus, Tailwind CSS
- **状态及路由**: Pinia, Vue Router
- **数据可视化**: ECharts
- **网络通信**: Axios

### ⚙️ 后端服务 (Backend)
负责核心业务逻辑处理、权限校验、数据持久化与微服务调度。
- **核心框架**: Spring Boot 3.5 (Java 21)
- **持久层架构**: MyBatis + MySQL + PageHelper
- **对象存储**: MinIO (图片、模型文件管理)
- **安全及工具**: JWT, Hutool

### 🧠 算法引擎 (Algorithm Service)
独立运行的人脸分析微服务，提供高性能的视觉计算能力。
- **Web 框架**: FastAPI (通过 Uvicorn 提供 HTTP 接口)
- **模型库**: Ultralytics (YOLO 人脸检测), InsightFace (人脸特征提取比对)
- **图像处理**: OpenCV, Numpy, SAHI (用于超大分辨率下的密集人脸切片推理)
- **运行环境**: 纯 Python 3.11 运行环境，支持 Docker 及 NVIDIA CUDA (12.1.0) 加速。

---

## 📂 目录结构

```text
classroom-attendance-system/
├── algorithm-service/       # 🧠 AI 算法服务
│   ├── app/                 # FastAPI 服务代码
│   ├── weights/             # 预训练模型 (YOLOv11 等)
│   ├── Dockerfile           # 算法服务容器化配置
│   ├── requirements.txt     # Python 依赖
│   └── test_*.py            # 本地调试与测试脚本
├── backend/                 # ⚙️ Java Spring Boot 后端
│   ├── src/                 # 后端源码目录
│   └── pom.xml              # Maven 依赖管理
├── frontend/                # 🎨 Vue3 前端项目
│   ├── src/                 # 前端源码目录
│   ├── package.json         # NPM/PNPM 依赖
│   ├── vite.config.ts       # Vite 构建配置
│   └── tailwind.config.ts   # Tailwind CSS 配置
└── README.md                # 项目文档
```

---

## ✨ 核心特性

- **📸 高精度人脸识别**：采用 YOLOv11 + InsightFace，结合 SAHI 算法，在密集人群和大尺寸教室照片中依然保持极高的检出率。
- **📊 实时数据看板**：基于 ECharts 提供考勤统计、缺勤趋势等可视化分析。
- **☁️ 分布式文件存储**：集成 MinIO，支持海量人脸底库和抓拍记录图片的高效存取。
- **🔒 安全与权限控制**：基于 JWT 提供无状态的用户会话与 API 访问控制。
- **🐳 容器化部署支持**：算法端已内建 Dockerfile 配置，一键构建配置好 CUDA 环境的服务容器。

---

## 🚀 快速开始

### 1. 算法服务启动 (Algorithm Service)
进入 `algorithm-service` 目录，准备 Python 环境并安装依赖：
```bash
cd algorithm-service
pip install -r requirements.txt
# 或者使用 docker 构建
docker build -t attendance-ai .
docker run -p 8000:8000 --gpus all attendance-ai
```
> *注：首次运行会自动下载相关识别模型到 weights 目录。*

### 2. 后端服务启动 (Backend)
安装 MySQL 和 MinIO，创建数据库，然后进入 `backend` 目录编译运行：
```bash
cd backend
mvn clean install
mvn spring-boot:run
```
> *注：请确保检查 `application.yml` 中的数据库和 MinIO 配置信息正确。*

### 3. 前端界面启动 (Frontend)
请确保已安装 Node.js 和 pnpm：
```bash
cd frontend
pnpm install
pnpm run dev
```
然后打开浏览器访问 `http://localhost:8888` 即可看到应用界面。

---

## 📄 许可证
本项目采用 [MIT License](./LICENSE) 开源协议。