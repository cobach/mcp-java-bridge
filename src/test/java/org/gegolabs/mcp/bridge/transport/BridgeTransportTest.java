package org.gegolabs.mcp.bridge.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.io.*;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BridgeTransportTest {
    
    private Socket mockSocket;
    private BufferedReader mockReader;
    private PrintWriter mockWriter;
    private BridgeTransport transport;
    private ObjectMapper objectMapper = new ObjectMapper();
    
    @BeforeEach
    void setUp() throws IOException {
        mockSocket = mock(Socket.class);
        
        // Mock input/output streams
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayInputStream inputStream = new ByteArrayInputStream("{}".getBytes());
        
        when(mockSocket.getOutputStream()).thenReturn(outputStream);
        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getRemoteSocketAddress()).thenReturn(null);
        
        // Create transport
        transport = new BridgeTransport(mockSocket);
    }
    
    @Test
    void testSendMessage() throws IOException {
        // Create a request message
        var message = new McpSchema.JSONRPCRequest(
            "2.0",
            "test",
            null,
            "1"
        );
        
        // Create transport with real streams
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        when(socket.getInputStream()).thenReturn(new ByteArrayInputStream("{}".getBytes()));
        when(socket.getRemoteSocketAddress()).thenReturn(null);
        
        BridgeTransport transport = new BridgeTransport(socket);
        
        StepVerifier.create(transport.sendMessage(message))
            .verifyComplete();
            
        // Verify message was written
        String output = outputStream.toString();
        assertTrue(output.contains("\"jsonrpc\":\"2.0\""));
        assertTrue(output.contains("\"method\":\"test\""));
    }
    
    @Test
    void testCloseConnection() throws IOException {
        transport.close();
        
        verify(mockSocket).close();
    }
    
    @Test
    void testSendAfterClose() throws IOException {
        transport.close();
        
        var message = new McpSchema.JSONRPCRequest(
            "2.0",
            "test",
            null,
            "1"
        );
        
        StepVerifier.create(transport.sendMessage(message))
            .expectError(IllegalStateException.class)
            .verify();
    }
}