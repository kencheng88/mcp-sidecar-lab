package com.example.mcpserversidecar;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.example.mcpserversidecar.service.OpenApiScannerService;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * Swagger/OpenAPI 配置類
 * 使用標準 OpenAPI 結構來描述 MCP Streamable HTTP 端點及動態工具
 */
@Configuration
public class SwaggerConfig {

    private static final String TAG_MCP = "MCP Protocol";
    private static final String TAG_TOOLS = "MCP Tools";

    @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}")
    private String mcpEndpoint;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MCP Sidecar API")
                        .version("1.0.0")
                        .description("這是一個 MCP (Model Context Protocol) Sidecar 服務。\n\n"
                                + "它動態掃描外部 OpenAPI 定義並將其轉換為 MCP 工具。\n\n"
                                + "**協定：** Streamable HTTP\n\n"
                                + "**連接方式：** 發送 JSON-RPC 請求至 `/mcp` 端點")
                        .contact(new Contact()
                                .name("Developer")
                                .url("https://github.com/mcp-sidecar-lab")))
                .addTagsItem(new Tag().name(TAG_MCP).description("MCP 協定端點"))
                .addTagsItem(new Tag().name(TAG_TOOLS).description("動態掃描並轉換的 MCP 工具"));
    }

    /**
     * 自訂 OpenAPI，動態加入 MCP 端點和已掃描的工具
     */
    @Bean
    public OpenApiCustomizer mcpOpenApiCustomizer(OpenApiScannerService scannerService) {
        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                paths = new Paths();
                openApi.setPaths(paths);
            }

            // 1. 加入 MCP 協定端點
            paths.putAll(createMcpProtocolPaths());

            // 2. 動態加入每個 Tool 作為虛擬端點
            for (OpenApiScannerService.ToolDefinition def : scannerService.getCachedTools()) {
                paths.addPathItem("/mcp/tool/" + def.tool().name(), createToolPathItem(def));
            }
        };
    }

    /**
     * 建立 MCP 協定端點的 OpenAPI Path 定義
     */
    private Paths createMcpProtocolPaths() {
        Paths paths = new Paths();

        paths.addPathItem(mcpEndpoint, new PathItem()
                .post(new Operation()
                        .addTagsItem(TAG_MCP)
                        .summary("MCP Streamable HTTP 端點")
                        .description("透過此端點發送 MCP JSON-RPC 請求。\n\n"
                                + "**支援的 JSON-RPC 方法：**\n"
                                + "- `initialize`: 初始化 MCP 會話\n"
                                + "- `notifications/initialized`: 通知伺服器初始化完成\n"
                                + "- `tools/list`: 列出可用工具\n"
                                + "- `tools/call`: 呼叫指定工具")
                        .operationId("mcpStreamableHttp")
                        .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody()
                                .required(true)
                                .description("JSON-RPC 2.0 請求物件")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(new Schema<>()
                                                        .type("object")
                                                        .addProperty("jsonrpc",
                                                                new Schema<String>().type("string").example("2.0"))
                                                        .addProperty("id",
                                                                new Schema<Integer>().type("integer").example(1))
                                                        .addProperty("method",
                                                                new Schema<String>().type("string")
                                                                        .example("tools/list"))
                                                        .addProperty("params", new Schema<>().type("object"))))))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse().description("JSON-RPC 回應"))
                                .addApiResponse("400", new ApiResponse().description("無效的 JSON-RPC 請求")))));

        return paths;
    }

    /**
     * 為每個 MCP Tool 建立虛擬 OpenAPI Path 定義
     */
    @SuppressWarnings("unchecked")
    private PathItem createToolPathItem(OpenApiScannerService.ToolDefinition def) {
        McpSchema.Tool tool = def.tool();
        McpSchema.JsonSchema inputSchema = tool.inputSchema();

        // 建立 Request Body Schema
        Schema<?> requestSchema = new Schema<>().type("object");
        if (inputSchema != null && inputSchema.properties() != null) {
            Map<String, Object> props = (Map<String, Object>) inputSchema.properties();
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                Map<String, Object> propDef = (Map<String, Object>) entry.getValue();
                Schema<?> propSchema = new Schema<>()
                        .type((String) propDef.getOrDefault("type", "string"))
                        .description((String) propDef.get("description"));
                requestSchema.addProperty(entry.getKey(), propSchema);
            }
        }

        return new PathItem()
                .post(new Operation()
                        .addTagsItem(TAG_TOOLS)
                        .summary(tool.name())
                        .description(tool.description() + "\n\n"
                                + "> **注意：** 這是一個虛擬端點，用於文檔展示。\n"
                                + "> 實際呼叫請透過 `POST /mcp` 使用 `tools/call` 方法。\n\n"
                                + "**原始 API：** `" + def.method() + " " + def.path() + "`")
                        .operationId("tool_" + tool.name())
                        .requestBody(new io.swagger.v3.oas.models.parameters.RequestBody()
                                .required(true)
                                .description("工具參數")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()
                                                .schema(requestSchema))))
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("工具執行結果")
                                        .content(new Content()
                                                .addMediaType("application/json", new MediaType()
                                                        .schema(new Schema<>().type("object")))))));
    }
}
