# MCP Sidecar Lab 🚀

這是一個先進的範例專案，展示如何將**業務微服務 (Business Microservice)** 與 **MCP (Model Context Protocol) Server Sidecar** 完美結合，並透過雲端原生架構部署在單一 Kubernetes Pod 中。

## 🌟 專案特性 (Project Features)

這個專案不僅僅是簡單的通訊，它整合了多項技術：

*   **⚡️ Spring AI 1.1.2 GA 整合**：採用目前最新的穩定版 Spring AI，利用 auto-configuration 實現極簡的 Model Context Protocol 伺服器建置。
*   **🚀 GraalVM Native Image 支援**：支援將 Spring Boot 應用程式編譯為原生二進位檔。這使得 Sidecar 的記憶體占用下降約 **80% (約 80-100MB)**，且啟動時間縮短至 **0.1 秒**，達到與 Python/Go 相當的輕量化性能。
*   **🧩 動態 OpenAPI 掃描 (Dynamic OpenAPI Scanning)**：突破傳統硬編碼工具定義，Sidecar 能自動從遠端 Legacy 系統抓取 OpenAPI 定義並轉化為 MCP 工具。這實現了**零代碼 (Zero-code)** 的 AI 能力整合。
*   **🎭 語義映射層 (Semantic Mapping Layer)**：透過 `mcp-mapping.json`，在不變動任何原始程式碼的情況下，賦予 Legacy API 更具 AI 語義的名稱與描述。
*   **🛠 通用代理模式 (Generic Pure Proxy)**：Sidecar 作為純代理運行，具備透明的請求轉發能力，能完美對接到任何現有的 REST 系列微服務。
*   **⚡️ WebFlux 反應式架構 (Reactive Architecture)**：Sidecar 採用非阻塞 WebFlux 實作，支援高併發與串流通訊，確保在代理大量請求時依然維持極低延遲。
*   **☁️ Sidecar 設計模式**：完美的職責分離 (SoC)。業務邏輯專注於業務，MCP 伺服器專注於 AI 通訊，兩者透過 localhost 極速對接。
*   **📦 Kubernetes 原生支援**：內含精心設計的多容器 Deployment 配置，包含健康檢查 (Health Checks) 與資源隔離設定。

## 📁 專案結構

- **/biz**: 模擬 Legacy 業務系統 (Spring Boot, Port 8080)
- **/mcp-server-sidecar**: MCP 代理服務 (Spring Boot, Port 8081)
- **/k8s**: Kubernetes 部署清單 (YAML)
- **manual_test_guide.md**: 包含 MCP Inspector SSE 連線操作指南

## 🚀 部署策略

這兩個服務被部署在同一個 Pod 中，以便透過 `localhost` 進行極低延遲的通訊。
- `biz` 提供業務 API 與 OpenAPI 文檔。
- `mcp-server-sidecar` 定期掃描 `biz` 並將其能力曝露給 AI 客戶端。

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

## 🔮 未來展望 (Future Tasks)

- [x] **⚡️ WebFlux 反應式架構**：已完成。支援大規模並行流式通訊。
- [ ] **🛡 生產環境安全增強 (Production Security)**：整合 NetworkPolicy、API Key 與 Istio mTLS。
- [ ] **📊 全鏈路觀測 (Observability)**：整合 OpenTelemetry 與 Prometheus 監控。
- [ ] **🔍 語義化搜尋與動態載入 (Registry Mode)**：實現工具自動發現與 RAG 載入。
