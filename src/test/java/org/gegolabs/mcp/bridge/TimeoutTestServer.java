package org.gegolabs.mcp.bridge;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Simple test server to reproduce timeout issues
 */
@Slf4j
public class TimeoutTestServer {
    
    public static void main(String[] args) throws Exception {
        log.info("Starting timeout test server...");
        
        // Create a simple server with one tool
        var server = McpServer.async(McpBridge.tcpTransport(3000))
            .serverInfo(new McpSchema.Implementation("timeout-test", "1.0.0"))
            .tool(new McpSchema.Tool(
                "test_tool",
                "A simple test tool",
                "{\"type\":\"object\"}"
            ), (exchange, params) -> {
                log.info("Tool called with params: {}", params);
                return Mono.just(new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent("Test response")),
                    false
                ));
            })
            .build();
        
        log.info("Server built, waiting for connections...");
        
        // Keep running
        Thread.currentThread().join();
    }
}