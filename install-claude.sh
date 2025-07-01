#!/bin/bash
#
# MCP Java Bridge - Claude Desktop Installer
# This script installs/updates the bridge connector configuration in Claude Desktop
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
SERVER_NAME=""
HOST="localhost"
PORT="3000"
CONNECTOR_PATH=""

# Function to print colored output
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 -n SERVER_NAME -c CONNECTOR_PATH [-h HOST] [-p PORT]

Install MCP Java Bridge connector in Claude Desktop configuration.

Required arguments:
  -n SERVER_NAME      Name for the server in Claude Desktop
  -c CONNECTOR_PATH   Path to the connector JAR file

Optional arguments:
  -h HOST            Server host (default: localhost)
  -p PORT            Server port (default: 3000)
  --help             Display this help message

Examples:
  $0 -n "my-server" -c /path/to/mcp-connector.jar
  $0 -n "my-server" -c /path/to/mcp-connector.jar -h 0.0.0.0 -p 8080

EOF
    exit 1
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n)
            SERVER_NAME="$2"
            shift 2
            ;;
        -c)
            CONNECTOR_PATH="$2"
            shift 2
            ;;
        -h)
            HOST="$2"
            shift 2
            ;;
        -p)
            PORT="$2"
            shift 2
            ;;
        --help)
            usage
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Validate required arguments
if [ -z "$SERVER_NAME" ]; then
    print_error "Server name is required (-n)"
    usage
fi

if [ -z "$CONNECTOR_PATH" ]; then
    print_error "Connector path is required (-c)"
    usage
fi

# Validate connector JAR exists
if [ ! -f "$CONNECTOR_PATH" ]; then
    print_error "Connector JAR not found: $CONNECTOR_PATH"
    exit 1
fi

# Get absolute path
CONNECTOR_PATH=$(cd "$(dirname "$CONNECTOR_PATH")" && pwd)/$(basename "$CONNECTOR_PATH")

# Detect OS and set config path
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    CONFIG_DIR="$HOME/Library/Application Support/Claude"
    CONFIG_FILE="$CONFIG_DIR/claude_desktop_config.json"
elif [[ "$OSTYPE" == "msys" ]] || [[ "$OSTYPE" == "cygwin" ]] || [[ "$OSTYPE" == "win32" ]]; then
    # Windows
    CONFIG_DIR="$APPDATA/Claude"
    CONFIG_FILE="$CONFIG_DIR/claude_desktop_config.json"
else
    # Linux
    CONFIG_DIR="$HOME/.config/Claude"
    CONFIG_FILE="$CONFIG_DIR/claude_desktop_config.json"
fi

print_info "Detected OS: $OSTYPE"
print_info "Config location: $CONFIG_FILE"

# Create config directory if it doesn't exist
mkdir -p "$CONFIG_DIR"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    print_info "Creating new Claude Desktop configuration..."
    echo '{"mcpServers":{}}' > "$CONFIG_FILE"
fi

# Backup existing config
BACKUP_FILE="$CONFIG_FILE.backup.$(date +%Y%m%d_%H%M%S)"
cp "$CONFIG_FILE" "$BACKUP_FILE"
print_info "Backed up existing config to: $BACKUP_FILE"

# Check if jq is available
if ! command -v jq &> /dev/null; then
    print_warning "jq is not installed. Using Python for JSON manipulation."
    
    # Use Python to update the config
    python3 << EOF
import json
import sys

config_file = "$CONFIG_FILE"
server_name = "$SERVER_NAME"
connector_path = "$CONNECTOR_PATH"
host = "$HOST"
port = "$PORT"

# Read existing config
with open(config_file, 'r') as f:
    config = json.load(f)

# Ensure mcpServers exists
if 'mcpServers' not in config:
    config['mcpServers'] = {}

# Add/update server configuration
config['mcpServers'][server_name] = {
    "command": "java",
    "args": [
        "-jar",
        connector_path,
        host,
        port
    ]
}

# Write updated config
with open(config_file, 'w') as f:
    json.dump(config, f, indent=2)

print(f"Successfully added '{server_name}' to Claude Desktop configuration")
EOF

else
    # Use jq to update the config
    jq --arg name "$SERVER_NAME" \
       --arg jar "$CONNECTOR_PATH" \
       --arg host "$HOST" \
       --arg port "$PORT" \
       '.mcpServers[$name] = {
           "command": "java",
           "args": ["-jar", $jar, $host, $port]
       }' "$CONFIG_FILE" > "$CONFIG_FILE.tmp" && mv "$CONFIG_FILE.tmp" "$CONFIG_FILE"
    
    print_info "Successfully added '$SERVER_NAME' to Claude Desktop configuration"
fi

# Display the configuration
print_info "Configuration added:"
echo "  Name: $SERVER_NAME"
echo "  Connector: $CONNECTOR_PATH"
echo "  Host: $HOST"
echo "  Port: $PORT"

print_info "✅ Installation complete!"
print_info ""
print_info "Next steps:"
print_info "1. Start your MCP server on $HOST:$PORT"
print_info "2. Restart Claude Desktop to connect to your server"
print_info ""
print_info "To verify the installation, check: $CONFIG_FILE"