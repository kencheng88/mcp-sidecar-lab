# Biz Microservice

這是 MCP Sidecar Lab 的主要業務微服務。

## 說明
這是一個提供業務邏輯的 Spring Boot 應用程式。在 Kubernetes 部署中，此容器會與一個 MCP Sidecar 並行運行。

## 如何編譯
```bash
docker build -t biz:latest .
```

## 本地執行
```bash
./mvnw spring-boot:run
```
服務將啟動於 `http://localhost:8080`。
