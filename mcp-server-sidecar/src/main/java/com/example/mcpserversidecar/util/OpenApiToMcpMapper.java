package com.example.mcpserversidecar.util;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OpenApiToMcpMapper {

    private static final Logger log = LoggerFactory.getLogger(OpenApiToMcpMapper.class);

    /**
     * 將 OpenAPI 的 Operation 轉換為 MCP 的 Input Schema (請求參數)
     */
    public static McpSchema.JsonSchema mapOperationToInputSchema(Operation operation) {
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        // 1. 處理 Path/Query Parameters
        if (operation.getParameters() != null) {
            for (Parameter p : operation.getParameters()) {
                Map<String, Object> paramSchema = mapSchemaToMap(p.getSchema());
                if (p.getDescription() != null) {
                    paramSchema.put("description", p.getDescription());
                }
                properties.put(p.getName(), paramSchema);
                if (Boolean.TRUE.equals(p.getRequired())) {
                    required.add(p.getName());
                }
            }
        }

        // 2. 處理 Request Body (主要針對 POST/PUT)
        if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
            Content content = operation.getRequestBody().getContent();
            if (content.containsKey("application/json")) {
                Schema<?> schema = content.get("application/json").getSchema();
                if (schema != null && schema.getProperties() != null) {
                    for (Map.Entry<String, Schema> entry : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
                        properties.put(entry.getKey(), mapSchemaToMap(entry.getValue()));
                    }
                    if (schema.getRequired() != null) {
                        required.addAll(schema.getRequired());
                    }
                }
            }
        }

        return new McpSchema.JsonSchema(
                "object",
                properties,
                required.isEmpty() ? null : required,
                true,
                null,
                null);
    }

    /**
     * 從 OpenAPI Operation 中提取成功回應 (200 OK) 的 Schema 描述
     */
    public static Map<String, Object> mapOperationToOutputSchema(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null)
            return null;

        // 優先找 200, 其次找 201
        ApiResponse successResponse = responses.get("200");
        if (successResponse == null) {
            successResponse = responses.get("201");
        }

        if (successResponse != null && successResponse.getContent() != null) {
            Content content = successResponse.getContent();
            if (content.containsKey("application/json")) {
                return mapSchemaToMap(content.get("application/json").getSchema());
            }
        }
        return null;
    }

    /**
     * 遞迴將 OpenAPI Schema 轉換為 Map 格式 (供裝飾 Tool Description 或構建 Input Schema 使用)
     */
    private static Map<String, Object> mapSchemaToMap(Schema<?> schema) {
        Map<String, Object> map = new HashMap<>();
        if (schema == null) {
            map.put("type", "string");
            return map;
        }

        String type = schema.getType();
        // OpenAPI 類型映射
        map.put("type", type != null ? type : "string");

        if (schema.getDescription() != null) {
            map.put("description", schema.getDescription());
        }

        // 處理 Array 類型 (遞迴轉換 items)
        if ("array".equals(type) && schema.getItems() != null) {
            map.put("items", mapSchemaToMap(schema.getItems()));
        }

        // 處理 Object 類型屬性
        if (schema.getProperties() != null) {
            Map<String, Object> props = new HashMap<>();
            for (Map.Entry<String, Schema> entry : ((Map<String, Schema>) schema.getProperties()).entrySet()) {
                props.put(entry.getKey(), mapSchemaToMap(entry.getValue()));
            }
            map.put("properties", props);
        }

        if (schema.getRequired() != null) {
            map.put("required", schema.getRequired());
        }

        return map;
    }
}
