package org.gegolabs.mcp.bridge;

import io.modelcontextprotocol.spec.McpServerTransportProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class McpBridgeTest {
    
    @Test
    void testStaticFactoryWithPort() {
        McpServerTransportProvider provider = McpBridge.tcpTransport(3000);
        assertNotNull(provider);
    }
    
    @Test
    void testStaticFactoryWithHostAndPort() {
        McpServerTransportProvider provider = McpBridge.tcpTransport("localhost", 3000);
        assertNotNull(provider);
    }
    
    @Test
    void testBuilder() {
        McpBridge bridge = McpBridge.builder()
            .port(3001)
            .host("127.0.0.1")
            .build();
            
        assertNotNull(bridge);
        assertNotNull(bridge.getTransportProvider());
    }
    
    @Test
    void testBuilderWithDefaults() {
        McpBridge bridge = McpBridge.builder().build();
        
        assertNotNull(bridge);
        assertNotNull(bridge.getTransportProvider());
    }
}