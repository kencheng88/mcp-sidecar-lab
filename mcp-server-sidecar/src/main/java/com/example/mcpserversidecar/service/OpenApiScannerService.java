package com.example.mcpserversidecar.service;

import com.example.mcpserversidecar.util.OpenApiToMcpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.util.*;

@Service
public class OpenApiScannerService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiScannerService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${target.api.url}")
    private String targetApiUrl;

    private List<ToolDefinition> cachedTools = new ArrayList<>();

    public OpenApiScannerService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
            ResourceLoader resourceLoader) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public String getTargetApiUrl() {
        return targetApiUrl;
    }

    public List<ToolDefinition> getCachedTools() {
        return Collections.unmodifiableList(cachedTools);
    }

    public record ToolDefinition(
            McpSchema.Tool tool,
            String path,
            String method) {
    }

    public List<ToolDefinition> scanAndMap() {
        List<ToolDefinition> results = new ArrayList<>();
        try {
            // 1. 獲取 OpenAPI JSON (同步阻塞以進行啟動時註冊)
            String openApiUrl = targetApiUrl + "/v3/api-docs";
            log.info("正在從 {} 獲取 OpenAPI 定義...", openApiUrl);
            String openApiJson = webClient.get()
                    .uri(openApiUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openApiJson);
            OpenAPI openAPI = parseResult.getOpenAPI();
            log.info("OpenAPI 定義已獲取: {}", openAPI.getInfo().getTitle());

            // 2. 獲取 Mapping 配置
            Map<String, Map<String, Object>> mappings = loadMappings();
            log.info("Mapping 配置已獲取: {}", mappings.size());

            // 3. 遍歷 Paths 並轉換
            if (openAPI.getPaths() != null) {
                for (Map.Entry<String, PathItem> pathEntry : openAPI.getPaths().entrySet()) {
                    String path = pathEntry.getKey();
                    PathItem item = pathEntry.getValue();

                    item.readOperationsMap().forEach((method, operation) -> {
                        // 嚴格依照 mapping 定義來曝露工具
                        String opId = operation.getOperationId();
                        if (opId != null && mappings.containsKey(opId)) {
                            results.add(convertToMcpTool(path, method.name(), operation, mappings));
                            log.info("發現並對齊映射工具: {} -> {}", opId, mappings.get(opId).get("toolName"));
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.error("掃描 OpenAPI 失敗: {}", e.getMessage(), e);
        }
        this.cachedTools = results;
        log.debug("掃描 OpenAPI 完成，發現 {} 個工具", results.size());
        return results;
    }

    private ToolDefinition convertToMcpTool(String path, String method, Operation operation,
            Map<String, Map<String, Object>> mappings) {
        String operationId = operation.getOperationId();
        Map<String, Object> mapping = mappings.getOrDefault(operationId, Collections.emptyMap());

        String toolName = (String) mapping.getOrDefault("toolName", operationId);
        String description = (String) mapping.getOrDefault("description", operation.getSummary());

        McpSchema.JsonSchema inputSchema = OpenApiToMcpMapper.mapOperationToInputSchema(operation);

        // 提取 Output Schema 並附加到描述中，讓 LLM 了解返回格式
        Map<String, Object> outputSchema = OpenApiToMcpMapper.mapOperationToOutputSchema(operation);
        if (outputSchema != null) {
            String returnsInfo = "\nReturns: " + outputSchema.toString();
            if (description == null) {
                description = returnsInfo.trim();
            } else {
                description += returnsInfo;
            }
        }

        // 注入 Mapping 中的參數詳細描述或型別
        if (mapping.containsKey("parameters")) {
            Object parametersObj = mapping.get("parameters");
            Map<String, Object> properties = (Map<String, Object>) inputSchema.properties();

            if (properties != null) {
                if (parametersObj instanceof Map) {
                    Map<String, Object> paramMappings = (Map<String, Object>) parametersObj;
                    for (Map.Entry<String, Object> entry : paramMappings.entrySet()) {
                        String paramName = entry.getKey();
                        if (properties.containsKey(paramName)) {
                            Map<String, Object> fieldSchema = (Map<String, Object>) properties.get(paramName);
                            Object val = entry.getValue();

                            if (val instanceof String) {
                                // 舊版格式：直接是描述字串
                                fieldSchema.put("description", val);
                            } else if (val instanceof Map) {
                                // 新版格式：包含 type, description 等
                                Map<String, Object> valMap = (Map<String, Object>) val;
                                if (valMap.containsKey("description")) {
                                    fieldSchema.put("description", valMap.get("description"));
                                }
                                if (valMap.containsKey("type")) {
                                    fieldSchema.put("type", valMap.get("type"));
                                }
                            }
                        }
                    }
                }
            }
        }

        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(toolName)
                .description(description)
                .inputSchema(inputSchema)
                .build();

        return new ToolDefinition(tool, path, method);
    }

    private Map<String, Map<String, Object>> loadMappings() {
        Map<String, Map<String, Object>> result = new HashMap<>();
        try {
            Resource resource = resourceLoader.getResource("classpath:mcp-mapping.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    JsonNode root = objectMapper.readTree(is);
                    JsonNode mappings = root.get("mappings");
                    if (mappings != null && mappings.isArray()) {
                        for (JsonNode m : mappings) {
                            String opId = m.get("operationId").asText();
                            result.put(opId, objectMapper.convertValue(m, Map.class));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("無法載入 mcp-mapping.json: {}", e.getMessage());
        }
        return result;
    }
}
