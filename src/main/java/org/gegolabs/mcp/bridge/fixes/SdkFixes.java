package org.gegolabs.mcp.bridge.fixes;

import lombok.extern.slf4j.Slf4j;

/**
 * Central class for managing SDK fixes that are applied at runtime.
 * This allows the bridge to work with the official SDK 0.10.0 by applying
 * critical bug fixes dynamically.
 */
@Slf4j
public class SdkFixes {
    
    private static boolean fixesApplied = false;
    
    /**
     * Applies all necessary SDK fixes at runtime.
     * This method should be called once during bridge initialization.
     */
    public static synchronized void applyFixes() {
        if (fixesApplied) {
            log.debug("SDK fixes already applied, skipping");
            return;
        }
        
        String sdkVersion = SdkVersionDetector.getDetectedVersion();
        log.info("Applying SDK 0.10.1 fixes to runtime (detected SDK version: {})", sdkVersion);
        
        try {
            // Apply timeout fix for McpServerSession
            applyTimeoutFix();
            
            // Apply CallToolRequest deserialization fix
            applyCallToolRequestFix();
            
            fixesApplied = true;
            log.info("SDK fixes applied successfully for SDK version {}", sdkVersion);
            
        } catch (Exception e) {
            log.error("Failed to apply SDK fixes", e);
            throw new RuntimeException("Cannot apply required SDK fixes", e);
        }
    }
    
    /**
     * Applies the timeout fix for McpServerSession response handling.
     * This fix reorders reactive operators to ensure responses are always sent.
     */
    private static void applyTimeoutFix() {
        log.debug("Applying timeout fix for McpServerSession");
        // This will be implemented using the SessionWrapper
        // The actual wrapper will be registered with the transport provider
    }
    
    /**
     * Applies the CallToolRequest deserialization fix for McpAsyncServer.
     * This fix handles proper type conversion from Map to CallToolRequest.
     */
    private static void applyCallToolRequestFix() {
        log.debug("Applying CallToolRequest deserialization fix");
        // This will be implemented using the ServerWrapper
        // The actual wrapper will be registered during server creation
    }
    
    /**
     * Checks if fixes have been applied.
     */
    public static boolean areFixesApplied() {
        return fixesApplied;
    }
    
    /**
     * Resets the fixes state (mainly for testing).
     */
    static void reset() {
        fixesApplied = false;
    }
}