package org.gegolabs.mcp.bridge.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for configuring logging in MCP Bridge applications.
 * Migrated and adapted from uMCP's MiscTools.
 */
public class LoggingUtils {
    
    /**
     * Initializes file-based logging for the MCP Bridge.
     * This is essential for debugging MCP communication issues.
     * 
     * @param filename The name of the log file (without path)
     */
    public static void initializeFileLogging(String filename) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
        fileAppender.setContext(loggerContext);
        fileAppender.setName("FILE");
        
        // Log to user's home directory under .mcp-bridge/logs
        Path logDir = Paths.get(System.getProperty("user.home"), ".mcp-bridge", "logs");
        Path logFile = logDir.resolve(filename);
        
        // Create directory if it doesn't exist
        logDir.toFile().mkdirs();
        
        fileAppender.setFile(logFile.toString());
        
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder.start();
        
        fileAppender.setEncoder(encoder);
        fileAppender.start();
        
        // Add appender to root logger
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(fileAppender);
        rootLogger.setLevel(Level.DEBUG);
        
        // Log initial message
        org.slf4j.Logger log = LoggerFactory.getLogger(LoggingUtils.class);
        log.info("Logging initialized to file: {}", logFile);
    }
    
    /**
     * Configures logging level for specific packages.
     * 
     * @param packageName The package name to configure
     * @param level The logging level (DEBUG, INFO, WARN, ERROR)
     */
    public static void setLogLevel(String packageName, String level) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(packageName);
        logger.setLevel(Level.toLevel(level));
    }
    
    /**
     * Enables debug logging for MCP Bridge components.
     */
    public static void enableDebugLogging() {
        setLogLevel("org.gegolabs.mcp.bridge", "DEBUG");
        setLogLevel("io.modelcontextprotocol.sdk", "DEBUG");
    }
}