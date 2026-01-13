# MCP 認證傳遞指南 (Authentication Forwarding Guide)

本文檔說明如何在 MCP (Model Context Protocol) 架構中正確傳遞認證資訊，特別是當 MCP Host 使用 OIDC 登入後，如何將 Access Token 傳遞給 MCP Server 並轉發到後端 API。

## 📋 目錄

- [認證傳遞流程](#認證傳遞流程)
- [HTTP Authorization Header](#http-authorization-header)
- [Sidecar 實作](#sidecar-實作)
- [MCP Host/Client 端實作](#mcp-hostclient-端實作)
- [完整架構圖](#完整架構圖)

---

## 認證傳遞流程

在 MCP 協定中，認證資訊的傳遞主要發生在 HTTP 層級。當使用 **Streamable HTTP** 協定時，每個 HTTP 請求都可以攜帶標準的認證 Header。

### 流程概述

1. **使用者登入**：透過 OIDC/OAuth2 取得 Access Token
2. **Token 儲存**：MCP Host 將 Token 儲存於 Cookie 或 Memory
3. **MCP 請求**：MCP Client 在 HTTP 請求中加入 `Authorization` Header
4. **Token 轉發**：MCP Server (Sidecar) 將 Token 轉發給後端 API

---

## HTTP Authorization Header

這是最標準且推薦的做法。MCP Client 在發送 HTTP 請求時，將 Access Token 放在 `Authorization` Header 中：

```http
POST /mcp HTTP/1.1
Host: localhost:8081
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "get_enterprise_info",
    "arguments": { "level": "premium" }
  }
}
```

### 為什麼選擇 Authorization Header

| 方式 | 優點 | 缺點 |
|------|------|------|
| **Authorization Header** | 標準、安全、各框架支援完整 | 需要 Client 主動設定 |
| Custom Header | 彈性高 | 非標準、可能被 Proxy 過濾 |
| Query String | 簡單 | 不安全、會被記錄在 Log |
| Request Body | 可與 JSON-RPC 整合 | 非 HTTP 標準做法 |

---

## Sidecar 實作

### AuthenticationFilter.java

使用 Servlet Filter 攔截每個請求，並透過 `ThreadLocal` 儲存認證資訊：

```java
@Component
public class AuthenticationFilter implements Filter {

    private static final ThreadLocal<String> authTokenHolder = new ThreadLocal<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null) {
                    authTokenHolder.set(authHeader);
                }
            }
            chain.doFilter(request, response);
        } finally {
            // 避免 Memory Leak
            authTokenHolder.remove();
        }
    }

    public static String getAuthToken() {
        return authTokenHolder.get();
    }
}
```

### DynamicToolRegistry.java

在呼叫後端 API 時讀取並轉發認證：

```java
private McpSchema.CallToolResult executeToolCall(...) {
    HttpHeaders headers = new HttpHeaders();
    
    // 讀取並轉發認證
    String authHeader = AuthenticationFilter.getAuthToken();
    if (authHeader != null) {
        headers.set("Authorization", authHeader);
    }
    
    // 發送請求到後端 API
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<byte[]> response = restTemplate.exchange(url, method, entity, byte[].class);
    
    return handleResponse(response);
}
```

---

## MCP Host/Client 端實作

### JavaScript/TypeScript 範例

```typescript
// 從 Cookie 取得 Access Token
function getAccessToken(): string | null {
  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=');
    if (name === 'access_token') {
      return value;
    }
  }
  return null;
}

// 建立 MCP Client 並設定認證
async function callMcpTool(toolName: string, args: object) {
  const accessToken = getAccessToken();
  
  const response = await fetch('http://localhost:8081/mcp', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { 'Authorization': `Bearer ${accessToken}` })
    },
    body: JSON.stringify({
      jsonrpc: '2.0',
      id: Date.now(),
      method: 'tools/call',
      params: { name: toolName, arguments: args }
    })
  });
  
  return response.json();
}
```

### Java Client 範例

```java
public class McpClient {
    private final WebClient webClient;
    
    public McpClient(String serverUrl, String accessToken) {
        this.webClient = WebClient.builder()
            .baseUrl(serverUrl)
            .defaultHeader("Authorization", "Bearer " + accessToken)
            .build();
    }
    
    public Mono<CallToolResult> callTool(String name, Map<String, Object> args) {
        return webClient.post()
            .uri("/mcp")
            .bodyValue(Map.of(
                "jsonrpc", "2.0",
                "id", System.currentTimeMillis(),
                "method", "tools/call",
                "params", Map.of("name", name, "arguments", args)
            ))
            .retrieve()
            .bodyToMono(CallToolResult.class);
    }
}
```

---

## 完整架構圖

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              MCP 認證傳遞流程                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Browser    │     │   MCP Host   │     │ MCP Sidecar  │     │   Backend    │
│   (User)     │     │   (Client)   │     │   (Server)   │     │     API      │
└──────┬───────┘     └──────┬───────┘     └──────┬───────┘     └──────┬───────┘
       │                    │                    │                    │
       │ ① OIDC Login       │                    │                    │
       │ ─────────────────> │                    │                    │
       │                    │                    │                    │
       │ ② Set Cookie       │                    │                    │
       │    (access_token)  │                    │                    │
       │ <───────────────── │                    │                    │
       │                    │                    │                    │
       │ ③ User Request     │                    │                    │
       │    (e.g., "查詢企業資訊")                │                    │
       │ ─────────────────> │                    │                    │
       │                    │                    │                    │
       │                    │ ④ POST /mcp        │                    │
       │                    │ ┌─────────────────┐│                    │
       │                    │ │Authorization:   ││                    │
       │                    │ │Bearer <token>   ││                    │
       │                    │ │                 ││                    │
       │                    │ │{                ││                    │
       │                    │ │ "method":       ││                    │
       │                    │ │ "tools/call",   ││                    │
       │                    │ │ "params": {...} ││                    │
       │                    │ │}                ││                    │
       │                    │ └─────────────────┘│                    │
       │                    │ ─────────────────> │                    │
       │                    │                    │                    │
       │                    │                    │ ⑤ AuthFilter       │
       │                    │                    │ extracts token     │
       │                    │                    │ ─────────────>     │
       │                    │                    │                    │
       │                    │                    │ ⑥ GET /api/biz    │
       │                    │                    │ Authorization:     │
       │                    │                    │ Bearer <token>     │
       │                    │                    │ ─────────────────> │
       │                    │                    │                    │
       │                    │                    │ ⑦ Validate Token  │
       │                    │                    │ <───────────────── │
       │                    │                    │                    │
       │                    │ ⑧ JSON-RPC Response│                    │
       │                    │ <───────────────── │                    │
       │                    │                    │                    │
       │ ⑨ Display Result   │                    │                    │
       │ <───────────────── │                    │                    │
       │                    │                    │                    │
```

---

## 安全性注意事項

> [!WARNING]
> **生產環境安全建議**

1. **HTTPS 強制**：所有 MCP 通訊必須透過 HTTPS
2. **Token 過期處理**：Client 應處理 401 回應並刷新 Token
3. **CORS 配置**：限制 `AllowedOrigins` 為特定網域
4. **Token 驗證**：Sidecar 可選擇性驗證 Token 有效性後再轉發

---

## 相關檔案

- [AuthenticationFilter.java](../mcp-server-sidecar-mvc/src/main/java/com/example/mcpserversidecar/AuthenticationFilter.java)
- [DynamicToolRegistry.java](../mcp-server-sidecar-mvc/src/main/java/com/example/mcpserversidecar/service/DynamicToolRegistry.java)
