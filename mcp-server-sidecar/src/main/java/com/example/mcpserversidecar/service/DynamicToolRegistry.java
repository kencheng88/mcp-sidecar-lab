package com.example.mcpserversidecar.service;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistry.class);

    private final WebClient webClient;
    private final OpenApiScannerService scannerService;

    public DynamicToolRegistry(WebClient.Builder webClientBuilder, OpenApiScannerService scannerService) {
        this.webClient = webClientBuilder.build();
        this.scannerService = scannerService;
    }

    /**
     * 提供動態工具規格。Spring AI MCP 會自動掃描 Bean 並註冊。
     */
    @Bean
    public List<AsyncToolSpecification> dynamicTools() {
        log.info("正在從 OpenAPI 掃描並準備動態工具規格...");
        List<OpenApiScannerService.ToolDefinition> tools = scannerService.scanAndMap();

        return tools.stream()
                .map(def -> AsyncToolSpecification.builder()
                        .tool(def.tool())
                        .callHandler((exchange, request) -> executeToolCall(def, (McpSchema.CallToolRequest) request))
                        .build())
                .collect(Collectors.toList());
    }

    private Mono<McpSchema.CallToolResult> executeToolCall(OpenApiScannerService.ToolDefinition def,
            McpSchema.CallToolRequest request) {
        String url = def.path().startsWith("/") ? "http://127.0.0.1:8080" + def.path() : def.path();
        log.info("執行工具呼叫: {} {}, 參數: {}", def.method(), url, request.arguments());

        if ("POST".equalsIgnoreCase(def.method())) {
            return webClient.post()
                    .uri(url)
                    .bodyValue(request.arguments())
                    .retrieve()
                    .bodyToMono(Object.class)
                    .map(response -> McpSchema.CallToolResult.builder()
                            .addTextContent(response.toString())
                            .build())
                    .onErrorResume(e -> Mono.just(McpSchema.CallToolResult.builder()
                            .addTextContent("Error: " + e.getMessage())
                            .isError(true)
                            .build()));
        } else {
            return Mono.defer(() -> {
                String targetUrl = url;
                if (request.arguments() != null) {
                    StringBuilder sb = new StringBuilder(url);
                    boolean first = !url.contains("?");
                    for (Map.Entry<String, Object> entry : request.arguments().entrySet()) {
                        String key = entry.getKey();
                        String val = entry.getValue().toString();
                        if (sb.toString().contains("{" + key + "}")) {
                            String newUrl = sb.toString().replace("{" + key + "}", val);
                            sb.setLength(0);
                            sb.append(newUrl);
                        } else {
                            sb.append(first ? "?" : "&").append(key).append("=").append(val);
                            first = false;
                        }
                    }
                    targetUrl = sb.toString();
                }
                return webClient.get()
                        .uri(targetUrl)
                        .retrieve()
                        .bodyToMono(Object.class)
                        .map(response -> McpSchema.CallToolResult.builder()
                                .addTextContent(response.toString())
                                .build())
                        .onErrorResume(e -> Mono.just(McpSchema.CallToolResult.builder()
                                .addTextContent("Error: " + e.getMessage())
                                .isError(true)
                                .build()));
            });
        }
    }
}
