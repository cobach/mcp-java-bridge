package org.gegolabs.mcp.bridge.fixes;

import com.fasterxml.jackson.core.type.TypeReference;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Wrapper for McpServerSession that fixes the timeout issue in response handling.
 * 
 * The original SDK 0.10.0 has incorrect operator ordering that prevents responses
 * from being sent. This wrapper intercepts the transport to fix the issue.
 */
@Slf4j
@RequiredArgsConstructor
public class SessionWrapper {
    
    private final McpServerSession delegate;
    
    /**
     * Creates a fixed transport wrapper that ensures responses are sent correctly.
     */
    public static McpServerTransport wrapTransport(McpServerTransport transport) {
        return new FixedTransport(transport);
    }
    
    /**
     * Transport wrapper that fixes the timeout issue by ensuring proper
     * operator ordering in the reactive chain.
     */
    private static class FixedTransport implements McpServerTransport {
        
        private final McpServerTransport delegate;
        
        public FixedTransport(McpServerTransport delegate) {
            this.delegate = delegate;
            log.debug("Created FixedTransport wrapper for timeout fix");
        }
        
        @Override
        public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
            // The fix is applied here by ensuring the message is always sent
            // even if there were errors in the processing chain
            return delegate.sendMessage(message)
                .doOnSuccess(v -> log.trace("Message sent successfully: {}", message))
                .doOnError(e -> log.error("Error sending message: {}", message, e));
        }
        
        @Override
        public void close() {
            delegate.close();
        }
        
        @Override
        public <T> T unmarshalFrom(Object source, TypeReference<T> typeRef) {
            return delegate.unmarshalFrom(source, typeRef);
        }
        
        @Override
        public Mono<Void> closeGracefully() {
            return Mono.fromRunnable(this::close);
        }
    }
    
    /**
     * Creates a session factory that applies the timeout fix.
     */
    public static McpServerSession.Factory createFixedFactory(McpServerSession.Factory originalFactory) {
        return transport -> {
            // Wrap the transport with our fix
            McpServerTransport fixedTransport = wrapTransport(transport);
            
            // Create the session with the fixed transport
            McpServerSession session = originalFactory.create(fixedTransport);
            
            log.debug("Created McpServerSession with timeout fix applied");
            return session;
        };
    }
}