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
- **Transport**: `Streamable HTTP`
- **MCP 端點**: `http://localhost:8081/mcp`
- **Server Type**: `ASYNC`

> [!NOTE]
> 本專案使用 Streamable HTTP 作為 MCP Transport，支援 HTTP POST/GET 請求並可選用 SSE 串流。

## 📖 API 文件 (OpenAPI)

本專案集成了 Swagger UI，方便開發者與程式查閱目前 Sidecar 曝露的工具。

*   **人機互動 Web 介面 (Swagger UI)**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
*   **程式讀取用 JSON 定義**: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

> [!TIP]
> Swagger UI 的首頁描述中會動態顯示目前已發現並映射的工具清單。

---

## 🏗 動態工具發現架構 (Dynamic Tool Discovery)

本專案實作了自動化的 **OpenAPI to MCP** 轉換器，能將任何 REST 服務轉化為 AI 工具。

### 1. 核心流程
1.  **OpenAPI 掃描**：啟動時，Sidecar 會從 `target.api.url` (如 `biz` 服務) 抓取 `v3/api-docs`。
2.  **語義映射 (Semantic Mapping)**：
    *   讀取 `src/main/resources/mcp-mapping.json`。
    *   **優先權 1**：如果映射檔有定義，則使用映射檔中的 `toolName`、描述與參數說明。
    *   **優先權 2**（退而求其次）：如果映射檔未定義，則嘗試抓取 OpenAPI 中的 `@Operation` 與 `@Parameter` 註解內容。
3.  **自動註冊**：利用 `DynamicToolRegistry` 將解析出的規格轉化為 Spring AI 標準的 `AsyncToolSpecification`。

### 2. 優勢
*   **零代碼維護**：當 Legacy 系統新增 API 時，Sidecar 只要重啟即可自動識別，無需撰寫 Java 代碼。
*   **AI 友好化**：透過 `mcp-mapping.json`，您可以將工程化的 API 名稱 (如 `getBizInfo`) 改為 AI 更易理解的語法 (如 `get_enterprise_info`)。

### 3. 圖片處理
*   支援後端 API 回傳的圖片（`image/*` Content-Type）
*   自動轉換為 Base64 編碼的 `ImageContent`
*   WebClient 設定 16MB buffer 以處理大型圖片

---

## 🧪 測試與驗證

### 1. 整合測試
```bash
# 執行 MCP Client 整合測試（需先啟動 biz 服務和 sidecar 服務）
mvn test -Dtest=McpClientIntegrationTest
```

### 2. 使用 MCP Inspector
```bash
npx @modelcontextprotocol/inspector
```
連接到 `http://localhost:8081/mcp` 後點選 **"List Tools"** 即可看到動態註冊的工具。

### 3. 目前已啟用的工具 (範例)
*   **`calculate_sum`**: 執行加法運算 (映射自 `/api/calculate`)。
*   **`get_enterprise_info`**: 取得企業等級資訊 (映射自 `/api/business-info`)。
*   **`get_manga_image`**: 取得漫畫圖片 (映射自 `/api/manga/image`)。

---

## 📁 專案結構

```
src/
├── main/java/com/example/mcpserversidecar/
│   ├── McpConfig.java           # CORS 配置
│   ├── AuthenticationFilter.java # 認證轉發 Filter
│   └── service/
│       ├── DynamicToolRegistry.java    # 動態工具註冊與 API 呼叫
│       └── OpenApiScannerService.java  # OpenAPI 掃描與映射
├── main/resources/
│   ├── application.properties   # Server 配置
│   └── mcp-mapping.json         # 工具語義映射
└── test/
    ├── java/.../McpClientIntegrationTest.java  # 整合測試
    └── resources/application.yml               # 測試配置

docs/
├── adr/                         # 架構決策記錄
│   └── 001-response-handling-strategy.md
└── tracking/                    # 問題追蹤記錄
    └── TRACK-001-image-buffer-limit.md
```

---

## 🚀 未來展望與生產環境強化 (Future Outlook)

為了將此 Sidecar 投入嚴格的生產環境，以下是計畫中與建議的技術強化方向：

- [x] **⚡️ WebFlux 反應式架構**：已完成。支援高併發與非阻塞通訊。
- [x] **� 圖片處理支援**：已完成。支援大型圖片的 Base64 編碼傳輸。
- [x] **�🛡 安全性增強 (Security)**
    - [x] **API 身份驗證轉發**：已完成。支援將 MCP Client 的 `Authorization` 標頭自動轉發至下游 API。
    - [ ] **動態 CORS 配置**：將目前的 `addAllowedOrigin("*")` 改為從環境變數注入。
    - [ ] **K8s NetworkPolicy**：在網路層級鎖定僅允許特定 Pod 連線。
    - [ ] **Service Mesh (Istio)**：利用 mTLS 與 AuthorizatonPolicy 實現加密通訊。
- [ ] **🚀 效能優化 (Performance)**
    - [ ] **URL 引用模式**：對於大型圖片，考慮改為回傳 URL 引用而非直接嵌入 Base64。
    - [ ] **Streaming Discovery**：優化巨型系統的 OpenAPI 掃描流程為全異步。
    - [ ] **連線池調優**：優化 `WebClient` 的 Connection Pool 配置。
- [ ] **📊 可觀測性 (Observability)**
    - [ ] **OpenTelemetry 整合**：追蹤 MCP 指令的全鏈路 Trace。
    - [ ] **Prometheus Metrics**：監控工具呼叫延遲、成功率與併發數。
