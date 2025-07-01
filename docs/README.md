# MCP Java Bridge Documentation

This directory contains technical documentation for the MCP Java Bridge project.

## Documentation Index

### API Reference
- [API.md](API.md) - Complete API documentation for all public classes and methods

### Guides
- [Getting Started](../README.md#quick-start) - Quick start guide in main README
- [Claude Desktop Configuration](../README.md#claude-desktop-configuration) - Setup instructions for Claude Desktop

### Examples
- [Simple Example](../src/main/java/io/modelcontextprotocol/bridge/examples/SimpleExample.java) - Basic echo server
- [Example Server](../src/main/java/io/modelcontextprotocol/bridge/examples/ExampleServer.java) - Full-featured example with tools and prompts

### Architecture

The MCP Java Bridge implements a Stub-Skeleton pattern:

1. **Stub (Client Side)**
   - `BridgeStub` - Stdio client that runs in Claude Desktop's process
   - Reads JSON-RPC from stdin, forwards to TCP
   - Receives responses from TCP, writes to stdout

2. **Skeleton (Server Side)**
   - `BridgeTransportProvider` - TCP server socket management
   - `BridgeTransport` - Per-client connection handling
   - Converts between stdio protocol and TCP transport

### Protocol Flow

```
Claude Desktop → stdin → BridgeStub → TCP → BridgeTransport → MCP Server
                                                    ↓
Claude Desktop ← stdout ← BridgeStub ← TCP ← BridgeTransport ← Response
```

### Key Design Decisions

1. **Transport Abstraction**: Implements MCP SDK's transport interfaces for seamless integration
2. **Thread Safety**: Synchronized writes, dedicated read threads per connection
3. **Error Recovery**: Automatic reconnection with exponential backoff
4. **Logging**: File-based logging for production debugging without stdout interference

### Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.