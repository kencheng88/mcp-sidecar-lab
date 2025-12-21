# MCP Sidecar Lab 🚀

這是一個先進的範例專案，展示如何將**業務微服務 (Business Microservice)** 與 **MCP (Model Context Protocol) Server Sidecar** 完美結合，並透過雲端原生架構部署在單一 Kubernetes Pod 中。

## 🌟 核心亮點 (Exciting Features)

這個專案不僅僅是簡單的通訊，它集成了多項前瞻性的技術特色：

*   **⚡️ Spring AI 1.1.2 GA 整合**：採用目前最新的穩定版 Spring AI，利用 auto-configuration 實現極簡的 Model Context Protocol 伺服器建置。
*   **🚀 GraalVM Native Image 支援**：支援將 Spring Boot 應用程式編譯為原生二進位檔。這使得 Sidecar 的記憶體占用下降約 **80% (約 80-100MB)**，且啟動時間縮短至 **0.1 秒**，達到與 Python/Go 相當的輕量化性能。
*   **🧩 動態資料庫驅動註冊 (DB-Driven Tool Registration)**：突破傳統硬編碼 (Hard-coded) 工具定義，實現從「模擬資料庫」動態載入工具清單。這意味著你可以在**不重新編譯 Java 程式碼**的情況下，動態更新 AI 的能力。
*   **🛠 通用執行器模式 (Generic Tool Executor)**：實作了一個強大的通訊模式，能自動將 AI 傳入的參數映射到後端微服務的 REST API 模板中。
*   **☁️ Sidecar 設計模式**：完美的職責分離 (SoC)。業務邏輯專注於業務，MCP 伺服器專注於 AI 通訊，兩者透過 localhost 極速對接。
*   **📦 Kubernetes 原生支援**：內含精心設計的多容器 Deployment 配置，包含健康檢查 (Health Checks) 與資源隔離設定。

## 📁 專案結構

- **/biz**: 主要業務微服務 (Spring Boot, Port 8080)
- **/mcp-server-sidecar**: MCP Server Sidecar (Spring Boot, Port 8081)
- **/k8s**: 用於部署的 Kubernetes Manifests
- **design_remote_registration.md**: 深入探討遠端註冊架構的技術文檔

## 🚀 部署策略

這兩個服務被部署在同一個 Pod 中，以便透過 `localhost` 進行極低延遲的通訊。
- `biz` 曝露於 Port 8080。
- `mcp-server-sidecar` 曝露於 Port 8081，採用 **SSE (Server-Sent Events) Transport**。

## 🛠 如何部署

1. 編譯 Docker 映像檔 (Images)：
   ```bash
   cd biz && docker build -t biz:latest .
   cd ../mcp-server-sidecar && docker build -t mcp-server-sidecar:latest .
   ```

2. 套用 Kubernetes 設定：
   ```bash
   kubectl apply -f k8s/deployment.yml
   kubectl apply -f k8s/service.yml
   ```
