package com.agenticai.workflow

import zio._
import com.agenticai.workflow.server.WorkflowHttpServer

/** Main entry point for the Workflow Demo application
  *
  * This app starts a HTTP server that provides:
  *   - REST API endpoints for workflow operations
  *   - WebSocket support for real-time updates
  */
object WorkflowDemoLauncher extends ZIOAppDefault {
  override def run =
    WorkflowHttpServer.run
}
