package com.agenticai.dashboard

import zio.*

/** Dashboard server placeholder This is a temporary implementation until we can properly configure
  * ZIO HTTP
  */
object DashboardServer:

  /** Run the server
    */
  def run: ZIO[Any, Throwable, ExitCode] =
    for
      _ <- Console.printLine("=== Dashboard Server ===")
      _ <- Console.printLine("Dashboard would serve static files from resources/public/")
      _ <- Console.printLine("There are compatibility issues with the ZIO HTTP version")
      _ <- Console.printLine("For now, you can access the dashboard directly with a browser at:")
      _ <- Console.printLine(
        "file:///Users/E74823/projects/agentic-ai-framework/modules/dashboard/src/main/resources/public/index.html"
      )

      // Wait for user input to simulate a running server
      _ <- Console.printLine("\nPress Enter to stop the server...")
      _ <- Console.readLine
    yield ExitCode.success
