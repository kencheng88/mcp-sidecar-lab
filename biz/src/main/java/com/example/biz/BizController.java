package com.example.biz;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class BizController {

    @Operation(summary = "Get business info", description = "Original description from Biz system")
    @GetMapping("/api/business-info")
    public Map<String, Object> getBusinessInfo(
            @Parameter(description = "The business level (original description)") @RequestParam(defaultValue = "standard") String level) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", "This is business information for level: " + level);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @Operation(summary = "Calculate sum", description = "Perform basic addition")
    @GetMapping("/api/calculate")
    public Map<String, Object> calculate(
            @Parameter(description = "First number (original)") @RequestParam int a,
            @Parameter(description = "Second number (original)") @RequestParam int b) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", a + b);
        response.put("operation", "addition");
        return response;
    }
}
