package com.agenticai.dashboard

import zio._

/**
 * Main entry point for the Dashboard application
 */
object DashboardLauncher extends ZIOAppDefault {
  override def run = {
    for {
      _ <- Console.printLine("=== Agentic AI Dashboard ===")
      _ <- Console.printLine("Starting the Dashboard server...")
      _ <- Console.printLine("The web interface will be available at http://localhost:8081")
      _ <- Console.printLine("Press Ctrl+C to stop the server")
      exitCode <- DashboardServer.run.exitCode
    } yield exitCode
  }
}