# ADR-001: 回應處理策略（圖片 vs JSON）

## 狀態
已採納 (Accepted)

## 日期
2026-01-11

## 背景

DynamicToolRegistry 使用 WebClient 向後端 API 發送請求並處理回應。後端可能回傳兩種類型的資料：
1. **JSON 資料** - 結構化資料（如企業資訊、計算結果）
2. **圖片二進位資料** - 需轉換為 Base64 傳給 MCP Client

WebClient 的 `maxInMemorySize` 預設為 256KB，對於較大的回應會拋出 `DataBufferLimitException`。

## 決策

採用**分流處理策略**，並將 WebClient buffer 設為 16MB：

### WebClient 配置
```java
this.webClient = webClientBuilder
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
        .build();
```

### 圖片處理
```java
DataBufferUtils.join(response.bodyToFlux(DataBuffer.class))
        .map(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            String base64 = Base64.getEncoder().encodeToString(bytes);
            // ...
        })
```

### JSON 處理
```java
response.bodyToMono(Object.class)
        .map(body -> body.toString())
```

## 為何選擇 16MB？

- 後端 Biz 專案的圖片每張超過 10MB
- Base64 編碼後約為原始大小的 133%
- 16MB 可涵蓋大多數圖片場景

## 考慮過的替代方案

### 替代方案 1：串流處理（reduce + ByteArrayOutputStream）
嘗試使用 `reduce()` 繞過 buffer 限制，但測試仍失敗。

### 替代方案 2：全域 WebClientCustomizer
嘗試使用全域 `WebClientCustomizer` Bean，但 MCP Transport 層可能未使用此配置。

### 結論
最簡單有效的方案是在 DynamicToolRegistry 中直接設定 16MB buffer 限制。

## 相關檔案
- `DynamicToolRegistry.java` - handleResponse() 方法
