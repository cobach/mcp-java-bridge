package org.gegolabs.mcp.bridge;

import org.gegolabs.mcp.bridge.client.BridgeStub;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for MCP Java Bridge CLI.
 * 
 * Behaviors:
 * 1. No arguments: Installs itself as connector in Claude Desktop
 * 2. --connector: Runs as connector with default or specified host/port
 * 3. install command: Legacy install command for backward compatibility
 */
public class Main {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3000";
    
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                // No arguments: Install self as connector
                installSelfAsConnector();
            } else if ("--connector".equals(args[0])) {
                // Run as connector
                runAsConnector(args);
            } else if ("install".equals(args[0])) {
                // Legacy install command
                String[] installArgs = new String[args.length - 1];
                System.arraycopy(args, 1, installArgs, 0, args.length - 1);
                ClaudeInstaller.main(installArgs);
            } else if ("--help".equals(args[0]) || "-h".equals(args[0]) || "help".equals(args[0])) {
                printUsage();
            } else {
                // Unknown command
                System.err.println("Unknown command: " + args[0]);
                printUsage();
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void installSelfAsConnector() throws Exception {
        System.out.println("MCP Java Bridge - Auto-Installation");
        System.out.println("===================================");
        System.out.println();
        
        // Get the path to this JAR
        String jarPath = getJarPath();
        if (jarPath == null) {
            System.err.println("Error: Could not determine JAR location");
            System.err.println("Please run with explicit install command:");
            System.err.println("  java -jar mcp-java-bridge.jar install -n <name> -c <jar-path>");
            System.exit(1);
        }
        
        System.out.println("Detected JAR location: " + jarPath);
        System.out.println();
        
        // Prompt for server name
        System.out.print("Enter a name for your MCP server [default: mcp-server]: ");
        String serverName = System.console() != null ? System.console().readLine() : "";
        if (serverName == null || serverName.trim().isEmpty()) {
            serverName = "mcp-server";
        }
        
        // Prompt for host
        System.out.print("Enter server host [default: " + DEFAULT_HOST + "]: ");
        String host = System.console() != null ? System.console().readLine() : "";
        if (host == null || host.trim().isEmpty()) {
            host = DEFAULT_HOST;
        }
        
        // Prompt for port
        System.out.print("Enter server port [default: " + DEFAULT_PORT + "]: ");
        String port = System.console() != null ? System.console().readLine() : "";
        if (port == null || port.trim().isEmpty()) {
            port = DEFAULT_PORT;
        }
        
        System.out.println();
        System.out.println("Installing with configuration:");
        System.out.println("  Name: " + serverName);
        System.out.println("  JAR: " + jarPath);
        System.out.println("  Host: " + host);
        System.out.println("  Port: " + port);
        System.out.println();
        
        // Install using the ClaudeInstaller
        String[] installArgs = {"-n", serverName, "-c", jarPath, "-h", host, "-p", port};
        ClaudeInstaller.main(installArgs);
    }
    
    private static void runAsConnector(String[] args) throws Exception {
        String host = DEFAULT_HOST;
        String port = DEFAULT_PORT;
        
        // Parse additional arguments
        if (args.length > 1) {
            host = args[1];
        }
        if (args.length > 2) {
            port = args[2];
        }
        
        System.err.println("[MCP Bridge Connector] Connecting to " + host + ":" + port);
        
        // Run the BridgeStub (connector)
        BridgeStub.main(new String[]{host, port});
    }
    
    private static String getJarPath() {
        try {
            // Try to get the JAR path from the class location
            String className = Main.class.getName().replace('.', '/') + ".class";
            String classPath = Main.class.getClassLoader().getResource(className).toString();
            
            if (classPath.startsWith("jar:")) {
                // Extract JAR path from jar:file:/path/to/jar.jar!/package/Class.class
                String jarPath = classPath.substring(4, classPath.indexOf("!"));
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring(5);
                }
                // Handle Windows paths
                if (System.getProperty("os.name").toLowerCase().contains("win") && jarPath.startsWith("/")) {
                    jarPath = jarPath.substring(1);
                }
                return new File(jarPath).getAbsolutePath();
            }
            
            // Fallback: try system property
            String jarFile = System.getProperty("java.class.path");
            if (jarFile != null && jarFile.endsWith(".jar")) {
                return new File(jarFile).getAbsolutePath();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static void printUsage() {
        System.out.println("MCP Java Bridge - Runtime decoupling for MCP Java servers");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar mcp-java-bridge.jar                    # Install as connector (interactive)");
        System.out.println("  java -jar mcp-java-bridge.jar --connector [host] [port]  # Run as connector");
        System.out.println("  java -jar mcp-java-bridge.jar install <options>  # Manual install");
        System.out.println("  java -jar mcp-java-bridge.jar --help            # Show this help");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Interactive installation");
        System.out.println("  java -jar mcp-java-bridge.jar");
        System.out.println();
        System.out.println("  # Run as connector with defaults (localhost:3000)");
        System.out.println("  java -jar mcp-java-bridge.jar --connector");
        System.out.println();
        System.out.println("  # Run as connector with custom host/port");
        System.out.println("  java -jar mcp-java-bridge.jar --connector 192.168.1.100 8080");
        System.out.println();
        System.out.println("  # Manual installation");
        System.out.println("  java -jar mcp-java-bridge.jar install -n \"my-server\" -c ./mcp-java-bridge.jar -h localhost -p 3000");
    }
}