package com.example.mcpserversidecar;

import com.example.mcpserversidecar.service.OpenApiScannerService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.Map;

@Configuration
public class McpConfig {

        /**
         * [僅限開發/測試] 寬鬆的 CORS 配置。
         * 為了讓 MCP Inspector (通常運行在 http://localhost:5173) 能夠跨網域存取 SSE 端點。
         * 在生產環境 (Production) 中，建議收緊 AllowedOrigins，或僅允許特定的 Gateway 存取。
         */
        @Bean
        public CorsFilter corsFilter() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(false);
                config.addAllowedOrigin("*");
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                config.addExposedHeader("*");
                config.setMaxAge(3600L); // 快取預檢請求
                source.registerCorsConfiguration("/**", config);
                return new CorsFilter(source);
        }
}
