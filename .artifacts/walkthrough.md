# 導覽 (Walkthrough) - MCP Sidecar Lab

我已經完成了 `biz` 和 `mcp-server-sidecar` 專案的 Docker 化與 Kubernetes 部署設定。

## 已完成的變更

### 1. 設定變更
- **[application.properties](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/mcp-server-sidecar/src/main/resources/application.properties)**：更新了 Sidecar 的 `server.port` 為 `8081`，以防止與同一 Pod 內 `8080` 上的 `biz` 服務發生 Port 衝突。

### 2. Docker 化
- 為 `biz` 微服務新增了 **[Dockerfile](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/biz/Dockerfile)**。
- 為 `mcp-server-sidecar` 服務新增了 **[Dockerfile](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/mcp-server-sidecar/Dockerfile)**。
- 兩者均使用多階段編譯 (Multi-stage Build)，確保映像檔更輕量且高效。

### 3. Kubernetes Manifests
- 建立了 **[deployment.yml](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/k8s/deployment.yml)**，特點如下：
    - 單一 Pod 部署，內含兩個容器 (Containers)。
    - 為兩個容器分別建立健康檢查 (Health Checks)。
    - Port 映射設定：8080 (biz) 與 8081 (sidecar)。
- 建立了 **[service.yml](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/k8s/service.yml)**，將 `biz` 服務曝露於 Port 80。

### 4. 說明文件
- 為 **[biz](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/biz/README.md)** 與 **[mcp-server-sidecar](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/mcp-server-sidecar/README.md)** 分別建立了專屬 README。
- 建立了根目錄的 **[README.md](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/README.md)** 以提供整體說明。

## 驗證細節

### Manifest 驗證
我已手動驗證 Kubernetes YAML 結構，確保：
- 映像檔名稱與標籤正確。
- Pod 內的容器 Port 無衝突。
- 正確的容器命名與健康檢查探針 (Liveness Probes)。

### 部署總結
- **Pod 名稱**：`mcp-sidecar-lab`
- **容器 1**：`biz` (Port 8080)
- **容器 2**：`mcp-server-sidecar` (Port 8081)
- **Service**：`biz-service` (Port 80 -> 8080)

### 5. Spring AI 版本升級 (1.1.2 GA)

已成功將所有 MCP 相關專案升級至目前最新的穩定版本：

- **版本號**：`1.1.2`
- **關鍵異動**：
    - Artifact ID 已由 `spring-ai-mcp-server-webmvc-spring-boot-starter` 變更為更加標準的 **`spring-ai-starter-mcp-server-webmvc`**。
    - 在 `pom.xml` 中明確指定 `${spring-ai.version}` 以確保依賴解析正確。
    - 經驗證，所有專案皆能在此版本下正常編譯並啟動 Auto-configuration。

### 6. 資料庫驅動的動態工具註冊 (Database-Driven Tool Registration)

為了展示高度動態化的 MCP Server 設計，我實作了模擬資料庫驅動的模式：

- **動態註冊邏輯**：在 `McpConfig.java` 中建立了一個 `SIMULATED_DB` (模擬工具定義表)。
- **通用執行器 (Generic Executor)**：實作了一個統一的 `FunctionToolCallback` 處理邏輯，它會自動解析工具定義中的 URL 模板，並將 AI 傳入的參數填入後透過 `RestTemplate` 呼叫後端 API。
- **配置與邏輯分離**：
    - [BizTools.java](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/mcp-server-sidecar/src/main/java/com/example/mcpserversidecar/BizTools.java)：原有的硬編碼工具邏輯已被註解保留作為參考。
    - [McpConfig.java](file:///Users/kencheng/Documents/lab/mcp_sidecar_lab/mcp-server-sidecar/src/main/java/com/example/mcpserversidecar/McpConfig.java)：現在負責根據「資料庫」內容動態產出工具清單。
- **優勢**：未來只需更新資料庫記錄，即可在不重新編譯 Java 程式碼的情況下，動態新增、刪除或修改 AI 工具的描述與功能。
