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
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.util.*;

@Service
public class OpenApiScannerService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiScannerService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    @Value("${target.api.url}")
    private String targetApiUrl;

    public OpenApiScannerService(RestTemplate restTemplate, ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public record ToolDefinition(
            McpSchema.Tool tool,
            String path,
            String method) {
    }

    public List<ToolDefinition> scanAndMap() {
        List<ToolDefinition> results = new ArrayList<>();
        try {
            // 1. 獲取 OpenAPI JSON
            String openApiUrl = targetApiUrl + "/v3/api-docs";
            log.info("正在從 {} 獲取 OpenAPI 定義...", openApiUrl);
            String openApiJson = restTemplate.getForObject(openApiUrl, String.class);

            SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(openApiJson);
            OpenAPI openAPI = parseResult.getOpenAPI();

            // 2. 獲取 Mapping 配置
            Map<String, Map<String, Object>> mappings = loadMappings();

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
        return results;
    }

    private ToolDefinition convertToMcpTool(String path, String method, Operation operation,
            Map<String, Map<String, Object>> mappings) {
        String operationId = operation.getOperationId();
        Map<String, Object> mapping = mappings.getOrDefault(operationId, Collections.emptyMap());

        String toolName = (String) mapping.getOrDefault("toolName", operationId);
        String description = (String) mapping.getOrDefault("description", operation.getSummary());

        McpSchema.JsonSchema inputSchema = OpenApiToMcpMapper.mapOperationToInputSchema(operation);

        // 注入 Mapping 中的參數描述
        if (mapping.containsKey("parameters")) {
            Map<String, String> paramMappings = (Map<String, String>) mapping.get("parameters");
            Map<String, Object> properties = (Map<String, Object>) inputSchema.properties();
            if (properties != null) {
                for (Map.Entry<String, String> entry : paramMappings.entrySet()) {
                    if (properties.containsKey(entry.getKey())) {
                        Map<String, Object> fieldSchema = (Map<String, Object>) properties.get(entry.getKey());
                        fieldSchema.put("description", entry.getValue());
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
