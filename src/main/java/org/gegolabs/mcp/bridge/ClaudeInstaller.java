package org.gegolabs.mcp.bridge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Command-line tool to install MCP Java Bridge connector in Claude Desktop configuration.
 */
public class ClaudeInstaller {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3000";
    
    public static void main(String[] args) {
        if (args.length < 2 || "--help".equals(args[0])) {
            printUsage();
            System.exit(args.length < 2 ? 1 : 0);
        }
        
        // Parse arguments
        String serverName = null;
        String connectorPath = null;
        String host = DEFAULT_HOST;
        String port = DEFAULT_PORT;
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n":
                    if (i + 1 < args.length) {
                        serverName = args[++i];
                    }
                    break;
                case "-c":
                    if (i + 1 < args.length) {
                        connectorPath = args[++i];
                    }
                    break;
                case "-h":
                    if (i + 1 < args.length) {
                        host = args[++i];
                    }
                    break;
                case "-p":
                    if (i + 1 < args.length) {
                        port = args[++i];
                    }
                    break;
            }
        }
        
        // Validate required arguments
        if (serverName == null) {
            System.err.println("[ERROR] Server name is required (-n)");
            printUsage();
            System.exit(1);
        }
        
        if (connectorPath == null) {
            System.err.println("[ERROR] Connector path is required (-c)");
            printUsage();
            System.exit(1);
        }
        
        // Validate connector exists
        File connectorFile = new File(connectorPath);
        if (!connectorFile.exists()) {
            System.err.println("[ERROR] Connector JAR not found: " + connectorPath);
            System.exit(1);
        }
        
        // Get absolute path
        connectorPath = connectorFile.getAbsolutePath();
        
        try {
            // Find Claude config file
            Path configPath = findClaudeConfig();
            System.out.println("[INFO] Config location: " + configPath);
            
            // Create config directory if needed
            Files.createDirectories(configPath.getParent());
            
            // Create default config if doesn't exist
            if (!Files.exists(configPath)) {
                System.out.println("[INFO] Creating new Claude Desktop configuration...");
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode root = mapper.createObjectNode();
                root.putObject("mcpServers");
                mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), root);
            }
            
            // Backup existing config
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path backupPath = Paths.get(configPath + ".backup." + timestamp);
            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[INFO] Backed up existing config to: " + backupPath);
            
            // Read and update config
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            
            JsonNode root = mapper.readTree(configPath.toFile());
            ObjectNode rootNode = (ObjectNode) root;
            
            // Ensure mcpServers exists
            if (!rootNode.has("mcpServers")) {
                rootNode.putObject("mcpServers");
            }
            
            ObjectNode servers = (ObjectNode) rootNode.get("mcpServers");
            
            // Create server configuration
            ObjectNode serverConfig = mapper.createObjectNode();
            
            // Check if we're installing the main JAR (which has --connector mode)
            boolean isMainJar = connectorPath.endsWith("mcp-java-bridge-1.0.0-SNAPSHOT.jar") || 
                               connectorPath.endsWith("mcp-java-bridge.jar");
            
            if (isMainJar) {
                // Use --connector mode for the main JAR
                serverConfig.put("command", "java");
                serverConfig.putArray("args")
                    .add("-jar")
                    .add(connectorPath)
                    .add("--connector")
                    .add(host)
                    .add(port);
            } else {
                // Legacy mode for separate connector JARs
                serverConfig.put("command", "java");
                serverConfig.putArray("args")
                    .add("-jar")
                    .add(connectorPath)
                    .add(host)
                    .add(port);
            }
            
            // Add server to config
            servers.set(serverName, serverConfig);
            
            // Write updated config
            mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), rootNode);
            
            System.out.println("[INFO] Successfully added '" + serverName + "' to Claude Desktop configuration");
            System.out.println("[INFO] Configuration added:");
            System.out.println("  Name: " + serverName);
            System.out.println("  Connector: " + connectorPath);
            System.out.println("  Host: " + host);
            System.out.println("  Port: " + port);
            System.out.println();
            System.out.println("[INFO] ✅ Installation complete!");
            System.out.println();
            System.out.println("[INFO] Next steps:");
            System.out.println("1. Start your MCP server on " + host + ":" + port);
            System.out.println("2. Restart Claude Desktop to connect to your server");
            System.out.println();
            System.out.println("To verify the installation, check: " + configPath);
            
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to update configuration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static Path findClaudeConfig() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");
        
        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "Claude", "claude_desktop_config.json");
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null) {
                appData = Paths.get(home, "AppData", "Roaming").toString();
            }
            return Paths.get(appData, "Claude", "claude_desktop_config.json");
        } else {
            // Linux and others
            return Paths.get(home, ".config", "Claude", "claude_desktop_config.json");
        }
    }
    
    private static void printUsage() {
        System.out.println("Usage: java -jar mcp-java-bridge.jar install -n SERVER_NAME -c JAR_PATH [-h HOST] [-p PORT]");
        System.out.println();
        System.out.println("Install MCP Java Bridge connector in Claude Desktop configuration.");
        System.out.println();
        System.out.println("Required arguments:");
        System.out.println("  -n SERVER_NAME      Name for the server in Claude Desktop");
        System.out.println("  -c JAR_PATH         Path to the JAR file (typically mcp-java-bridge.jar itself)");
        System.out.println();
        System.out.println("Optional arguments:");
        System.out.println("  -h HOST            Server host (default: localhost)");
        System.out.println("  -p PORT            Server port (default: 3000)");
        System.out.println("  --help             Display this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar mcp-java-bridge.jar install -n \"my-server\" -c ./mcp-java-bridge.jar");
        System.out.println("  java -jar mcp-java-bridge.jar install -n \"my-server\" -c ./mcp-java-bridge.jar -h 0.0.0.0 -p 8080");
    }
}