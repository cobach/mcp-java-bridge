package org.gegolabs.mcp.bridge.client;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Bridge stub that runs on the client side (Claude Desktop).
 * It connects to the TCP server and bridges stdio to TCP.
 * 
 * Usage: java -jar mcp-bridge-stub.jar <host> <port>
 */
@Slf4j
public class BridgeStub {
    
    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader tcpReader;
    private PrintWriter tcpWriter;
    private BufferedReader stdinReader;
    private PrintWriter stdoutWriter;
    private volatile boolean running = true;
    
    public BridgeStub(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void start() throws IOException {
        // Connect to TCP server
        socket = new Socket(host, port);
        tcpReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        tcpWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        
        // Set up stdio
        stdinReader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        stdoutWriter = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        
        // Start forwarding threads
        Thread stdinToTcp = new Thread(this::forwardStdinToTcp, "stdin-to-tcp");
        Thread tcpToStdout = new Thread(this::forwardTcpToStdout, "tcp-to-stdout");
        
        stdinToTcp.start();
        tcpToStdout.start();
        
        // Wait for threads to complete
        try {
            stdinToTcp.join();
            tcpToStdout.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void forwardStdinToTcp() {
        try {
            String line;
            while (running && (line = stdinReader.readLine()) != null) {
                tcpWriter.println(line);
                tcpWriter.flush();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error reading from stdin: " + e.getMessage());
            }
        } finally {
            shutdown();
        }
    }
    
    private void forwardTcpToStdout() {
        try {
            String line;
            while (running && (line = tcpReader.readLine()) != null) {
                stdoutWriter.println(line);
                stdoutWriter.flush();
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error reading from TCP: " + e.getMessage());
            }
        } finally {
            shutdown();
        }
    }
    
    private void shutdown() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore
        }
    }
    
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar mcp-bridge-stub.jar <host> <port>");
            System.exit(1);
        }
        
        String host = args[0];
        int port;
        
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            System.exit(1);
            return;
        }
        
        try {
            BridgeStub stub = new BridgeStub(host, port);
            stub.start();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            System.exit(1);
        }
    }
}