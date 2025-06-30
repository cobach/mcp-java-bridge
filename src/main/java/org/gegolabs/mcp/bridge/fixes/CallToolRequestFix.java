package org.gegolabs.mcp.bridge.fixes;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Fix for CallToolRequest deserialization issue in SDK 0.10.0.
 * 
 * The problem: JSON-RPC deserializes params as LinkedHashMap, but the SDK
 * expects it to be CallToolRequest, causing ClassCastException.
 * 
 * The solution: This class provides utility methods to safely convert
 * the params to CallToolRequest.
 */
@Slf4j
public class CallToolRequestFix {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Safely converts params object to CallToolRequest.
     * This handles the case where JSON-RPC provides params as a Map.
     * 
     * @param params The params object from JSON-RPC
     * @return The converted CallToolRequest
     * @throws IllegalArgumentException if conversion fails
     */
    public static McpSchema.CallToolRequest convertToCallToolRequest(Object params) {
        if (params == null) {
            throw new IllegalArgumentException("Params cannot be null for tools/call");
        }
        
        // If already the correct type (unlikely but safe to check)
        if (params instanceof McpSchema.CallToolRequest) {
            return (McpSchema.CallToolRequest) params;
        }
        
        // Expected case - params is a Map
        if (params instanceof Map) {
            try {
                log.debug("Converting Map params to CallToolRequest");
                return objectMapper.convertValue(params, McpSchema.CallToolRequest.class);
            } catch (Exception e) {
                log.error("Failed to convert Map to CallToolRequest: {}", params, e);
                throw new IllegalArgumentException("Invalid params for tools/call: " + params, e);
            }
        }
        
        // Unknown type - try generic conversion
        try {
            log.warn("Attempting generic conversion for params type: {}", params.getClass());
            return objectMapper.convertValue(params, McpSchema.CallToolRequest.class);
        } catch (Exception e) {
            log.error("Failed to convert params to CallToolRequest: {}", params, e);
            throw new IllegalArgumentException("Cannot convert params to CallToolRequest: " + params, e);
        }
    }
    
    /**
     * Checks if the params object needs conversion.
     * 
     * @param params The params object to check
     * @return true if conversion is needed
     */
    public static boolean needsConversion(Object params) {
        return !(params instanceof McpSchema.CallToolRequest);
    }
}