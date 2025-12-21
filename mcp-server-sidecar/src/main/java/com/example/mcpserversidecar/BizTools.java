package com.example.mcpserversidecar;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class BizTools {

    private final RestTemplate restTemplate;
    private final String BIZ_SERVICE_URL = "http://localhost:8080";

    public BizTools(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /*
     * // [Legacy Declarative/Programmatic Tools]
     * // 這裡保留之前的實作邏輯作為參考。
     * // 在資料庫驅動模式下，這些方法將由 McpConfig 中的通用執行器 (Generic Executor) 統一處理。
     * 
     * public Map<String, Object> getBusinessInfo(String level) {
     * try {
     * String url = BIZ_SERVICE_URL + "/api/business-info?level=" + (level != null ?
     * level : "standard");
     * return restTemplate.getForObject(url, Map.class);
     * } catch (Exception e) {
     * Map<String, Object> error = new HashMap<>();
     * error.put("error", "無法連線至業務服務 (Cannot connect to biz service)");
     * error.put("details", e.getMessage());
     * return error;
     * }
     * }
     * 
     * @Tool(description = "使用業務服務進行加法運算 (Calculate a + b using the biz service)")
     * public Map<String, Object> calculate(int a, int b) {
     * try {
     * String url = BIZ_SERVICE_URL + "/api/calculate?a=" + a + "&b=" + b;
     * return restTemplate.getForObject(url, Map.class);
     * } catch (Exception e) {
     * Map<String, Object> error = new HashMap<>();
     * error.put("error", "計算失敗 (Calculation failed)");
     * error.put("details", e.getMessage());
     * return error;
     * }
     * }
     * 
     * @Tool(description = "檢查業務服務的健康狀態 (Check the health of the biz service)")
     * public Map<String, Object> checkHealth() {
     * try {
     * String url = BIZ_SERVICE_URL + "/actuator/health";
     * return restTemplate.getForObject(url, Map.class);
     * } catch (Exception e) {
     * Map<String, Object> status = new HashMap<>();
     * status.put("status", "BIZ_SERVICE_UNAVAILABLE");
     * status.put("message", "業務服務目前不可用 (Biz service is currently unavailable)");
     * return status;
     * }
     * }
     */
}
