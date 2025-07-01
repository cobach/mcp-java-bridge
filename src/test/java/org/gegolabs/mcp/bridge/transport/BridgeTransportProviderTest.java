package org.gegolabs.mcp.bridge.transport;

import io.modelcontextprotocol.spec.McpServerTransport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BridgeTransportProviderTest {
    
    @Test
    void testBuilderWithDefaults() {
        BridgeTransportProvider provider = BridgeTransportProvider.builder().build();
        assertNotNull(provider);
    }
    
    @Test
    void testBuilderWithCustomValues() {
        BridgeTransportProvider provider = BridgeTransportProvider.builder()
            .host("127.0.0.1")
            .port(4000)
            .build();
            
        assertNotNull(provider);
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testBuilderCreatesProvider() {
        BridgeTransportProvider provider = BridgeTransportProvider.builder()
            .port(0) // Use any available port
            .build();
            
        assertNotNull(provider);
        // Provider starts when setSessionFactory is called
    }
    
    @Test
    void testCloseProvider() {
        BridgeTransportProvider provider = BridgeTransportProvider.builder()
            .port(0)
            .build();
            
        // Provider needs sessionFactory to start
        assertDoesNotThrow(() -> provider.close());
    }
    
    @Test
    void testProviderLifecycle() {
        // Test that provider can be created and closed
        BridgeTransportProvider provider = BridgeTransportProvider.builder()
            .port(0)
            .host("127.0.0.1")
            .build();
            
        assertNotNull(provider);
        provider.close();
    }
}