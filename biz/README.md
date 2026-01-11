# Biz Microservice (Legacy System Simulation)

這是 MCP Sidecar Lab 中的核心業務系統範例。

## 🔹 功能特點
*   **業務介面**：提供基礎的業務邏輯 API，包含運算、資訊查詢與圖片服務。
*   **OpenAPI 整合**：預裝 `springdoc-openapi`，完整支援 Swagger UI 與 `/v3/api-docs`。
*   **語義註解**：程式碼中包含標準 OpenAPI `@Operation` 與 `@Parameter` 註解，可作為 MCP Sidecar 的預設語義來源。
*   **模擬環境**：在架構中扮演「不被修改的 Legacy 系統」，用於展示 Sidecar 如何在外部賦予其 MCP 能力。

## 📍 API 端點

| 端點 | 方法 | 說明 |
|-----|------|------|
| `/api/calculate` | GET | 執行加法運算，接受 `a` 和 `b` 兩個參數 |
| `/api/business-info` | GET | 取得企業等級資訊，接受 `level` 參數 |
| `/api/manga-image` | GET | 隨機回傳一張漫畫風格圖片 (PNG 格式) |

## 📂 專案結構

```
src/
├── main/
│   ├── java/com/example/biz/
│   │   └── BizController.java    # REST API 控制器
│   └── resources/
│       ├── application.properties
│       └── pic/                  # 漫畫圖片資源
│           └── *.png
└── test/
    └── java/com/example/biz/
        └── BizControllerTest.java
```

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

## 🔗 與 MCP Sidecar 的關係

此服務作為 MCP Sidecar 的後端 API 來源：

```
MCP Client ──► MCP Sidecar (8081) ──► Biz Service (8080)
                    │
                    └── 自動掃描 /v3/api-docs 並轉換為 MCP 工具
```

MCP Sidecar 會自動將以下 API 轉換為 MCP 工具：
- `/api/calculate` → `calculate_sum`
- `/api/business-info` → `get_enterprise_info`
- `/api/manga-image` → `get_manga_image`
