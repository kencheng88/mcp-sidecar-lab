# Biz Microservice (Legacy System Simulation)

這是 MCP Sidecar Lab 中的核心業務系統範例。

## 🔹 功能特點
*   **業務介面**：提供基礎的業務邏輯 API，如 `/api/calculate` (運算) 與 `/api/business-info` (資訊查詢)。
*   **OpenAPI 整合**：預裝 `springdoc-openapi`，完整支援 Swagger UI 與 `/v3/api-docs`。
*   **語義註解**：程式碼中包含標準 OpenAPI `@Operation` 與 `@Parameter` 註解，可作為 MCP Sidecar 的預設語義來源。
*   **模擬環境**：在架構中扮演「不被修改的 Legacy 系統」，用於展示 Sidecar 如何在外部賦予其 MCP 能力。

## 📍 存取點
*   **服務地址**：`http://localhost:8080`
*   **Swagger UI**：`http://localhost:8080/swagger-ui/index.html`
*   **OpenAPI 定義**：`http://localhost:8080/v3/api-docs`

## 🚀 如何編譯與執行

### 標準 JVM 執行
```bash
mvn spring-boot:run
```

### Docker 建構
```bash
docker build -t biz:latest .
```
