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

## 🏗 動態工具發現架構 (Dynamic Tool Discovery)

本專案實作了自動化的 **OpenAPI to MCP** 轉換器，能將任何 REST 服務轉化為 AI 工具。

### 1. 核心流程
1.  **OpenAPI 掃描**：啟動時，Sidecar 會從 `target.api.url` (如 `biz` 服務) 抓取 `v3/api-docs`。
2.  **語義映射 (Semantic Mapping)**：
    *   讀取 `src/main/resources/mcp-mapping.json`。
    *   **優先權 1**：如果映射檔有定義，則使用映射檔中的 `toolName`、描述與參數說明。
    *   **優先權 2**（退而求其次）：如果映射檔未定義，則嘗試抓取 OpenAPI 中的 `@Operation` 與 `@Parameter` 註解內容。
3.  **自動註冊**：利用 `DynamicToolRegistry` 將解析出的規格轉化為 Spring AI 標準的 `SyncToolSpecification`。

### 2. 優勢
*   **零代碼維護**：當 Legacy 系統新增 API 時，Sidecar 只要重啟即可自動識別，無需撰寫 Java 代碼。
*   **AI 友好化**：透過 `mcp-mapping.json`，您可以將工程化的 API 名稱 (如 `getBizInfo`) 改為 AI 更易理解的語法 (如 `get_enterprise_info`)。

## 🧪 測試與驗證

### 1. 使用 MCP Inspector
這是最推薦的測試方式：
```bash
npx @modelcontextprotocol/inspector --transport sse --server-url http://localhost:8081/mcp/sse
```
*   進入 `http://localhost:5173` 後點選 **"List Tools"** 即可看到動態註冊的工具。

### 2. 目前已啟用的工具 (範例)
*   **`calculate_sum`**: 執行加法運算 (映射自 `/api/calculate`)。
*   **`get_enterprise_info`**: 取得企業等級資訊 (映射自 `/api/business-info`)。

---

## 🛠 開發說明
*   **CORS 配置**：目前的專案配置了寬鬆的 `CorsFilter`，僅供開發與 MCP Inspector 測試使用。
*   **循環依賴**：`RestTemplate` 已定義於主類別中，避免與 `McpConfig` 產生啟動衝突。
