package com.example.mcpserversidecar;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.mcpserversidecar.service.OpenApiScannerService;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Sidecar API")
                        .version("1.0.0")
                        .description("這是一個 MCP (Model Context Protocol) Sidecar 服務。它動態掃描外部 OpenAPI 定義並將其轉換為 MCP 工具。")
                        .contact(new Contact()
                                .name("Developer")
                                .url("https://github.com/mcp-sidecar-lab")));
    }

    @Bean
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer(OpenApiScannerService scannerService) {
        return openApi -> {
            String description = openApi.getInfo().getDescription();
            description += "\n\n### 目前已掃描並生成的 MCP 工具集：\n";

            if (scannerService.getCachedTools().isEmpty()) {
                description += "_目前尚未發現可用工具。_\n";
            } else {
                for (OpenApiScannerService.ToolDefinition def : scannerService.getCachedTools()) {
                    description += String.format("- **%s**: %s (由 `%s %s` 轉換)\n",
                            def.tool().name(),
                            def.tool().description(),
                            def.method(),
                            def.path());
                }
            }

            description += "\n\n### 如何連接到此 MCP Server？\n";
            description += "此服務採用 SSE (Server-Sent Events) 傳輸協議：\n";
            description += "1.  透過端點 `/sse` 建立長連接。\n";
            description += "2.  回應中會包含 `sessionId`，之後將 JSON-RPC 請求發送至 `/mcp/message?sessionId={id}`。\n";

            openApi.getInfo().description(description);
        };
    }
}
