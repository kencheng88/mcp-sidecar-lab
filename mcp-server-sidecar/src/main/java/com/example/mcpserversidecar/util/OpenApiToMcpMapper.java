package com.example.mcpserversidecar.util;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OpenApiToMcpMapper {

    private static final Logger log = LoggerFactory.getLogger(OpenApiToMcpMapper.class);

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

        // 2. 處理 Request Body
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

    private static Map<String, Object> mapSchemaToMap(Schema<?> schema) {
        Map<String, Object> map = new HashMap<>();
        if (schema == null) {
            map.put("type", "string");
            return map;
        }

        String type = schema.getType();
        // OpenAPI "integer" -> JSON Schema "integer"
        // OpenAPI "number" -> JSON Schema "number"
        map.put("type", type != null ? type : "string");

        if (schema.getDescription() != null) {
            map.put("description", schema.getDescription());
        }

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
