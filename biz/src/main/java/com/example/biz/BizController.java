package com.example.biz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class BizController {

    @GetMapping("/api/business-info")
    public Map<String, Object> getBusinessInfo(@RequestParam(defaultValue = "standard") String level) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", "This is business information for level: " + level);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/api/calculate")
    public Map<String, Object> calculate(@RequestParam int a, @RequestParam int b) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", a + b);
        response.put("operation", "addition");
        return response;
    }
}
