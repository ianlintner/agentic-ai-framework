package com.agenticai.workflow

import com.agenticai.workflow.server.WorkflowHttpServer
import zio._

/**
 * Main entry point for the Workflow Demo application
 * 
 * This launcher starts the ZIO HTTP server implementation which provides:
 * - Static file serving for the web UI
 * - REST API endpoints for workflow operations
 * - WebSocket support for real-time updates
 */
object WorkflowDemoLauncher extends ZIOAppDefault {

  override def run =
    Console.printLine("=== Agentic AI Workflow Demo ===").orDie *>
    Console.printLine("Starting HTTP server for the workflow demo...").orDie *>
    Console.printLine("The web interface will be available at http://localhost:8080").orDie *>
    Console.printLine("Press Ctrl+C to stop the server").orDie *>
    WorkflowHttpServer.run.exitCode
}