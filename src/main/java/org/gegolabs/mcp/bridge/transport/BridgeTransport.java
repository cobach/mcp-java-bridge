package org.gegolabs.mcp.bridge.transport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.modelcontextprotocol.spec.McpServerTransport;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BridgeTransport implements McpServerTransport {
    
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object writeLock = new Object();
    
    public BridgeTransport(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        this.objectMapper = new ObjectMapper();
        
        log.info("Bridge transport created for {}", socket.getRemoteSocketAddress());
    }
    
    @Override
    public Mono<Void> sendMessage(JSONRPCMessage message) {
        return Mono.fromRunnable(() -> {
            if (closed.get()) {
                throw new IllegalStateException("Transport is closed");
            }
            
            try {
                String json = objectMapper.writeValueAsString(message);
                
                // Escape newlines as per MCP protocol
                json = json.replace("\n", "\\n");
                
                synchronized (writeLock) {
                    writer.println(json);
                    writer.flush();
                }
                
                log.debug("Sent message: {}", json);
                
            } catch (Exception e) {
                log.error("Failed to send message", e);
                throw new RuntimeException("Failed to send message", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    @Override
    public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
        return objectMapper.convertValue(data, typeRef);
    }
    
    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            if (closed.compareAndSet(false, true)) {
                log.info("Closing bridge transport for {}", socket.getRemoteSocketAddress());
                
                try {
                    if (reader != null) {
                        reader.close();
                    }
                    
                    if (writer != null) {
                        writer.close();
                    }
                    
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    
                } catch (IOException e) {
                    log.error("Error closing transport", e);
                }
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
    
    /**
     * Reads a message from the transport.
     * This should be called by the session handler.
     */
    public JSONRPCMessage readMessage() throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new EOFException("End of stream reached");
        }
        
        if (line.trim().isEmpty()) {
            return null; // Skip empty lines
        }
        
        try {
            return McpSchema.deserializeJsonRpcMessage(objectMapper, line);
        } catch (Exception e) {
            log.error("Failed to parse message: {}", line, e);
            throw new IOException("Failed to parse JSON-RPC message", e);
        }
    }
    
    public boolean isClosed() {
        return closed.get();
    }
    
    public Socket getSocket() {
        return socket;
    }
}