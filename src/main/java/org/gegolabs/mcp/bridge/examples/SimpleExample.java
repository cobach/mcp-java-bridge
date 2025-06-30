package org.gegolabs.mcp.bridge.examples;

import org.gegolabs.mcp.bridge.McpBridge;
import org.gegolabs.mcp.bridge.utils.LoggingUtils;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Simple example MCP server using the bridge for TCP transport.
 */
@Slf4j
public class SimpleExample {
    
    public static void main(String[] args) throws Exception {
        // Enable file logging for debugging
        LoggingUtils.initializeFileLogging("simple-mcp-server.log");
        LoggingUtils.enableDebugLogging();
        
        // Create echo tool
        var echoTool = new McpSchema.Tool(
            "echo",
            "Echoes back the input message",
            """
            {
                "type": "object",
                "properties": {
                    "message": {
                        "type": "string",
                        "description": "The message to echo"
                    }
                },
                "required": ["message"]
            }
            """
        );
        
        var echoSpec = new McpServerFeatures.AsyncToolSpecification(
            echoTool,
            (exchange, arguments) -> {
                log.info("Echo tool called with: {}", arguments);
                String message = (String) arguments.get("message");
                return Mono.just(new McpSchema.CallToolResult(
                    "Echoed: " + message,
                    false
                ));
            }
        );
        
        // Create server with TCP transport
        var server = McpServer.async(McpBridge.tcpTransport(3000))
            .serverInfo(new McpSchema.Implementation(
                "simple-example-server",
                "1.0.0"
            ))
            .tools(echoSpec)
            .build();
        
        log.info("Starting simple MCP server on port 3000");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.closeGracefully().block();
        }));
        
        // Keep the server running
        Thread.currentThread().join();
    }
}