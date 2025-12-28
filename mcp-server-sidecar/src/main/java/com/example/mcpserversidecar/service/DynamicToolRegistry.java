package com.example.mcpserversidecar.service;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistry.class);

    @Value("${target.api.url}")
    private String targetApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OpenApiScannerService scannerService;

    /**
     * 提供動態工具規格。Spring AI MCP 會自動掃描 Bean 並註冊。
     */
    @Bean
    public List<SyncToolSpecification> dynamicTools() {
        log.info("正在從 OpenAPI 掃描並準備動態工具規格...");
        List<OpenApiScannerService.ToolDefinition> tools = scannerService.scanAndMap();

        return tools.stream().map(def -> SyncToolSpecification.builder()
                .tool(def.tool())
                .callHandler((exchange, request) -> executeToolCall(def, (McpSchema.CallToolRequest) request))
                .build()).collect(Collectors.toList());
    }

    private McpSchema.CallToolResult executeToolCall(OpenApiScannerService.ToolDefinition def,
            McpSchema.CallToolRequest request) {
        String url = targetApiUrl + def.path();
        log.info("執行工具代理請求: {} {}, 參數: {}", def.method(), url, request.arguments());

        try {
            Object response;
            if ("POST".equalsIgnoreCase(def.method())) {
                response = restTemplate.postForObject(url, request.arguments(), Object.class);
            } else {
                String targetUrl = url;
                if (request.arguments() != null) {
                    for (Map.Entry<String, Object> entry : request.arguments().entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue().toString();
                        if (targetUrl.contains("{" + key + "}")) {
                            targetUrl = targetUrl.replace("{" + key + "}", val);
                        } else {
                            targetUrl += (targetUrl.contains("?") ? "&" : "?") + key + "=" + val;
                        }
                    }
                }
                response = restTemplate.getForObject(targetUrl, Object.class);
            }

            return McpSchema.CallToolResult.builder()
                    .addTextContent(response != null ? response.toString() : "Success")
                    .build();
        } catch (Exception e) {
            log.error("代理請求失敗: {}", e.getMessage());
            return McpSchema.CallToolResult.builder()
                    .addTextContent("Error: " + e.getMessage())
                    .isError(true)
                    .build();
        }
    }
}
