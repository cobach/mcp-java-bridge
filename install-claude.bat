@echo off
rem
rem MCP Java Bridge - Claude Desktop Installer for Windows
rem This script installs/updates the bridge connector configuration in Claude Desktop
rem

setlocal enabledelayedexpansion

rem Default values
set "SERVER_NAME="
set "HOST=localhost"
set "PORT=3000"
set "CONNECTOR_PATH="

rem Parse command line arguments
:parse_args
if "%~1"=="" goto validate_args
if /i "%~1"=="-n" (
    set "SERVER_NAME=%~2"
    shift
    shift
    goto parse_args
)
if /i "%~1"=="-c" (
    set "CONNECTOR_PATH=%~2"
    shift
    shift
    goto parse_args
)
if /i "%~1"=="-h" (
    set "HOST=%~2"
    shift
    shift
    goto parse_args
)
if /i "%~1"=="-p" (
    set "PORT=%~2"
    shift
    shift
    goto parse_args
)
if /i "%~1"=="--help" goto usage
echo [ERROR] Unknown option: %~1
goto usage

:usage
echo Usage: %~nx0 -n SERVER_NAME -c CONNECTOR_PATH [-h HOST] [-p PORT]
echo.
echo Install MCP Java Bridge connector in Claude Desktop configuration.
echo.
echo Required arguments:
echo   -n SERVER_NAME      Name for the server in Claude Desktop
echo   -c CONNECTOR_PATH   Path to the connector JAR file
echo.
echo Optional arguments:
echo   -h HOST            Server host (default: localhost)
echo   -p PORT            Server port (default: 3000)
echo   --help             Display this help message
echo.
echo Examples:
echo   %~nx0 -n "my-server" -c C:\path\to\mcp-connector.jar
echo   %~nx0 -n "my-server" -c C:\path\to\mcp-connector.jar -h 0.0.0.0 -p 8080
exit /b 1

:validate_args
if "%SERVER_NAME%"=="" (
    echo [ERROR] Server name is required (-n^)
    goto usage
)
if "%CONNECTOR_PATH%"=="" (
    echo [ERROR] Connector path is required (-c^)
    goto usage
)

rem Validate connector JAR exists
if not exist "%CONNECTOR_PATH%" (
    echo [ERROR] Connector JAR not found: %CONNECTOR_PATH%
    exit /b 1
)

rem Get absolute path
for %%I in ("%CONNECTOR_PATH%") do set "CONNECTOR_PATH=%%~fI"

rem Set config file path
set "CONFIG_DIR=%APPDATA%\Claude"
set "CONFIG_FILE=%CONFIG_DIR%\claude_desktop_config.json"

echo [INFO] Config location: %CONFIG_FILE%

rem Create config directory if it doesn't exist
if not exist "%CONFIG_DIR%" mkdir "%CONFIG_DIR%"

rem Check if config file exists
if not exist "%CONFIG_FILE%" (
    echo [INFO] Creating new Claude Desktop configuration...
    echo {"mcpServers":{}} > "%CONFIG_FILE%"
)

rem Backup existing config
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do set date=%%c%%a%%b
for /f "tokens=1-2 delims=: " %%a in ('time /t') do set time=%%a%%b
set "BACKUP_FILE=%CONFIG_FILE%.backup.%date%_%time: =0%"
copy "%CONFIG_FILE%" "%BACKUP_FILE%" >nul
echo [INFO] Backed up existing config to: %BACKUP_FILE%

rem Create Python script to update JSON
set "PYTHON_SCRIPT=%TEMP%\update_claude_config.py"
(
echo import json
echo import sys
echo.
echo config_file = r"%CONFIG_FILE%"
echo server_name = "%SERVER_NAME%"
echo connector_path = r"%CONNECTOR_PATH%"
echo host = "%HOST%"
echo port = "%PORT%"
echo.
echo # Read existing config
echo with open(config_file, 'r'^) as f:
echo     config = json.load(f^)
echo.
echo # Ensure mcpServers exists
echo if 'mcpServers' not in config:
echo     config['mcpServers'] = {}
echo.
echo # Add/update server configuration
echo config['mcpServers'][server_name] = {
echo     "command": "java",
echo     "args": [
echo         "-jar",
echo         connector_path,
echo         host,
echo         port
echo     ]
echo }
echo.
echo # Write updated config
echo with open(config_file, 'w'^) as f:
echo     json.dump(config, f, indent=2^)
echo.
echo print(f"Successfully added '{server_name}' to Claude Desktop configuration"^)
) > "%PYTHON_SCRIPT%"

rem Run Python script
python "%PYTHON_SCRIPT%"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to update configuration. Make sure Python is installed.
    del "%PYTHON_SCRIPT%"
    exit /b 1
)

rem Clean up
del "%PYTHON_SCRIPT%"

rem Display the configuration
echo [INFO] Configuration added:
echo   Name: %SERVER_NAME%
echo   Connector: %CONNECTOR_PATH%
echo   Host: %HOST%
echo   Port: %PORT%

echo.
echo [INFO] Installation complete!
echo.
echo Next steps:
echo 1. Start your MCP server on %HOST%:%PORT%
echo 2. Restart Claude Desktop to connect to your server
echo.
echo To verify the installation, check: %CONFIG_FILE%