# MCP Server Sidecar

這是作為 `biz` 微服務 Sidecar 運行的 Model Context Protocol (MCP) Server。

## 說明
這是一個作為 MCP Server 的 Spring Boot 應用程式。它與 `biz` 服務部署在同一個 Pod 中，以提供局部的上下文與能力。

## 設定
- **Port**: `8081` (已設定為避免與 `8080` 上的 `biz` 服務衝突)

## 如何編譯

### 1. 標準 JAR 編譯 (JVM 模式)
這是最穩定的模式，建議用於開發階段與快速除錯。
```bash
# 使用預設 Dockerfile
docker build -t mcp-server-sidecar:latest .
```

### 2. 原生編譯 (Native Image 模式)
針對生產環境優化，提供極低記憶體占用 (約 80Mi) 與極速啟動。
```bash
# 使用 Maven 直接編譯 (需安裝 GraalVM)
./mvnw -Pnative native:compile

# 使用 Docker 進行多階段原生編譯 (推薦，需使用特定 Dockerfile)
docker build -t mcp-server-sidecar:native -f Dockerfile.native .
```

## 本地執行
```bash
./mvnw spring-boot:run
```
服務將啟動於 `http://localhost:8081`。

## MCP 說明
- **Transport**: `SSE` (Server-Sent Events)
- **Endpoint**: `http://localhost:8081/mcp/sse`

---

## 🏗 動態註冊架構 (Dynamic Registration Architecture)

本專案採用了先進的 **資料庫驅動 (Database-Driven)** 工具註冊設計，而非傳統的靜態代碼定義。

### 1. 設計核心
*   **配置與邏輯分離**：工具的名稱、描述、JSON Schema 及目標 API URL 皆存儲於資料庫（目前為 `McpConfig` 中的 `SIMULATED_DB`）。
*   **通用執行器 (Generic Executor)**：實作了一個統一的 `ToolCallback` 處理器，能動態解析參數並轉發至對應的業務服務端點。
*   **執行流程**：
    1.  啟動時，`McpConfig` 掃描資料庫中的工具定義。
    2.  為每個定義實例化一個 `FunctionToolCallback`。
    3.  當 AI 呼叫工具時，通用執行器根據模板填入參數並完成 `RestTemplate` 請求。

### 2. 優勢
*   **零代碼更新 (Zero-code Updates)**：新增工具僅需更新資料庫記錄，無需重啟及重新編譯。
*   **描述動態化**：可以根據環境或需求，隨時調整給 AI 看的工具描述。

### 3. 目前註冊的動態工具 (模擬自資料庫)
*   **`getBusinessInfo`**: 取得業務等級資訊。
*   **`calculate`**: 執行加法運算。
*   **`checkHealth`**: 監控業務服務狀態。
