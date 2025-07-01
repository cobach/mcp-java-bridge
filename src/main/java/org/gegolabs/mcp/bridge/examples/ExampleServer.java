package org.gegolabs.mcp.bridge.examples;

import org.gegolabs.mcp.bridge.McpBridge;
import org.gegolabs.mcp.bridge.utils.JsonSchemaUtils;
import org.gegolabs.mcp.bridge.utils.LoggingUtils;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive example MCP server demonstrating various features.
 * This server includes multiple tools and prompts to showcase the bridge capabilities.
 */
@Slf4j
public class ExampleServer {
    
    private static final List<String> todoList = new ArrayList<>();
    private static final Map<String, String> keyValueStore = new HashMap<>();
    
    public static void main(String[] args) throws Exception {
        // Enable file logging
        LoggingUtils.initializeFileLogging("example-mcp-server.log");
        LoggingUtils.enableDebugLogging();
        
        log.info("Initializing Example MCP Server...");
        
        // Create server with multiple tools and prompts
        var server = McpServer.async(McpBridge.tcpTransport(3000))
            .serverInfo(new McpSchema.Implementation(
                "example-mcp-server",
                "1.0.0"
            ))
            .tools(
                createEchoTool(),
                createTimeTool(),
                createTodoTool(),
                createKeyValueTool(),
                createCalculatorTool()
            )
            // Prompts are not supported in this example yet
            // .prompts() would require AsyncPromptSpecification implementation
            .build();
        
        log.info("Example MCP Server started on port 3000");
        log.info("Available tools: echo, get_time, todo_list, key_value_store, calculator");
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down server...");
            server.closeGracefully().block();
        }));
        
        // Keep server running
        Thread.currentThread().join();
    }
    
    /**
     * Echo tool - echoes back the input message
     */
    private static McpServerFeatures.AsyncToolSpecification createEchoTool() {
        var tool = new McpSchema.Tool(
            "echo",
            "Echoes back the input message",
            """
            {
                "type": "object",
                "properties": {
                    "message": {
                        "type": "string",
                        "description": "The message to echo back"
                    }
                },
                "required": ["message"]
            }
            """
        );
        
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, arguments) -> {
                String message = (String) arguments.get("message");
                log.info("Echo tool called with: {}", message);
                return Mono.just(new McpSchema.CallToolResult(
                    "Echo: " + message,
                    false
                ));
            }
        );
    }
    
    /**
     * Time tool - returns current date and time
     */
    private static McpServerFeatures.AsyncToolSpecification createTimeTool() {
        var tool = new McpSchema.Tool(
            "get_time",
            "Get the current date and time",
            """
            {
                "type": "object",
                "properties": {
                    "format": {
                        "type": "string",
                        "description": "Time format (ISO, SIMPLE, FULL)",
                        "enum": ["ISO", "SIMPLE", "FULL"],
                        "default": "ISO"
                    }
                },
                "required": []
            }
            """
        );
        
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, arguments) -> {
                String format = (String) arguments.getOrDefault("format", "ISO");
                LocalDateTime now = LocalDateTime.now();
                
                String formattedTime = switch (format) {
                    case "SIMPLE" -> now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    case "FULL" -> now.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm:ss a"));
                    default -> now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                };
                
                log.info("Time requested in {} format: {}", format, formattedTime);
                return Mono.just(new McpSchema.CallToolResult(formattedTime, false));
            }
        );
    }
    
    /**
     * Todo list tool - manage a simple todo list
     */
    private static McpServerFeatures.AsyncToolSpecification createTodoTool() {
        var tool = new McpSchema.Tool(
            "todo_list",
            "Manage a todo list",
            """
            {
                "type": "object",
                "properties": {
                    "action": {
                        "type": "string",
                        "description": "Action to perform",
                        "enum": ["add", "remove", "list", "clear"]
                    },
                    "item": {
                        "type": "string",
                        "description": "Todo item (required for add/remove)"
                    }
                },
                "required": ["action"]
            }
            """
        );
        
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, arguments) -> {
                String action = (String) arguments.get("action");
                String item = (String) arguments.get("item");
                
                synchronized (todoList) {
                    String result = switch (action) {
                        case "add" -> {
                            if (item == null || item.trim().isEmpty()) {
                                yield "Error: Item is required for add action";
                            }
                            todoList.add(item);
                            log.info("Added todo item: {}", item);
                            yield "Added: " + item;
                        }
                        case "remove" -> {
                            if (item == null || item.trim().isEmpty()) {
                                yield "Error: Item is required for remove action";
                            }
                            boolean removed = todoList.remove(item);
                            log.info("Removed todo item: {} (success: {})", item, removed);
                            yield removed ? "Removed: " + item : "Item not found: " + item;
                        }
                        case "list" -> {
                            log.info("Listing {} todo items", todoList.size());
                            yield todoList.isEmpty() ? "Todo list is empty" :
                                "Todo items:\n" + String.join("\n", todoList.stream()
                                    .map(i -> "- " + i)
                                    .toList());
                        }
                        case "clear" -> {
                            int count = todoList.size();
                            todoList.clear();
                            log.info("Cleared {} todo items", count);
                            yield "Cleared " + count + " items";
                        }
                        default -> "Unknown action: " + action;
                    };
                    
                    return Mono.just(new McpSchema.CallToolResult(result, false));
                }
            }
        );
    }
    
    /**
     * Key-value store tool
     */
    private static McpServerFeatures.AsyncToolSpecification createKeyValueTool() {
        var tool = new McpSchema.Tool(
            "key_value_store",
            "Simple key-value store",
            """
            {
                "type": "object",
                "properties": {
                    "action": {
                        "type": "string",
                        "description": "Action to perform",
                        "enum": ["get", "set", "delete", "list"]
                    },
                    "key": {
                        "type": "string",
                        "description": "Key (required for get/set/delete)"
                    },
                    "value": {
                        "type": "string",
                        "description": "Value (required for set)"
                    }
                },
                "required": ["action"]
            }
            """
        );
        
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, arguments) -> {
                String action = (String) arguments.get("action");
                String key = (String) arguments.get("key");
                String value = (String) arguments.get("value");
                
                synchronized (keyValueStore) {
                    String result = switch (action) {
                        case "get" -> {
                            if (key == null) yield "Error: Key is required";
                            String val = keyValueStore.get(key);
                            yield val != null ? val : "Key not found: " + key;
                        }
                        case "set" -> {
                            if (key == null) yield "Error: Key is required";
                            if (value == null) yield "Error: Value is required";
                            keyValueStore.put(key, value);
                            yield "Set " + key + " = " + value;
                        }
                        case "delete" -> {
                            if (key == null) yield "Error: Key is required";
                            String removed = keyValueStore.remove(key);
                            yield removed != null ? "Deleted key: " + key : "Key not found: " + key;
                        }
                        case "list" -> {
                            if (keyValueStore.isEmpty()) {
                                yield "Store is empty";
                            }
                            yield "Keys:\n" + keyValueStore.entrySet().stream()
                                .map(e -> e.getKey() + " = " + e.getValue())
                                .reduce((a, b) -> a + "\n" + b)
                                .orElse("Store is empty");
                        }
                        default -> "Unknown action: " + action;
                    };
                    
                    log.info("Key-value action {} result: {}", action, result);
                    return Mono.just(new McpSchema.CallToolResult(result, false));
                }
            }
        );
    }
    
    /**
     * Calculator tool
     */
    private static McpServerFeatures.AsyncToolSpecification createCalculatorTool() {
        var tool = new McpSchema.Tool(
            "calculator",
            "Simple calculator for basic math operations",
            """
            {
                "type": "object",
                "properties": {
                    "operation": {
                        "type": "string",
                        "description": "Math operation",
                        "enum": ["add", "subtract", "multiply", "divide", "power", "sqrt"]
                    },
                    "a": {
                        "type": "number",
                        "description": "First operand"
                    },
                    "b": {
                        "type": "number",
                        "description": "Second operand (not needed for sqrt)"
                    }
                },
                "required": ["operation", "a"]
            }
            """
        );
        
        return new McpServerFeatures.AsyncToolSpecification(
            tool,
            (exchange, arguments) -> {
                String operation = (String) arguments.get("operation");
                Object aObj = arguments.get("a");
                Object bObj = arguments.get("b");
                
                try {
                    double a = aObj instanceof Number ? ((Number) aObj).doubleValue() : Double.parseDouble(aObj.toString());
                    double b = bObj instanceof Number ? ((Number) bObj).doubleValue() : 
                               (bObj != null ? Double.parseDouble(bObj.toString()) : 0);
                    
                    double result = switch (operation) {
                        case "add" -> a + b;
                        case "subtract" -> a - b;
                        case "multiply" -> a * b;
                        case "divide" -> {
                            if (b == 0) throw new ArithmeticException("Division by zero");
                            yield a / b;
                        }
                        case "power" -> Math.pow(a, b);
                        case "sqrt" -> Math.sqrt(a);
                        default -> throw new IllegalArgumentException("Unknown operation: " + operation);
                    };
                    
                    String response = String.format("%s result: %.6f", operation, result);
                    log.info("Calculator: {} {} {} = {}", a, operation, b, result);
                    return Mono.just(new McpSchema.CallToolResult(response, false));
                    
                } catch (Exception e) {
                    log.error("Calculator error", e);
                    return Mono.just(new McpSchema.CallToolResult(
                        "Error: " + e.getMessage(), 
                        true
                    ));
                }
            }
        );
    }
}