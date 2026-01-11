package com.example.biz;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
public class BizController {

    private final ResourcePatternResolver resourceResolver;
    private final Random random = new Random();

    public BizController(ResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

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

    @Operation(summary = "Get random manga image", description = "Returns arandom comic-style image from local resources")
    @GetMapping(value = "/api/manga-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> getMangaImage() throws IOException {
        Resource[] resources = resourceResolver.getResources("classpath:pic/*.png");
        if (resources.length == 0) {
            return ResponseEntity.notFound().build();
        }
        Resource randomImage = resources[random.nextInt(resources.length)];
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(randomImage);
    }
}
