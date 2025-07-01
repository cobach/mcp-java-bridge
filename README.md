# MCP Java Bridge

Runtime decoupling solution for MCP Java servers, solving the tight coupling issues inherent in stdio-based integration.

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![MCP SDK](https://img.shields.io/badge/MCP%20SDK-0.10.0-green.svg)](https://github.com/modelcontextprotocol/java-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## The Problem

The native stdio implementation in the MCP Java SDK creates a tight coupling between the client and server runtimes. This coupling causes several critical issues:

- **Resource contention**: Client and server compete for the same system resources
- **Logging conflicts**: Both processes write to the same output streams, making debugging difficult
- **Context pollution**: Server environment variables and system properties affect the client
- **Lifecycle management**: Server lifecycle is tied to client process, preventing independent scaling
- **Development complexity**: Testing and debugging require running both components together

## The Solution

While Streamable HTTP would be the ideal alternative for decoupled communication, the current MCP Java SDK only supports SSE (Server-Sent Events) and stdio transports - not Streamable HTTP. This limitation led to the creation of MCP Java Bridge.

MCP Java Bridge decouples the client and server runtimes while maintaining full stdio compatibility. It introduces a lightweight "connector" that:

1. **Integrates with MCP clients via stdio** (100% compatible with Claude Desktop and other clients)
2. **Connects to your Java server via TCP** behind the scenes
3. **Runs each component in its own process** with isolated resources
4. **Requires zero changes** to your existing MCP server code
5. **Transparent to both client and developer** - just works out of the box

The result is a robust, production-ready integration that solves all the coupling issues while maintaining the simplicity of the MCP protocol.

## Architecture

```
┌─────────────────┐        stdio         ┌───────────────────────────────────┐
│  Claude Desktop │ ◄──────────────────► │          MCP Bridge               │
│    (Client)     │                      │ ┌─────────┐      ┌──────────┐   │
└─────────────────┘                      │ │  Stub   │ TCP  │ Skeleton │   │
                                         │ │ (stdio) │◄────►│  (Java)  │   │
                                         │ └─────────┘      └──────────┘   │
                                         └───────────────────────────────────┘
                                                                 │
                                                                 │ Embedded
                                                                 ▼
                                                         ┌─────────────────┐
                                                         │ MCP Java Server │
                                                         │   (with SDK)    │
                                                         └─────────────────┘
```

## Features

- **All-in-One JAR**: Single JAR serves as library, connector, and installer
- **Transparent TCP Support**: Enables TCP connectivity without client modifications
- **Simple Integration**: Easy to integrate with existing MCP Java servers
- **Production Ready**: Includes logging, error handling, and connection management
- **Flexible Configuration**: Configurable ports and connection settings
- **Interactive Installer**: Zero-configuration setup for Claude Desktop
- **Self-Installing**: The JAR can install itself as a connector

## Getting Started

### Step 1: Add Dependency

Add mcp-java-bridge to your project:

#### Maven
```xml
<dependency>
    <groupId>org.gegolabs.mcp</groupId>
    <artifactId>mcp-java-bridge</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Gradle
```groovy
implementation 'org.gegolabs.mcp:mcp-java-bridge:1.0.0-SNAPSHOT'
```

**Note**: This is currently a SNAPSHOT version. Add `mavenLocal()` to your repositories if you've installed it locally.

### Step 2: Create Your MCP Server

Use the bridge to create your MCP server with TCP transport:

#### Option 1: Using the Bridge Builder

```java
import org.gegolabs.mcp.bridge.McpBridge;
import io.modelcontextprotocol.sdk.McpServer;

public class MyMcpServer {
    public static void main(String[] args) throws Exception {
        // Create bridge
        McpBridge bridge = McpBridge.builder()
            .port(3000)
            .build();
        
        // Create your MCP server with bridge transport
        McpServer server = McpServer.builder()
            .transportProvider(bridge.getTransportProvider())
            .toolsProvider(() -> /* your tools */)
            .toolHandler((name, args) -> /* handle tool calls */)
            .build();
        
        server.start();
        
        // Keep the server running
        Thread.currentThread().join();
    }
}
```

### Option 2: Using Static Factory Method

```java
import org.gegolabs.mcp.bridge.McpBridge;
import io.modelcontextprotocol.sdk.McpServer;

public class MyMcpServer {
    public static void main(String[] args) throws Exception {
        McpServer server = McpServer.builder()
            .transportProvider(McpBridge.tcpTransport(3000))
            .toolsProvider(() -> /* your tools */)
            .toolHandler((name, args) -> /* handle tool calls */)
            .build();
        
        server.start();
        Thread.currentThread().join();
    }
}
```

### Step 3: Instalación en Claude Desktop

After building your MCP server, you need to configure Claude Desktop to connect to it. The mcp-java-bridge JAR includes a CLI installer for this purpose.

#### Access the Bridge JAR

Since you've added mcp-java-bridge as a dependency, you can access it in two ways:

**From Maven Repository:**
```bash
java -jar ~/.m2/repository/org/gegolabs/mcp/mcp-java-bridge/1.0.0/mcp-java-bridge-1.0.0.jar
```

**Or copy it using a Gradle task:**
```gradle
task copyBridgeJar(type: Copy) {
    from configurations.runtimeClasspath.filter { it.name.contains('mcp-java-bridge') }
    into 'install'
    rename { 'mcp-bridge.jar' }
}
```

Then: `./gradlew copyBridgeJar`

#### Configure Claude Desktop

Choose one of these three options:

##### Option A: Interactive Installation (Recommended)

Run the installer without arguments for a guided setup:

```bash
java -jar mcp-java-bridge-1.0.0.jar
```

This will:
- Auto-detect the JAR location
- Prompt for server name (e.g., "my-server")
- Prompt for host (default: localhost)
- Prompt for port (default: 3000)
- Automatically configure Claude Desktop
- Create a backup of existing configuration

##### Option B: Command Line Installation

For automated setups, use specific parameters:

```bash
java -jar mcp-java-bridge-1.0.0.jar install \
  -n "my-server" \
  -c mcp-java-bridge-1.0.0.jar \
  -h localhost \
  -p 3000
```

**Parameters:**
- `-n` - Server name in Claude Desktop (required)
- `-c` - Path to the JAR that will act as connector
- `-h` - Server host (default: localhost)
- `-p` - Server port (default: 3000)

##### Option C: Manual Configuration

If you prefer to configure manually, edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "my-server": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/mcp-java-bridge-1.0.0.jar",
        "--connector",
        "localhost",
        "3000"
      ]
    }
  }
}
```

### Step 4: Start Your Server

1. Start your MCP server (make sure it's running on the configured port)
2. Restart Claude Desktop to load the new configuration
3. Your server should now be available in Claude Desktop

## Additional Resources

### Example Code

The project includes example code in the source:
- `SimpleExample.java` - Basic echo server showing minimal setup
- `ExampleServer.java` - Full-featured server with multiple tools (this is the one built as the demo JAR)

## Build Artifacts

After building, you'll find these JARs in `build/libs/`:
- `mcp-java-bridge-1.0.0-SNAPSHOT.jar` - Main JAR (library + connector + installer)
- `mcp-java-bridge-1.0.0-SNAPSHOT-example.jar` - Demo server application
- `mcp-java-bridge-1.0.0-SNAPSHOT-sources.jar` - Source code

## Demo Application

The demo JAR (`mcp-java-bridge-1.0.0-SNAPSHOT-example.jar`) runs the ExampleServer with these tools:
- **echo** - Echoes back messages
- **get_time** - Returns current time in various formats
- **todo_list** - Manage a simple todo list (add, remove, list, clear)
- **key_value_store** - Simple key-value storage (get, set, delete, list)
- **calculator** - Basic math operations (add, subtract, multiply, divide, power, sqrt)

### Running the Demo

1. **Build the project** (if not already built):
   ```bash
   ./gradlew clean build
   ```

2. **Run the demo server**:
   ```bash
   # Using the provided script
   cd examples
   ./run-demo.sh
   
   # Or run directly
   java -jar build/libs/mcp-java-bridge-1.0.0-SNAPSHOT-example.jar
   ```

3. **Test with curl** (optional):
   While the server is designed for MCP clients, you can verify it's running:
   ```bash
   # This will fail with a protocol error (expected) but confirms the server is listening
   telnet localhost 3000
   ```

4. **Configure Claude Desktop** using the installer (see Step 3 in Getting Started)

### Demo Script

The `examples/run-demo.sh` script:
- Checks Java version (requires Java 17+)
- Builds the project if needed
- Starts the example server
- Shows Claude Desktop configuration

## CLI Commands

The MCP Java Bridge JAR is a multi-purpose tool that serves three different functions:

### 1. Interactive Installer (Default - No Arguments)

Running without arguments starts an interactive installer:

```bash
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar
```

This will:
- Auto-detect the JAR location
- Prompt for server name (e.g., "my-mcp-server")
- Prompt for host (default: localhost)
- Prompt for port (default: 3000)
- Automatically configure Claude Desktop
- Create a backup of existing configuration

### 2. Connector Mode

Run as a connector to bridge stdio↔TCP communication. This is what Claude Desktop executes:

```bash
# With default settings (localhost:3000)
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar --connector

# With custom host/port
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar --connector 192.168.1.100 8080
```

**Note**: This mode is typically not run manually - it's executed by Claude Desktop.

### 3. Install Command

For non-interactive installation with specific parameters:

```bash
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar install -n <server-name> -c <jar-path> [-h <host>] [-p <port>]
```

**Arguments:**
- `-n` - Server name in Claude Desktop (required)
- `-c` - Path to the JAR or script that will act as connector
- `-h` - Server host (default: localhost)
- `-p` - Server port (default: 3000)

**Examples:**

```bash
# Install using the same JAR as connector
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar install \
  -n "my-server" \
  -c ./mcp-java-bridge-1.0.0-SNAPSHOT.jar \
  -h localhost \
  -p 3000

# Install using a custom script as connector (e.g., from uMCP)
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar install \
  -n "my-umcp-server" \
  -c /path/to/uMCP/install/bin/uMCP-connector \
  -h localhost \
  -p 3000
```

### Help Command

Display usage information:

```bash
java -jar mcp-java-bridge-1.0.0-SNAPSHOT.jar --help
```

### Testing with Claude Desktop

Once connected, you can test the demo tools:

1. **Echo Tool**:
   ```
   "Please use the echo tool to say 'Hello from MCP!'"
   ```

2. **Time Tool**:
   ```
   "What time is it? Show me in different formats."
   ```

3. **Todo List**:
   ```
   "Add 'Test MCP Bridge' to my todo list"
   "Show me my todo list"
   "Remove 'Test MCP Bridge' from the list"
   ```

4. **Key-Value Store**:
   ```
   "Store my name as 'John Doe' in the key-value store"
   "What's stored under the key 'name'?"
   ```

5. **Calculator**:
   ```
   "Calculate 42 * 17 using the calculator tool"
   "What's the square root of 144?"
   ```

## Utilities

### Logging Configuration

The bridge includes utilities for configuring file-based logging, essential for debugging:

```java
import org.gegolabs.mcp.bridge.utils.LoggingUtils;

// Enable file logging
LoggingUtils.initializeFileLogging("my-mcp-server.log");

// Enable debug logging
LoggingUtils.enableDebugLogging();
```

Logs are saved to `~/.mcp-bridge/logs/`.

### JSON Schema Generation

Generate JSON schemas for your tool parameters:

```java
import org.gegolabs.mcp.bridge.utils.JsonSchemaUtils;

public class MyToolParams {
    @JsonSchemaUtils.Description("The user's name")
    private String name;
    
    @JsonSchemaUtils.Description("The user's age")
    private int age;
}

// Generate schema
String schema = JsonSchemaUtils.generateJsonSchema(MyToolParams.class);
```

## Development

### Building from Source

```bash
git clone https://github.com/gegolabs/mcp-java-bridge.git
cd mcp-java-bridge
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Publishing to Local Maven

```bash
./gradlew publishToMavenLocal
```

## Requirements

- Java 17 or higher
- MCP Java SDK 0.10.0 or higher

## License

MIT License - see LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.E