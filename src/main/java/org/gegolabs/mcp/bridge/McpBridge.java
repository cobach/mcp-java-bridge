package org.gegolabs.mcp.bridge;

import org.gegolabs.mcp.bridge.transport.BridgeTransportProvider;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * MCP Bridge main class that provides TCP transport support for MCP Java servers.
 * 
 * Usage:
 * <pre>
 * var transportProvider = McpBridge.tcpTransport(3000);
 * 
 * McpServer server = McpServer.builder()
 *     .transportProvider(transportProvider)
 *     .addTool(new MyTool())
 *     .build();
 * </pre>
 */
@Slf4j
public class McpBridge {
    
    private final int port;
    private final String host;
    
    private McpServerTransportProvider transportProvider;
    
    /**
     * Creates a new MCP Bridge instance.
     */
    private McpBridge(int port, String host) {
        this.port = port != 0 ? port : 3000;
        this.host = host != null ? host : "localhost";
        
        this.transportProvider = BridgeTransportProvider.builder()
            .port(this.port)
            .host(this.host)
            .build();
            
        log.info("MCP Bridge configured for {}:{}", host, port);
    }
    
    /**
     * Creates a builder for McpBridge.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for McpBridge.
     */
    public static class Builder {
        private int port = 3000;
        private String host = "localhost";
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder host(String host) {
            this.host = host;
            return this;
        }
        
        public McpBridge build() {
            return new McpBridge(port, host);
        }
    }
    
    public McpServerTransportProvider getTransportProvider() {
        return transportProvider;
    }
    
    /**
     * Static factory method for creating a TCP transport provider.
     * 
     * @param port The port to listen on
     * @return A configured transport provider
     */
    public static McpServerTransportProvider tcpTransport(int port) {
        return BridgeTransportProvider.builder()
            .port(port)
            .build();
    }
    
    /**
     * Static factory method with host and port.
     * 
     * @param host The host to bind to
     * @param port The port to listen on
     * @return A configured transport provider
     */
    public static McpServerTransportProvider tcpTransport(String host, int port) {
        return BridgeTransportProvider.builder()
            .host(host)
            .port(port)
            .build();
    }
}