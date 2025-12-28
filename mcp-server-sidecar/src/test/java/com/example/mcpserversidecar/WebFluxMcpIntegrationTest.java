package com.example.mcpserversidecar;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class WebFluxMcpIntegrationTest {

        @Test
        public void testMcpFlow() {
                WebClient client = WebClient.create("http://localhost:8081");
                AtomicReference<String> sessionUrlRef = new AtomicReference<>();

                // 1. Maintain SSE connection
                Flux<String> sseFlux = client.get()
                                .uri("/sse")
                                .retrieve()
                                .bodyToFlux(String.class)
                                .doOnNext(line -> {
                                        if (line.contains("sessionId=")) {
                                                String url = line.replace("data:", "").trim();
                                                sessionUrlRef.set(url);
                                        }
                                        System.out.println("SSE Received: " + line);
                                });

                sseFlux.subscribe();

                // Wait for session URL
                long start = System.currentTimeMillis();
                while (sessionUrlRef.get() == null && System.currentTimeMillis() - start < 5000) {
                        try {
                                Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                }

                String sessionUrl = sessionUrlRef.get();
                if (sessionUrl == null) {
                        throw new RuntimeException("Failed to get session URL from SSE");
                }

                // 2. Initialize
                Map<String, Object> init = Map.of(
                                "jsonrpc", "2.0",
                                "id", 1,
                                "method", "initialize",
                                "params", Map.of(
                                                "protocolVersion", "2024-11-05",
                                                "capabilities", Map.of(),
                                                "clientInfo", Map.of("name", "test", "version", "1")));

                client.post()
                                .uri(sessionUrl)
                                .bodyValue(init)
                                .retrieve()
                                .toBodilessEntity()
                                .block(Duration.ofSeconds(5));
                System.out.println("Initialize Sent.");

                // 3. List Tools
                Map<String, Object> listTools = Map.of(
                                "jsonrpc", "2.0",
                                "id", 2,
                                "method", "tools/list",
                                "params", Map.of());

                client.post()
                                .uri(sessionUrl)
                                .bodyValue(listTools)
                                .retrieve()
                                .toBodilessEntity()
                                .block(Duration.ofSeconds(5));
                System.out.println("List Tools Sent.");

                // 4. Call Tool (wait a bit for SSE to catch up)
                try {
                        Thread.sleep(1000);
                } catch (InterruptedException e) {
                }

                Map<String, Object> call = Map.of(
                                "jsonrpc", "2.0",
                                "id", 3,
                                "method", "tools/call",
                                "params", Map.of(
                                                "name", "test_tool",
                                                "arguments", Map.of()));

                client.post()
                                .uri(sessionUrl)
                                .bodyValue(call)
                                .retrieve()
                                .toBodilessEntity()
                                .block(Duration.ofSeconds(5));
                System.out.println("Tool Call Sent.");

                try {
                        Thread.sleep(2000);
                } catch (InterruptedException e) {
                }
        }
}
