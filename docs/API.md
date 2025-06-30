# MCP Java Bridge API Documentation

## Core Classes

### McpBridge

The main entry point for creating TCP-enabled MCP servers.

#### Static Factory Methods

```java
// Create TCP transport on default port
McpServerTransportProvider transport = McpBridge.tcpTransport(3000);

// Create TCP transport with custom host
McpServerTransportProvider transport = McpBridge.tcpTransport("0.0.0.0", 3000);
```

#### Builder Pattern

```java
McpBridge bridge = McpBridge.builder()
    .port(3000)
    .host("localhost")
    .build();
    
McpServerTransportProvider provider = bridge.getTransportProvider();
```

### BridgeTransportProvider

Implements `McpServerTransportProvider` to handle TCP server socket creation and client connections.

#### Key Methods

- `start()` - Starts the TCP server and begins accepting connections
- `acceptConnection()` - Creates a new `BridgeTransport` for each client

### BridgeTransport

Implements `McpServerTransport` to handle stdio↔TCP conversion for individual client connections.

#### Key Features

- Thread-safe message sending with write locks
- Automatic JSON serialization/deserialization
- Graceful connection shutdown
- Error recovery with automatic reconnection

## Utility Classes

### LoggingUtils

Provides file-based logging configuration for debugging MCP servers.

```java
// Initialize file logging
LoggingUtils.initializeFileLogging("my-server.log");

// Enable debug level
LoggingUtils.enableDebugLogging();

// Set custom log level
LoggingUtils.setLogLevel("io.modelcontextprotocol", org.slf4j.ch.qos.logback.classic.Level.TRACE);
```

Logs are saved to: `~/.mcp-bridge/logs/`

### JsonSchemaUtils

Generates JSON schemas from Java classes for tool parameters.

```java
// Basic usage
String schema = JsonSchemaUtils.generateJsonSchema(MyParams.class);

// With descriptions
public class MyParams {
    @JsonSchemaUtils.Description("User's name")
    private String name;
    
    @JsonSchemaUtils.Description("User's age")
    private Integer age;
}
```

## Client Components

### BridgeStub

The stdio client that connects to TCP servers. Used by Claude Desktop.

#### Usage

```bash
java -jar mcp-java-bridge-stub.jar <host> <port>
```

#### Protocol

1. Reads JSON-RPC messages from stdin
2. Forwards to TCP server
3. Receives responses from TCP
4. Writes to stdout

## Error Handling

### Connection Errors

The bridge handles various connection scenarios:

- **Server not running**: Stub retries connection with exponential backoff
- **Client disconnect**: Server cleans up resources and waits for new connections
- **Message errors**: Invalid JSON is logged and skipped

### Logging

All components use SLF4J with Logback. Key loggers:

- `org.gegolabs.mcp.bridge` - Main bridge operations
- `org.gegolabs.mcp.bridge.transport` - Transport layer
- `org.gegolabs.mcp.bridge.client` - Stub operations

## Thread Safety

- `BridgeTransport` uses synchronized blocks for write operations
- Message reading happens on dedicated threads
- Reactive streams (Project Reactor) handle async operations

## Performance Considerations

- Connection pooling: Each client gets a dedicated thread
- Message buffering: Uses BufferedReader/PrintWriter for efficiency
- Non-blocking I/O: Leverages Project Reactor for async operations

## Integration Examples

### With Spring Boot

```java
@Configuration
public class McpConfig {
    @Bean
    public McpServerTransportProvider mcpTransport() {
        return McpBridge.tcpTransport(3000);
    }
    
    @Bean
    public McpServer mcpServer(McpServerTransportProvider transport) {
        return McpServer.async(transport)
            .serverInfo(new McpSchema.Implementation("my-server", "1.0.0"))
            .tools(/* your tools */)
            .build();
    }
}
```

### With Custom Error Handling

```java
var transport = BridgeTransportProvider.builder()
    .port(3000)
    .errorHandler(error -> log.error("Transport error", error))
    .build();
```