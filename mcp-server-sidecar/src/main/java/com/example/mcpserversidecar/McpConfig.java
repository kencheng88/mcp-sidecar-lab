package com.example.mcpserversidecar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class McpConfig {

        /**
         * [僅限開發/測試] 寬鬆的 CORS 配置。
         * 為了讓 MCP Inspector (通常運行在 http://localhost:5173) 能夠跨網域存取 SSE 端點。
         * 在生產環境 (Production) 中，建議收緊 AllowedOrigins，或僅允許特定的 Gateway 存取。
         */
        @Bean
        public CorsWebFilter corsWebFilter() {
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(false);
                config.addAllowedOrigin("*");
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                config.addExposedHeader("*");
                config.setMaxAge(3600L); // 快取預檢請求
                source.registerCorsConfiguration("/**", config);
                return new CorsWebFilter(source);
        }
}
