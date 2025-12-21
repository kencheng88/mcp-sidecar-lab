# MCP Sidecar Lab

這是一個展示如何將業務微服務 (Business Microservice) 與 MCP (Model Context Protocol) Server Sidecar 同時部署在單一 Kubernetes Pod 中的範例專案。

## 專案結構
- **/biz**: 主要業務微服務 (Spring Boot, Port 8080)
- **/mcp-server-sidecar**: MCP Server Sidecar (Spring Boot, Port 8081)
- **/k8s**: 用於部署的 Kubernetes Manifests

## 部署策略
這兩個服務被部署在同一個 Pod 中，以便透過 `localhost` 進行極低延遲的通訊。
- `biz` 曝露於 Port 8080。
- `mcp-server-sidecar` 曝露於 Port 8081。

## 如何部署

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
