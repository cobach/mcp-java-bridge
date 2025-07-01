package org.gegolabs.mcp.bridge.transport;

import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Builder
public class BridgeTransportProvider implements McpServerTransportProvider {
    
    @Builder.Default
    private final int port = 3000;
    
    @Builder.Default
    private final String host = "localhost";
    
    @Builder.Default
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("mcp-bridge-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });
    
    private ServerSocket serverSocket;
    private McpServerSession.Factory sessionFactory;
    private final List<BridgeSession> activeSessions = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread acceptThread;
    
    @Override
    public void setSessionFactory(McpServerSession.Factory sessionFactory) {
        this.sessionFactory = sessionFactory;
        
        try {
            start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start bridge transport provider", e);
        }
    }
    
    private void start() throws IOException {
        if (running.get()) {
            throw new IllegalStateException("Bridge transport provider is already running");
        }
        
        log.info("Starting bridge transport provider on {}:{}", host, port);
        
        serverSocket = new ServerSocket(port);
        running.set(true);
        
        acceptThread = new Thread(this::acceptConnections, "mcp-bridge-accept");
        acceptThread.start();
        
        log.info("Bridge transport provider started successfully on port {}", port);
    }
    
    private void acceptConnections() {
        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.info("New connection from {}", clientSocket.getRemoteSocketAddress());
                
                // Handle each client connection in a separate thread
                executor.execute(() -> handleClient(clientSocket));
                
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error accepting connection", e);
                }
            }
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try {
            // Create transport for this client
            BridgeTransport transport = new BridgeTransport(clientSocket);
            
            // Create session for this client
            McpServerSession session = sessionFactory.create(transport);
            
            // Create bridge session to manage the lifecycle
            BridgeSession bridgeSession = new BridgeSession(transport, session);
            activeSessions.add(bridgeSession);
            
            // Start processing messages
            bridgeSession.start();
            
            log.info("Client session started for {}", clientSocket.getRemoteSocketAddress());
            
        } catch (Exception e) {
            log.error("Error handling client", e);
            try {
                clientSocket.close();
            } catch (IOException ex) {
                log.error("Error closing client socket", ex);
            }
        }
    }
    
    @Override
    public Mono<Void> notifyClients(String method, Object params) {
        return Flux.fromIterable(activeSessions)
            .filter(session -> !session.isClosed())
            .flatMap(session -> session.getSession()
                .sendNotification(method, params)
                .onErrorResume(e -> {
                    log.error("Failed to send notification to client", e);
                    return Mono.empty();
                }))
            .then();
    }
    
    @Override
    public Mono<Void> closeGracefully() {
        return Mono.fromRunnable(() -> {
            log.info("Closing bridge transport provider");
            running.set(false);
            
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                log.error("Error closing server socket", e);
            }
            
            executor.shutdown();
            
            if (acceptThread != null) {
                try {
                    acceptThread.join(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        })
        .then(Flux.fromIterable(activeSessions)
            .flatMap(session -> session.close())
            .then())
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    /**
     * Internal class to manage a client session
     */
    private class BridgeSession {
        private final BridgeTransport transport;
        private final McpServerSession session;
        private Thread readerThread;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        
        public BridgeSession(BridgeTransport transport, McpServerSession session) {
            this.transport = transport;
            this.session = session;
        }
        
        public void start() {
            readerThread = new Thread(() -> {
                try {
                    while (!transport.isClosed() && !closed.get()) {
                        var message = transport.readMessage();
                        if (message != null) {
                            session.handle(message)
                                .onErrorResume(e -> {
                                    log.error("Error handling message", e);
                                    return Mono.empty();
                                })
                                .subscribe();
                        }
                    }
                } catch (EOFException e) {
                    log.info("Client disconnected: {}", transport.getSocket().getRemoteSocketAddress());
                } catch (IOException e) {
                    if (!closed.get()) {
                        log.error("Error reading from client", e);
                    }
                } finally {
                    close().subscribe();
                }
            }, "mcp-bridge-reader-" + transport.getSocket().getPort());
            
            readerThread.start();
        }
        
        public Mono<Void> close() {
            return Mono.fromRunnable(() -> {
                if (closed.compareAndSet(false, true)) {
                    activeSessions.remove(this);
                    
                    if (readerThread != null) {
                        readerThread.interrupt();
                    }
                    
                    transport.closeGracefully().subscribe();
                }
            });
        }
        
        public boolean isClosed() {
            return closed.get();
        }
        
        public McpServerSession getSession() {
            return session;
        }
    }
}