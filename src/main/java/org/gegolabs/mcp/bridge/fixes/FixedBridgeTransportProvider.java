package org.gegolabs.mcp.bridge.fixes;

import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.gegolabs.mcp.bridge.transport.BridgeTransportProvider;
import reactor.core.publisher.Mono;

/**
 * A transport provider wrapper that applies SDK fixes at runtime.
 * This allows using the official SDK 0.10.0 while fixing critical bugs.
 */
@Slf4j
public class FixedBridgeTransportProvider implements McpServerTransportProvider {
    
    private final BridgeTransportProvider delegate;
    
    public FixedBridgeTransportProvider(BridgeTransportProvider delegate) {
        this.delegate = delegate;
        log.info("Created FixedBridgeTransportProvider with SDK fixes");
    }
    
    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        // Apply the timeout fix by wrapping the session factory
        McpServerSession.Factory fixedFactory = TimeoutFixInterceptor.createFixedSessionFactory(sessionFactory);
        
        delegate.setSessionFactory(fixedFactory);
        log.info("Applied timeout fix to session factory");
    }
    
    @Override
    public Mono<Void> closeGracefully() {
        return delegate.closeGracefully();
    }
    
    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return delegate.notifyClients(method, params);
    }
    
    /**
     * Creates a fixed transport provider from a regular BridgeTransportProvider.
     */
    public static FixedBridgeTransportProvider wrap(BridgeTransportProvider provider) {
        return new FixedBridgeTransportProvider(provider);
    }
}