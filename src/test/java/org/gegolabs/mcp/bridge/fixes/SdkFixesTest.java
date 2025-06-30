package org.gegolabs.mcp.bridge.fixes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SdkFixesTest {
    
    @BeforeEach
    void setUp() {
        // Reset fixes state before each test
        SdkFixes.reset();
    }
    
    @Test
    void testApplyFixesOnce() {
        assertFalse(SdkFixes.areFixesApplied());
        
        SdkFixes.applyFixes();
        assertTrue(SdkFixes.areFixesApplied());
        
        // Should not throw when called again
        assertDoesNotThrow(() -> SdkFixes.applyFixes());
        assertTrue(SdkFixes.areFixesApplied());
    }
    
    @Test
    void testFixesNotAppliedByDefault() {
        assertFalse(SdkFixes.areFixesApplied());
    }
}