package org.gegolabs.mcp.bridge.fixes;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Detects the version of the MCP SDK at runtime to determine if fixes should be applied.
 */
@Slf4j
public class SdkVersionDetector {
    
    private static final String TARGET_VERSION = "0.10.0";
    private static String detectedVersion = null;
    private static Boolean shouldApplyFixes = null;
    
    /**
     * Detects the MCP SDK version and determines if fixes should be applied.
     * 
     * @return true if using SDK 0.10.0 and fixes should be applied
     */
    public static boolean shouldApplyFixes() {
        // Check if already determined
        if (shouldApplyFixes != null) {
            return shouldApplyFixes;
        }
        
        // First check system property override
        String propertyOverride = System.getProperty("mcp.bridge.apply.fixes");
        if (propertyOverride != null) {
            shouldApplyFixes = Boolean.parseBoolean(propertyOverride);
            log.info("SDK fixes override via system property: {}", shouldApplyFixes);
            return shouldApplyFixes;
        }
        
        // Detect SDK version
        String version = detectSdkVersion();
        
        // Log detection attempt
        log.debug("Attempting to detect MCP SDK version...");
        
        // Determine if fixes are needed
        if (TARGET_VERSION.equals(version)) {
            shouldApplyFixes = true;
            log.info("Detected MCP SDK version {} - fixes will be applied", version);
        } else {
            shouldApplyFixes = false;
            log.info("Detected MCP SDK version {} - fixes not needed", version);
        }
        
        return shouldApplyFixes;
    }
    
    /**
     * Gets the detected SDK version.
     * 
     * @return the SDK version or "unknown" if not detected
     */
    public static String getDetectedVersion() {
        if (detectedVersion == null) {
            detectedVersion = detectSdkVersion();
        }
        return detectedVersion;
    }
    
    /**
     * Detects the actual SDK version from the classpath.
     */
    private static String detectSdkVersion() {
        String version = null;
        
        // Method 1: Try to read from Maven properties in the JAR
        version = detectFromMavenProperties();
        if (version != null) {
            log.debug("Detected version from Maven properties: {}", version);
            return version;
        }
        
        // Method 2: Try to read from MANIFEST.MF
        version = detectFromManifest();
        if (version != null) {
            log.debug("Detected version from MANIFEST.MF: {}", version);
            return version;
        }
        
        // Method 3: Try to detect from class metadata
        version = detectFromClassMetadata();
        if (version != null) {
            log.debug("Detected version from class metadata: {}", version);
            return version;
        }
        
        log.warn("Could not detect MCP SDK version - assuming fixes are needed");
        return "unknown";
    }
    
    /**
     * Try to detect version from Maven properties file in the JAR.
     */
    private static String detectFromMavenProperties() {
        try {
            String resourcePath = "/META-INF/maven/io.modelcontextprotocol.sdk/mcp/pom.properties";
            InputStream is = SdkVersionDetector.class.getResourceAsStream(resourcePath);
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                is.close();
                return props.getProperty("version");
            }
        } catch (Exception e) {
            log.debug("Could not read Maven properties: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Try to detect version from MANIFEST.MF.
     */
    private static String detectFromManifest() {
        try {
            Class<?> clazz = McpSchema.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            
            if (!classPath.startsWith("jar")) {
                // Not running from a JAR
                return null;
            }
            
            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            Manifest manifest = new Manifest(new java.net.URL(manifestPath).openStream());
            Attributes attr = manifest.getMainAttributes();
            
            // Try different version attributes
            String version = attr.getValue("Implementation-Version");
            if (version == null) {
                version = attr.getValue("Bundle-Version");
            }
            
            return version;
        } catch (Exception e) {
            log.debug("Could not read MANIFEST.MF: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Try to detect version from class metadata or known version markers.
     */
    private static String detectFromClassMetadata() {
        try {
            // Check if certain classes or methods exist that are version-specific
            // For now, we'll check the package structure
            Package pkg = McpSchema.class.getPackage();
            if (pkg != null) {
                String implVersion = pkg.getImplementationVersion();
                if (implVersion != null) {
                    return implVersion;
                }
            }
        } catch (Exception e) {
            log.debug("Could not read class metadata: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Resets the detection cache (mainly for testing).
     */
    static void reset() {
        detectedVersion = null;
        shouldApplyFixes = null;
    }
}