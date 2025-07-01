package org.gegolabs.mcp.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple test client to reproduce timeout issues
 */
@Slf4j
public class TimeoutTestClient {
    
    public static void main(String[] args) throws Exception {
        log.info("Connecting to test server...");
        
        Socket socket = new Socket("localhost", 3000);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        ObjectMapper mapper = new ObjectMapper();
        
        // Start reader thread
        CountDownLatch responseLatch = new CountDownLatch(2); // init response + tools/list response
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Received: {}", line);
                    responseLatch.countDown();
                }
            } catch (IOException e) {
                log.error("Reader error", e);
            }
        });
        readerThread.start();
        
        // Send initialize request
        var initRequest = new JSONRPCRequest(
            "2.0",
            "initialize",
            "init-1",
            new InitializeRequest(
                "2024-11-05",
                new ClientCapabilities(null, null, null),
                new Implementation("test-client", "1.0.0")
            )
        );
        
        String initJson = mapper.writeValueAsString(initRequest);
        log.info("Sending initialize: {}", initJson);
        writer.println(initJson);
        
        // Wait a bit for response
        Thread.sleep(1000);
        
        // Send initialized notification
        var initNotification = new JSONRPCNotification(
            "2.0",
            "notifications/initialized",
            null
        );
        
        String notifJson = mapper.writeValueAsString(initNotification);
        log.info("Sending initialized notification: {}", notifJson);
        writer.println(notifJson);
        
        // Wait a bit
        Thread.sleep(1000);
        
        // Send tools/list request
        var toolsRequest = new JSONRPCRequest(
            "2.0",
            "tools/list",
            "tools-1",
            null
        );
        
        String toolsJson = mapper.writeValueAsString(toolsRequest);
        log.info("Sending tools/list request: {}", toolsJson);
        writer.println(toolsJson);
        
        // Wait for responses with timeout
        boolean gotAllResponses = responseLatch.await(5, TimeUnit.SECONDS);
        
        if (gotAllResponses) {
            log.info("SUCCESS: Got all expected responses");
        } else {
            log.error("TIMEOUT: Did not receive all expected responses within 5 seconds");
            log.error("Remaining responses to receive: {}", responseLatch.getCount());
        }
        
        // Cleanup
        socket.close();
        System.exit(gotAllResponses ? 0 : 1);
    }
}