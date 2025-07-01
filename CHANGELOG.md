# Changelog

All notable changes to MCP Java Bridge will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial implementation of TCP transport bridge for MCP Java SDK
- BridgeTransportProvider for TCP server socket management
- BridgeTransport for stdio↔TCP protocol conversion
- BridgeStub client for Claude Desktop compatibility
- Static factory methods in McpBridge for easy configuration
- Builder pattern support for advanced configuration
- LoggingUtils for file-based debug logging
- JsonSchemaUtils for parameter schema generation
- Example implementations (SimpleExample, ExampleServer)
- Comprehensive documentation and API reference

### Technical Details
- Compatible with MCP Java SDK 0.11.0-SNAPSHOT
- Implements MCP transport protocol specification
- Thread-safe message handling with synchronized writes
- Reactive programming with Project Reactor
- Automatic JSON-RPC message serialization/deserialization
- Connection pooling with dedicated threads per client
- Graceful shutdown and error recovery

## [1.0.0] - TBD

### Notes
- First stable release
- Production-ready TCP transport support
- Full compatibility with Claude Desktop app
- Comprehensive test coverage

---

## Version History Format

### [Version] - YYYY-MM-DD

#### Added
- New features

#### Changed
- Changes in existing functionality

#### Deprecated
- Soon-to-be removed features

#### Removed
- Removed features

#### Fixed
- Bug fixes

#### Security
- Security vulnerability fixes