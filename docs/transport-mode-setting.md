# Spring AI MCP Server：Streamable vs Stateless 模式
## 概述
在 Spring AI MCP Server 中，**Streamable** 和 **Stateless** 是兩種不同的運作模式，透過 `spring.ai.mcp.server.protocol` 設定來切換。
---
## 核心差異
| 項目 | Streamable (有會話) | Stateless (無會話) |
|------|---------------------|-------------------|
| **Protocol 設定** | `protocol: STREAMABLE` | `protocol: STATELESS` |
| **Session 管理** | ✅ 維護 Session | ❌ 不維護 |
| **持久連線** | ✅ Persistent connection | ❌ 每次獨立請求 |
| **Server 主動推送** | ✅ 支援 | ❌ 不支援 |
---
## 功能差異對比
| 功能 | Streamable | Stateless |
|------|------------|-----------|
| Tools | ✅ | ✅ |
| Resources | ✅ | ✅ |
| Prompts | ✅ | ✅ |
| Completion | ✅ | ✅ |
| Logging | ✅ | ❌ |
| Progress 進度通知 | ✅ | ❌ |
| Ping | ✅ | ❌ |
| Keep-Alive | ✅ | ❌ |
| 資源變更通知 | ✅ | ❌ |
| 工具變更通知 | ✅ | ❌ |
| Prompt 變更通知 | ✅ | ❌ |
| Root List Changes | ✅ | ❌ |
---
## 配置範例
### Streamable 模式（有會話管理）
```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        name: streamable-mcp-server
        version: 1.0.0
        type: SYNC
        resource-change-notification: true
        tool-change-notification: true
        prompt-change-notification: true
        streamable-http:
          mcp-endpoint: /api/mcp
          keep-alive-interval: 30s
```
### Stateless 模式（無會話管理）
```yaml
spring:
  ai:
    mcp:
      server:
        protocol: STATELESS
        name: stateless-mcp-server
        version: 1.0.0
        type: ASYNC
        streamable-http:
          mcp-endpoint: /api/mcp
```
---
## 架構圖解
### Streamable 模式
```
┌─────────────┐                              ┌─────────────┐
│  MCP Client │ ═══════ Session 建立 ══════► │  MCP Server │
│             │ ◄═══════ Session ID ════════ │             │
│             │                              │             │
│             │ ◄── 工具變更通知 ─────────── │  (主動推送) │
│             │ ◄── 資源變更通知 ─────────── │             │
│             │ ◄── 進度更新 ────────────── │             │
│             │                              │             │
│             │ ──────── Ping ──────────────►│             │
│             │ ◄─────── Pong ────────────── │             │
└─────────────┘                              └─────────────┘
        ↑
        └── 持久連線，Server 可以主動推送訊息給 Client
```
### Stateless 模式
```
┌─────────────┐                              ┌─────────────┐
│  MCP Client │ ── 請求 1 ──────────────────►│  MCP Server │
│             │ ◄─ 回應 1 ─────────────────── │             │
│             │                              │             │
│             │ ── 請求 2 (獨立) ───────────►│             │
│             │ ◄─ 回應 2 ─────────────────── │             │
│             │                              │             │
│             │    ❌ 無法接收主動推送        │             │
└─────────────┘                              └─────────────┘
        ↑
        └── 每次請求獨立，Server 無法主動通知 Client
```
---
## 依賴配置
兩種模式可以使用相同的 starter dependency，差別在於 protocol 設定：
### WebMVC
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webmvc</artifactId>
</dependency>
```
### WebFlux
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server-webflux</artifactId>
</dependency>
```
---
## 使用場景建議
| 場景 | 建議模式 | 原因 |
|------|----------|------|
| 簡單 API 代理 | Stateless | 不需要變更通知 |
| 雲端微服務部署 | Stateless | 易於水平擴展 |
| Kubernetes 部署 | Stateless | 無狀態更容易管理 |
| 動態工具/資源 | Streamable | 需要通知 Client 工具列表變化 |
| 長時間任務 | Streamable | 需要 Progress 進度回報 |
| 需要心跳檢測 | Streamable | Keep-Alive 功能 |
| 即時通知需求 | Streamable | Server 可主動推送 |
---
## Client 互動注意事項
### Streamable 模式
- 需要管理 Session ID
- 處理 Session 過期的情況
- 連線中斷時需重新建立 Session
- 可接收 Server 主動推送的通知
### Stateless 模式
- 每次請求需帶完整資訊
- 無法依賴之前的上下文
- Authorization Token 每次都要傳
- 更容易做負載均衡
---
## 總結
> **簡單記法：**
> - **STREAMABLE** = 全功能版，有會話管理，Server 可主動推送
> - **STATELESS** = 精簡版，無會話管理，適合無狀態微服務架構
---
## 參考資料
- [Streamable-HTTP MCP Servers](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-streamable-http-server-boot-starter-docs.html)
- [Stateless MCP Servers](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-stateless-server-boot-starter-docs.html)
- [MCP Specification - Streamable HTTP Transport](https://modelcontextprotocol.io/specification/2025-06-18/basic/transports#streamable-http)