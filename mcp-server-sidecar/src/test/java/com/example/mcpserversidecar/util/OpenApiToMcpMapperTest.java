package com.example.mcpserversidecar.util;

import io.modelcontextprotocol.spec.McpSchema;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiToMcpMapperTest {

    @Test
    void testMapOperationToInputSchema() {
        Operation operation = new Operation();

        // 模擬 Path Parameter
        PathParameter pathParam = new PathParameter();
        pathParam.setName("customerId");
        pathParam.setSchema(new StringSchema());
        pathParam.setDescription("The customer unique ID");
        pathParam.setRequired(true);
        operation.addParametersItem(pathParam);

        // 模擬 Request Body
        RequestBody body = new RequestBody();
        Content content = new Content();
        MediaType mediaType = new MediaType();
        ObjectSchema bodySchema = new ObjectSchema();
        bodySchema.addProperty("remark", new StringSchema().description("Optional remark"));
        mediaType.setSchema(bodySchema);
        content.addMediaType("application/json", mediaType);
        body.setContent(content);
        operation.setRequestBody(body);

        McpSchema.JsonSchema result = OpenApiToMcpMapper.mapOperationToInputSchema(operation);

        assertThat(result.type()).isEqualTo("object");
        Map<String, Object> properties = (Map<String, Object>) result.properties();

        // 驗證是否有包含 Path Param
        assertThat(properties).containsKey("customerId");
        Map<String, Object> idProps = (Map<String, Object>) properties.get("customerId");
        assertThat(idProps.get("description")).isEqualTo("The customer unique ID");

        // 驗證是否有包含 Body Param
        assertThat(properties).containsKey("remark");
        Map<String, Object> remarkProps = (Map<String, Object>) properties.get("remark");
        assertThat(remarkProps.get("description")).isEqualTo("Optional remark");

        // 驗證 Required
        assertThat(result.required()).contains("customerId");
    }
}
