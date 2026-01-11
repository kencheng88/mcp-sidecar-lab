package com.example.mcpserversidecar;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * MCP Client 整合測試 (使用 Spring AI Auto-Configuration)
 * 
 * 此測試需要先手動啟動 mcp-server-sidecar 服務 (mvn spring-boot:run)
 * 以及後端 biz 服務
 * 
 * 執行方式: mvn test -Dtest=McpClientIntegrationTest
 */
@SpringBootTest
public class McpClientIntegrationTest {

    // 使用 List 注入，因為可能有多個 MCP 連接
    @Autowired
    private List<McpSyncClient> mcpClients;

    @Test
    public void testMcpClientConnection() {
        System.out.println("========================================");
        System.out.println("MCP Client 整合測試 (Spring AI Auto-Config)");
        System.out.println("========================================\n");

        if (mcpClients == null || mcpClients.isEmpty()) {
            System.out.println("⚠ McpSyncClient 未自動注入，嘗試手動建立連線...");
            testWithManualClient();
            return;
        }

        System.out.println("✓ 發現 " + mcpClients.size() + " 個 MCP Client\n");

        // 使用第一個 client 進行測試
        McpSyncClient mcpClient = mcpClients.get(0);

        try {
            // 1. 初始化連線
            System.out.println("[步驟 1] 初始化 MCP 連線...");
            McpSchema.InitializeResult initResult = mcpClient.initialize();
            System.out.println("✓ 初始化成功!");
            System.out.println("  - 伺服器名稱: " + initResult.serverInfo().name());
            System.out.println("  - 伺服器版本: " + initResult.serverInfo().version());
            System.out.println("  - 協定版本: " + initResult.protocolVersion());
            System.out.println();

            // 2. 列出可用工具
            System.out.println("[步驟 2] 列出可用工具...");
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools(null);
            List<McpSchema.Tool> tools = toolsResult.tools();
            System.out.println("✓ 發現 " + tools.size() + " 個工具:");
            for (McpSchema.Tool tool : tools) {
                System.out.println("  - " + tool.name() + ": " + tool.description());
            }
            System.out.println();

            // 3. 呼叫 calculate_sum 工具
            System.out.println("[步驟 3] 呼叫工具 'calculate_sum' (a=5, b=3)...");
            callToolAndPrintResult(mcpClient, "calculate_sum", Map.of("a", 5, "b", 3));

            // 4. 呼叫 get_enterprise_info 工具
            System.out.println("[步驟 4] 呼叫工具 'get_enterprise_info' (level=premium)...");
            callToolAndPrintResult(mcpClient, "get_enterprise_info", Map.of("level", "premium"));

            // 5. 呼叫 get_manga_image 工具
            System.out.println("[步驟 5] 呼叫工具 'get_manga_image'...");
            callToolAndPrintResult(mcpClient, "get_manga_image", Map.of());

            System.out.println("========================================");
            System.out.println("MCP Client 測試完成!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("✗ 錯誤: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\n[清理] 關閉 MCP Client...");
            mcpClient.close();
            System.out.println("✓ 連線已關閉");
        }
    }

    private void callToolAndPrintResult(McpSyncClient mcpClient, String toolName, Map<String, Object> args) {
        try {
            McpSchema.CallToolResult result = mcpClient.callTool(
                    new McpSchema.CallToolRequest(toolName, args));
            System.out.println("✓ 工具呼叫成功");
            for (McpSchema.Content content : result.content()) {
                if (content instanceof McpSchema.TextContent textContent) {
                    System.out.println("  回傳內容: " + textContent.text());
                } else if (content instanceof McpSchema.ImageContent imageContent) {
                    System.out.println("  圖片類型: " + imageContent.mimeType());
                    System.out.println(
                            "  資料長度: " + (imageContent.data() != null ? imageContent.data().length() : 0) + " bytes");
                }
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("✗ 工具呼叫失敗: " + e.getMessage());
            System.out.println();
        }
    }

    /**
     * 當 Spring Auto-Configuration 無法注入時，使用手動方式建立連線
     */
    private void testWithManualClient() {
        System.out.println("\n使用手動 HTTP 請求測試 MCP 端點...");

        org.springframework.web.reactive.function.client.WebClient webClient = org.springframework.web.reactive.function.client.WebClient
                .builder()
                .baseUrl("http://localhost:8081")
                .defaultHeader("Accept", "application/json, text/event-stream")
                .build();

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        try {
            // 發送 initialize 請求
            Map<String, Object> initRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "initialize",
                    "params", Map.of(
                            "protocolVersion", "2024-11-05",
                            "capabilities", Map.of(),
                            "clientInfo", Map.of("name", "test-client", "version", "1.0.0")));

            String requestJson = objectMapper.writeValueAsString(initRequest);
            System.out.println("[步驟 1] 發送 initialize: " + requestJson);

            String response = webClient.post()
                    .uri("/mcp")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("  回應: " + response);

            // 發送 tools/list
            Map<String, Object> toolsRequest = Map.of(
                    "jsonrpc", "2.0",
                    "id", 2,
                    "method", "tools/list",
                    "params", Map.of());

            requestJson = objectMapper.writeValueAsString(toolsRequest);
            System.out.println("\n[步驟 2] 發送 tools/list: " + requestJson);

            response = webClient.post()
                    .uri("/mcp")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(requestJson)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("  回應: " + response);

            // 呼叫 calculate_sum
            callToolManually(webClient, objectMapper, 3, "calculate_sum", Map.of("a", 5, "b", 3));

            // 呼叫 get_enterprise_info
            callToolManually(webClient, objectMapper, 4, "get_enterprise_info", Map.of("level", "premium"));

            // 呼叫 get_manga_image
            callToolManually(webClient, objectMapper, 5, "get_manga_image", Map.of());

            System.out.println("\n========================================");
            System.out.println("手動測試完成!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("✗ 錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void callToolManually(
            org.springframework.web.reactive.function.client.WebClient webClient,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            int id, String toolName, Map<String, Object> args) throws Exception {

        Map<String, Object> callRequest = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "method", "tools/call",
                "params", Map.of(
                        "name", toolName,
                        "arguments", args));

        String requestJson = objectMapper.writeValueAsString(callRequest);
        System.out.println("\n[步驟 " + id + "] 呼叫 " + toolName + ": " + requestJson);

        String response = webClient.post()
                .uri("/mcp")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        System.out.println("  回應: " + response);
    }
}
