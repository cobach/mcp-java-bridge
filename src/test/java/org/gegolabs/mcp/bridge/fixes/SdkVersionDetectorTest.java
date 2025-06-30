package org.gegolabs.mcp.bridge.fixes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdkVersionDetectorTest {
    
    @BeforeEach
    void setUp() {
        // Reset detector state before each test
        SdkVersionDetector.reset();
        // Clear system property
        System.clearProperty("mcp.bridge.apply.fixes");
    }
    
    @Test
    void testVersionDetection() {
        // We can't easily test the actual version detection without mocking
        // but we can test that it returns a non-null value
        String version = SdkVersionDetector.getDetectedVersion();
        assertNotNull(version);
        
        // The version should be cached
        String version2 = SdkVersionDetector.getDetectedVersion();
        assertSame(version, version2);
    }
    
    @Test
    void testSystemPropertyOverride() {
        // Test that system property overrides version detection
        System.setProperty("mcp.bridge.apply.fixes", "false");
        assertFalse(SdkVersionDetector.shouldApplyFixes());
        
        // Reset and test with true
        SdkVersionDetector.reset();
        System.setProperty("mcp.bridge.apply.fixes", "true");
        assertTrue(SdkVersionDetector.shouldApplyFixes());
    }
    
    @Test
    void testShouldApplyFixesConsistency() {
        // shouldApplyFixes should return consistent results
        boolean first = SdkVersionDetector.shouldApplyFixes();
        boolean second = SdkVersionDetector.shouldApplyFixes();
        assertEquals(first, second);
    }
    
    @Test
    void testDefaultBehaviorWith010() {
        // When we're actually using 0.10.0, it should detect and apply fixes
        // This test will pass when using SDK 0.10.0
        String version = SdkVersionDetector.getDetectedVersion();
        boolean shouldApply = SdkVersionDetector.shouldApplyFixes();
        
        // Log the detected version for debugging
        System.out.println("Detected SDK version: " + version);
        System.out.println("Should apply fixes: " + shouldApply);
        
        // If version is detected as 0.10.0, fixes should be applied
        if ("0.10.0".equals(version)) {
            assertTrue(shouldApply);
        }
    }
}