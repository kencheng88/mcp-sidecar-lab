# MCP Server Sidecar

這是作為 `biz` 微服務 Sidecar 運行的 Model Context Protocol (MCP) Server。

## 說明
這是一個作為 MCP Server 的 Spring Boot 應用程式。它與 `biz` 服務部署在同一個 Pod 中，以提供局部的上下文與能力。

## 設定
- **Port**: `8081` (已設定為避免與 `8080` 上的 `biz` 服務衝突)

## 如何編譯
```bash
docker build -t mcp-server-sidecar:latest .
```

## 本地執行
```bash
./mvnw spring-boot:run
```
服務將啟動於 `http://localhost:8081`。

## MCP 說明
- **Transport**: `SSE` (Server-Sent Events)
- **Endpoint**: `http://localhost:8081/mcp/sse`

### 提供的工具 (Tools)
- `getBusinessInfo`: 透過轉發請求至 `biz` 服務取得業務資訊。
- `calculate`: 透過轉發請求至 `biz` 服務執行加法運算。
