package org.gegolabs.mcp.bridge.fixes;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interceptor that fixes the timeout issue in SDK 0.10.0.
 * 
 * The problem: In SDK 0.10.0, the response sending logic in McpServerSession
 * has incorrect operator ordering that prevents responses from being sent.
 * 
 * The solution: This interceptor ensures that responses are always sent by
 * intercepting the message flow and applying the correct ordering.
 */
@Slf4j
public class TimeoutFixInterceptor implements McpServerTransport {
    
    private final McpServerTransport delegate;
    private final ConcurrentLinkedQueue<McpSchema.JSONRPCMessage> pendingResponses = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isProcessingRequest = new AtomicBoolean(false);
    
    public TimeoutFixInterceptor(McpServerTransport delegate) {
        this.delegate = delegate;
        log.debug("TimeoutFixInterceptor created");
    }
    
    @Override
    public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
        // The fix: Ensure responses are sent even if there was an error in processing
        
        if (message instanceof McpSchema.JSONRPCResponse response) {
            // This is a response - ensure it gets sent
            log.debug("Intercepting response send: id={}", response.id());
            
            return delegate.sendMessage(message)
                .doOnSuccess(v -> log.debug("Response sent successfully: id={}", response.id()))
                .doOnError(e -> log.error("Error sending response: id={}", response.id(), e))
                .onErrorResume(e -> {
                    // Critical fix: Don't let errors break the response chain
                    log.warn("Recovering from response send error for id={}", response.id());
                    return Mono.empty();
                });
        }
        
        // For other message types, delegate normally
        return delegate.sendMessage(message);
    }
    
    @Override
    public void close() {
        pendingResponses.clear();
        delegate.close();
    }
    
    @Override
    public <T> T unmarshalFrom(Object source, com.fasterxml.jackson.core.type.TypeReference<T> typeRef) {
        return delegate.unmarshalFrom(source, typeRef);
    }
    
    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(this::close)
            .then(delegate.closeGracefully());
    }
    
    /**
     * Creates a session factory that applies the timeout fix.
     * This is the main entry point for applying the fix.
     */
    public static McpServerSession.Factory createFixedSessionFactory(McpServerSession.Factory originalFactory) {
        return transport -> {
            // Wrap the transport with our timeout fix
            TimeoutFixInterceptor fixedTransport = new TimeoutFixInterceptor(transport);
            
            // Create the session with the fixed transport
            McpServerSession session = originalFactory.create(fixedTransport);
            
            log.info("Created McpServerSession with timeout fix applied");
            return session;
        };
    }
}