package com.example.mcpserversidecar;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class McpConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // --- 模擬資料庫部分 (Simulated Database) ---

    /**
     * 工具定義實體類別
     */
    public record ToolRecord(
            String name,
            String description,
            String inputSchema,
            String targetUrl,
            String method // GET or POST
    ) {
    }

    /**
     * 模擬資料庫中的資料
     */
    private static final List<ToolRecord> SIMULATED_DB = List.of(
            new ToolRecord(
                    "getBusinessInfo",
                    "取得業務服務的資訊 (DB-driven: Get business info)",
                    """
                            {
                              "type": "object",
                              "properties": {
                                "level": { "type": "string", "description": "業務等級 (Business level)" }
                              }
                            }
                            """,
                    "http://localhost:8080/api/business-info?level={level}",
                    "GET"),
            new ToolRecord(
                    "calculate",
                    "進行加法運算 (DB-driven: Calculate a + b)",
                    """
                            {
                              "type": "object",
                              "properties": {
                                "a": { "type": "integer" },
                                "b": { "type": "integer" }
                              },
                              "required": ["a", "b"]
                            }
                            """,
                    "http://localhost:8080/api/calculate?a={a}&b={b}",
                    "GET"),
            new ToolRecord(
                    "checkHealth",
                    "檢查系統健康狀態 (DB-driven: Check health)",
                    "{\"type\": \"object\", \"properties\": {}}",
                    "http://localhost:8080/actuator/health",
                    "GET"));

    // --- 動態註冊部分 (Dynamic Registration) ---

    @Bean
    public List<ToolCallback> dynamicTools(RestTemplate restTemplate) {
        return SIMULATED_DB.stream()
                .map(record -> FunctionToolCallback.builder(record.name(), (Map<String, Object> args) -> {
                    // 通用執行器 (Generic Executor)
                    // 根據資料庫中的 URL 模板與參數進行呼叫
                    String resUrl = record.targetUrl();
                    if (args != null) {
                        for (Map.Entry<String, Object> entry : args.entrySet()) {
                            resUrl = resUrl.replace("{" + entry.getKey() + "}", entry.getValue().toString());
                        }
                    }
                    // 移除未填入的佔位符 (簡單處理)
                    resUrl = resUrl.replaceAll("\\{.*?\\}", "");

                    try {
                        return restTemplate.getForObject(resUrl, Map.class);
                    } catch (Exception e) {
                        return Map.of("error", "API 呼叫失敗", "details", e.getMessage());
                    }
                })
                        .description(record.description())
                        .inputType(Map.class)
                        .build())
                .collect(Collectors.toList());
    }

    /*
     * // [之前的 Programmatic 範例 - 已註解]
     * 
     * @Bean
     * public ToolCallback getBusinessInfoTool(BizTools bizTools) {
     * // ... 原本的反射實作 ...
     * }
     */
}
