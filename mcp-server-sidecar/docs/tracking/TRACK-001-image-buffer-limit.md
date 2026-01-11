# 問題追蹤：圖片處理 Buffer 限制

## 問題 ID
TRACK-001

## 日期
2026-01-11

## 問題描述

在處理大型圖片時，遇到多個 `DataBufferLimitException` 相關錯誤。

### 錯誤訊息歷程

1. **Buffer 溢出**
   ```
   writerIndex(385) + minWritableBytes(8192) exceeds maxCapacity(385)
   ```

2. **超過預設 256KB 限制**
   ```
   DataBufferLimitException: Exceeded limit on max bytes to buffer : 262144
   ```

## 根本原因

- WebClient 預設 `maxInMemorySize` 為 256KB
- 後端 Biz 專案的圖片每張超過 10MB
- Base64 編碼後更大（約 133%）

## 最終解決方案

在 `DynamicToolRegistry` 建構子中設定 WebClient buffer 為 16MB：

```java
public DynamicToolRegistry(WebClient.Builder webClientBuilder, OpenApiScannerService scannerService) {
    this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
            .build();
    this.scannerService = scannerService;
}
```

圖片處理使用 `DataBufferUtils.join()`：
```java
DataBufferUtils.join(response.bodyToFlux(DataBuffer.class))
        .map(dataBuffer -> { ... })
```

## 嘗試過但不採用的方案

| 方案 | 結果 | 原因 |
|-----|------|-----|
| `reduce(DataBuffer::write)` | ❌ 失敗 | 第一個 buffer 容量不足 |
| `reduce() + ByteArrayOutputStream` | ❌ 失敗 | 仍觸發 buffer 限制 |
| `collectList()` | ❌ 失敗 | 內部使用 LimitedDataBufferList |
| 全域 `WebClientCustomizer` | ❌ 失敗 | MCP Transport 可能未使用 |

## 測試結果

修正後測試通過：
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## 相關檔案
- `DynamicToolRegistry.java` - 建構子與 handleResponse() 方法
- `docs/adr/001-response-handling-strategy.md` - 架構決策記錄

## 經驗教訓

1. 簡單直接的解決方案往往最有效
2. 增加 buffer 限制比複雜的串流處理更可靠
3. 在嘗試複雜方案前，先確認基本配置是否足夠
