package org.gegolabs.mcp.bridge.integration;

import org.gegolabs.mcp.bridge.McpBridge;
import org.gegolabs.mcp.bridge.transport.BridgeTransportProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeIntegrationTest {
    
    @Test
    void testStaticFactoryMethod() {
        // Test that static factory method works
        var provider = McpBridge.tcpTransport(0);
        assertNotNull(provider);
        assertInstanceOf(BridgeTransportProvider.class, provider);
    }
    
    @Test
    void testStaticFactoryWithHost() {
        var provider = McpBridge.tcpTransport("localhost", 0);
        assertNotNull(provider);
        assertInstanceOf(BridgeTransportProvider.class, provider);
    }
    
    @Test
    void testBuilderPattern() {
        var bridge = McpBridge.builder()
            .port(0)
            .host("localhost")
            .build();
            
        assertNotNull(bridge);
        assertNotNull(bridge.getTransportProvider());
        assertInstanceOf(BridgeTransportProvider.class, bridge.getTransportProvider());
    }
    
    @Test
    void testProviderCreation() {
        var provider = BridgeTransportProvider.builder()
            .port(0)
            .host("localhost")
            .build();
            
        assertNotNull(provider);
        
        // Provider starts when setSessionFactory is called
        // Just verify it was created successfully
        provider.close();
    }
}