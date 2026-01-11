package com.example.mcpserversidecar.service;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import com.example.mcpserversidecar.AuthenticationFilter;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(DynamicToolRegistry.class);

    private final WebClient webClient;
    private final OpenApiScannerService scannerService;

    public DynamicToolRegistry(WebClient.Builder webClientBuilder, OpenApiScannerService scannerService) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024)) // 16MB
                .build();
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
        String url = def.path().startsWith("/") ? scannerService.getTargetApiUrl() + def.path() : def.path();
        log.info("執行工具呼叫: {} {}, 參數: {}", def.method(), url, request.arguments());

        if ("POST".equalsIgnoreCase(def.method())) {
            return Mono.deferContextual(ctx -> {
                String authHeader = ctx.getOrDefault(AuthenticationFilter.AUTH_TOKEN_KEY, null);
                var requestSpec = webClient.post().uri(url).bodyValue(request.arguments());
                if (authHeader != null) {
                    requestSpec.header("Authorization", authHeader);
                }
                return requestSpec
                        .exchangeToMono(response -> handleResponse(response));
            });
        } else {
            return Mono.deferContextual(ctx -> {
                String authHeader = ctx.getOrDefault(AuthenticationFilter.AUTH_TOKEN_KEY, null);
                String targetUrl = buildTargetUrl(url, request.arguments());

                var requestSpec = webClient.get().uri(targetUrl);
                if (authHeader != null) {
                    requestSpec.header("Authorization", authHeader);
                }

                return requestSpec
                        .exchangeToMono(response -> handleResponse(response));
            });
        }
    }

    /**
     * 處理回應，根據 Content-Type 決定如何處理
     */
    private Mono<McpSchema.CallToolResult> handleResponse(
            org.springframework.web.reactive.function.client.ClientResponse response) {

        MediaType contentType = response.headers().contentType().orElse(MediaType.APPLICATION_JSON);

        // 處理圖片類型
        if (contentType.getType().equals("image")) {
            return DataBufferUtils.join(response.bodyToFlux(DataBuffer.class))
                    .map(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        String mimeType = contentType.toString();

                        log.info("收到圖片回應: {} ({} bytes)", mimeType, bytes.length);

                        @SuppressWarnings("deprecation")
                        var imageContent = new McpSchema.ImageContent(null, null, base64, mimeType);
                        return McpSchema.CallToolResult.builder()
                                .addContent(imageContent)
                                .build();
                    })
                    .onErrorResume(e -> Mono.just(McpSchema.CallToolResult.builder()
                            .addTextContent("Error processing image: " + e.getMessage())
                            .isError(true)
                            .build()));
        }

        // 處理 JSON 或其他文字類型
        return response.bodyToMono(Object.class)
                .map(body -> McpSchema.CallToolResult.builder()
                        .addTextContent(body.toString())
                        .build())
                .onErrorResume(e -> Mono.just(McpSchema.CallToolResult.builder()
                        .addTextContent("Error: " + e.getMessage())
                        .isError(true)
                        .build()));
    }

    /**
     * 構建目標 URL，替換路徑參數並添加查詢參數
     */
    private String buildTargetUrl(String url, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        boolean first = !url.contains("?");

        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
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

        return sb.toString();
    }
}
