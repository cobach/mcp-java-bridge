package org.gegolabs.mcp.bridge.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LoggingUtilsTest {
    
    @Test
    void testInitializeFileLogging() {
        // Initialize file logging
        LoggingUtils.initializeFileLogging("test.log");
        
        // Check that log directory exists
        File logDir = new File(System.getProperty("user.home") + "/.mcp-bridge/logs");
        assertTrue(logDir.exists());
        assertTrue(logDir.isDirectory());
    }
    
    @Test
    void testEnableDebugLogging() {
        LoggingUtils.enableDebugLogging();
        
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        
        assertEquals(Level.DEBUG, rootLogger.getLevel());
    }
    
    @Test
    void testSetLogLevel() {
        String loggerName = "io.modelcontextprotocol.test";
        LoggingUtils.setLogLevel(loggerName, "TRACE");
        
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(loggerName);
        
        assertEquals(Level.TRACE, logger.getLevel());
    }
}