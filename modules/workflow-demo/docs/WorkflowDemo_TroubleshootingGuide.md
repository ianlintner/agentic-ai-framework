# Workflow Demo Troubleshooting Guide

**Author:** Documentation Team  
**Date:** April 19, 2025  
**Version:** 1.0.0

## Overview

This guide provides solutions for common issues encountered when running the Workflow Demo. The Workflow Demo showcases the Agentic AI Framework's ability to connect specialized agents in a workflow to perform complex tasks.

## Known Configuration Issues

### ZIO HTTP Version Compatibility

The Workflow Demo uses ZIO HTTP 3.0.0-RC4, which differs from the version used in other modules (3.0.0-RC2). This inconsistency can cause compatibility issues when integrating with other modules.

**Current Status:** The module has been temporarily configured to work independently of core and memory dependencies to ensure functionality.

**Solution:** If you encounter HTTP-related errors, try the following:

1. Use the direct file access method described in the README instead of accessing through the server.
2. If you need to modify the server, ensure you maintain the ZIO HTTP 3.0.0-RC4 dependency in this module.

### Module Dependencies

The workflow-demo module has been temporarily decoupled from core and memory dependencies to ensure it works correctly. This is noted in the build.sbt file:

```scala
// Temporarily remove dependencies on core and memory to make the demo work
// In a production setup, we'd properly configure the module dependencies
```

**Solution:** If you need to integrate with core or memory modules, you'll need to carefully manage the dependencies to avoid version conflicts.

## Common Issues and Solutions

### Error: Failed to fetch

This error typically appears in the browser console when running a workflow and indicates that the frontend cannot connect to the backend API.

**Possible Causes:**

1. The workflow REST API server is not running
2. There's a CORS issue preventing the frontend from connecting to the backend
3. The frontend is using incorrect API endpoints or port
4. The ZIO HTTP server has compatibility issues

**Solutions:**

1. **Server Not Running**
   - Ensure the server is running by executing `./modules/workflow-demo/run-workflow-demo.sh`
   - Check the terminal output for any startup errors
   - Verify the server is listening on port 8083

2. **CORS Issues**
   - Open the browser console (F12) and look for specific CORS error messages
   - If using Chrome, try launching with CORS disabled for testing:
     ```
     chrome --disable-web-security --user-data-dir="/tmp/chrome-dev"
     ```
   - Alternatively, use the direct file access method which doesn't require the server

3. **Incorrect Endpoints**
   - Check the browser console for the exact API calls being made
   - Verify the frontend is using `http://localhost:8083/api/...` for all API calls
   - If you've modified the code, ensure the port numbers match between frontend and backend

4. **ZIO HTTP Compatibility**
   - Use the direct file access method which doesn't rely on the server
   - If server functionality is required, consider using mock data for development

### Workflow Execution Hangs

If a workflow starts but never completes, it may be stuck in execution.

**Possible Causes:**

1. An agent in the workflow is waiting for a response that never arrives
2. There's a deadlock in the workflow execution
3. The server is overloaded

**Solutions:**

1. **Check Agent Status**
   - Use the `/api/workflow/status/:id` endpoint to check the current status
   - Look for agents that are in a "Running" state for an extended period

2. **Restart the Server**
   - Stop the server (Ctrl+C in the terminal)
   - Restart with `./modules/workflow-demo/run-workflow-demo.sh`

3. **Simplify the Workflow**
   - Try a simpler workflow with fewer agents
   - Test each agent individually to identify problematic ones

### UI Rendering Issues

If the UI doesn't render correctly or shows blank components, there may be issues with the frontend code.

**Possible Causes:**

1. JavaScript errors in the browser
2. CSS conflicts
3. Missing assets

**Solutions:**

1. **Check Browser Console**
   - Open the browser console (F12) and look for JavaScript errors
   - Fix any reported issues in the frontend code

2. **Clear Browser Cache**
   - Clear your browser cache and reload the page
   - Try a different browser to rule out browser-specific issues

3. **Verify Assets**
   - Check that all required CSS and JavaScript files are being loaded
   - Look for 404 errors in the network tab of browser developer tools

### Server Startup Failures

If the server fails to start, it may be due to port conflicts or dependency issues.

**Possible Causes:**

1. Port 8083 is already in use
2. Missing dependencies
3. Configuration errors

**Solutions:**

1. **Port Conflicts**
   - Check if port 8083 is already in use:
     ```bash
     lsof -i :8083
     ```
   - Kill the process using the port:
     ```bash
     kill -9 <PID>
     ```
   - Alternatively, modify the port in the server configuration

2. **Dependency Issues**
   - Run `sbt update` to ensure all dependencies are downloaded
   - Check the build.sbt file for any errors

3. **Configuration Errors**
   - Verify the server configuration in the code
   - Check for any required environment variables

## Advanced Troubleshooting

### Debugging the Server

To get more detailed information about server issues:

1. **Enable Debug Logging**
   - Modify the logging configuration to include debug-level logs
   - Look for specific error messages or exceptions

2. **Run with Debugging**
   - Use the following command to run with more verbose output:
     ```bash
     ./modules/workflow-demo/run-workflow-demo.sh --debug
     ```

3. **Inspect Network Traffic**
   - Use tools like Wireshark or the browser's Network tab to inspect API calls
   - Look for unexpected responses or timeouts

### Debugging Workflows

To debug workflow execution issues:

1. **Inspect Workflow Definition**
   - Check the workflow JSON definition for errors or inconsistencies
   - Verify that all agent references are valid

2. **Test Individual Agents**
   - Test each agent in isolation to verify it works correctly
   - Look for agents that consistently fail or timeout

3. **Monitor Resource Usage**
   - Check CPU and memory usage during workflow execution
   - Look for resource exhaustion that might cause failures

## Getting Help

If you've tried the solutions in this guide and are still experiencing issues:

1. **Check GitHub Issues**
   - Search existing issues for similar problems and solutions
   - If no solution exists, consider opening a new issue

2. **Ask in Community Channels**
   - Post your question in the project's discussion forum
   - Include detailed information about the issue and steps to reproduce

3. **Contribute Fixes**
   - If you identify and fix an issue, consider submitting a pull request
   - Include tests that verify the fix works correctly

## Future Improvements

The following improvements are planned to address current limitations:

1. **Standardize ZIO HTTP Version**: Align the ZIO HTTP version across all modules
2. **Reintegrate Core Dependencies**: Properly integrate with core and memory modules
3. **Improve Error Handling**: Add more robust error handling and reporting
4. **Enhanced Logging**: Implement structured logging for better debugging
5. **UI Improvements**: Enhance the UI with better error visualization and workflow monitoring