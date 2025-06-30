#!/bin/bash

# MCP Java Bridge Demo Runner
# This script helps run the demo server and test it

echo "MCP Java Bridge Demo"
echo "===================="
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt "17" ]; then
    echo "Error: Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

# Build the project if not already built
if [ ! -f "../build/libs/mcp-java-bridge-1.0.0-SNAPSHOT-example.jar" ]; then
    echo "Building the project..."
    cd .. && ./gradlew build && cd demo
fi

echo "Starting MCP Example Server on port 3000..."
echo "Press Ctrl+C to stop the server"
echo
echo "To test with Claude Desktop:"
echo "1. Add this to your claude_desktop_config.json:"
echo '   {
     "mcpServers": {
       "example-server": {
         "command": "java",
         "args": [
           "-jar",
           "'$(pwd)'/../build/libs/mcp-java-bridge-1.0.0-SNAPSHOT-connector.jar",
           "localhost",
           "3000"
         ]
       }
     }
   }'
echo
echo "2. Restart Claude Desktop"
echo "3. Use tools like: echo, get_time, todo_list, key_value_store, calculator"
echo
echo "Starting server..."
echo "=================="

# Run the example server
java -jar ../build/libs/mcp-java-bridge-1.0.0-SNAPSHOT-example.jar