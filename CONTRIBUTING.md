# Contributing to MCP Java Bridge

We welcome contributions to the MCP Java Bridge project! This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites

- Java 17 or higher
- Gradle 8.5 or higher
- Git

### Getting Started

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/mcp-java-bridge.git
   cd mcp-java-bridge
   ```

3. Build the project:
   ```bash
   ./gradlew build
   ```

4. Run tests:
   ```bash
   ./gradlew test
   ```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Your Changes

- Follow the existing code style
- Add tests for new functionality
- Update documentation as needed

### 3. Run Tests

```bash
./gradlew test
```

### 4. Commit Your Changes

Follow the conventional commit format:

```
<type>: <description>

<detailed explanation if needed>

🤖 Generated with Claude Wing Coding support (https://claude.ai)
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a pull request on GitHub.

## Code Style Guidelines

### Java Code

- Use 4 spaces for indentation
- Follow standard Java naming conventions
- Use Lombok annotations where appropriate
- Add Javadoc for public APIs

### Package Structure

```
org.gegolabs.mcp.bridge
├── transport/     # Transport implementations
├── client/        # Client-side components
├── utils/         # Utility classes
└── examples/      # Example implementations
```

### Logging

- Use SLF4J with `@Slf4j` annotation
- Log levels:
  - ERROR: Unrecoverable errors
  - WARN: Recoverable errors, deprecations
  - INFO: Important state changes
  - DEBUG: Detailed flow information
  - TRACE: Very detailed debugging

## Testing Guidelines

### Unit Tests

- Test class naming: `<ClassName>Test`
- Use JUnit 5 (Jupiter)
- Use Mockito for mocking
- Aim for >80% code coverage

Example:
```java
@Test
void testTcpTransportCreation() {
    var transport = McpBridge.tcpTransport(3000);
    assertNotNull(transport);
    assertInstanceOf(BridgeTransportProvider.class, transport);
}
```

### Integration Tests

- Test actual TCP connections
- Test stdio↔TCP conversion
- Test error scenarios

## Documentation

### Code Documentation

- Add Javadoc to all public classes and methods
- Include examples in Javadoc where helpful
- Document thrown exceptions

### README Updates

Update README.md when:
- Adding new features
- Changing API
- Adding configuration options

### API Documentation

Update docs/API.md when:
- Adding new public APIs
- Changing method signatures
- Adding new utility classes

## Pull Request Process

1. **Description**: Clearly describe what the PR does
2. **Testing**: Describe how you tested the changes
3. **Breaking Changes**: Note any breaking changes
4. **Related Issues**: Link related issues with "Fixes #123"

### PR Template

```markdown
## Description
Brief description of changes

## Problem
What issue does this solve?

## Solution
How does this solve it?

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
```

## Release Process

1. Update version in `build.gradle`
2. Update CHANGELOG.md
3. Create release tag: `v1.0.0`
4. Build and publish artifacts

## Getting Help

- Open an issue for bugs
- Start a discussion for features
- Join our community chat (if available)

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers
- Focus on constructive criticism
- Follow the project's code of conduct

Thank you for contributing to MCP Java Bridge!