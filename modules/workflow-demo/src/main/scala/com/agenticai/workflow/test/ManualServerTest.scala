package com.agenticai.workflow.test

import com.agenticai.workflow.server.WorkflowHttpServer
import zio.*
import zio.http.*
import zio.http.Header.{ContentType => HeaderContentType}
import zio.json.*

/**
 * A simple manual test for the WorkflowHttpServer.
 * This is a standalone application that starts the server and makes some API requests
 * to verify that it's working correctly.
 */
object ManualServerTest extends ZIOAppDefault {

  override def run = {
    // Print instructions
    val instructions = ZIO.succeed {
      println("=== Manual Server Test ===")
      println("This test will start the WorkflowHttpServer and make some API requests to verify it's working.")
      println("Press Enter to start the test...")
      scala.io.StdIn.readLine()
    }

    // Start the server in the background
    val startServer = WorkflowHttpServer.run.fork

    // Test the server
    val testServer = for {
      // Wait for server to start
      _ <- ZIO.sleep(2.seconds)
      _ <- Console.printLine("Server started. Making API requests...")

      // No need to create a client here as we'll use ZIO.serviceWithZIO[Client]

      // 1. Execute a workflow
      _ <- Console.printLine("\n1. Executing a workflow...")
      executeRequest: Request = Request.post(
        url = URL.decode("http://localhost:8083/api/workflow/execute").toOption.get,
        body = Body.fromString("""{"input":"This is a test input for the workflow execution."}""")
      ).addHeader(HeaderContentType(MediaType.application.json))

      executeResponse <- ZIO.serviceWithZIO[Client](_.request(executeRequest))
      executeStatus = executeResponse.status
      executeBody <- executeResponse.body.asString

      _ <- Console.printLine(s"Execute response status: $executeStatus")
      _ <- Console.printLine(s"Execute response body: $executeBody")

      // Parse the workflow ID
      workflowId <- ZIO.fromEither(executeBody.fromJson[WorkflowHttpServer.WorkflowResponse])
        .map(_.id)
        .catchAll(error => {
          Console.printLine(s"Failed to parse response: $error") *>
          ZIO.succeed("unknown-id")
        })

      _ <- Console.printLine(s"Workflow ID: $workflowId")

      // 2. Check status
      _ <- Console.printLine("\n2. Checking workflow status...")
      statusRequest: Request = Request.get(
        url = URL.decode(s"http://localhost:8083/api/workflow/status/$workflowId").toOption.get
      )

      statusResponse <- ZIO.serviceWithZIO[Client](_.request(statusRequest))
      statusBody <- statusResponse.body.asString
      _ <- Console.printLine(s"Status response: $statusBody")

      // 3. Check progress
      _ <- Console.printLine("\n3. Checking workflow progress...")
      progressRequest: Request = Request.get(
        url = URL.decode(s"http://localhost:8083/api/workflow/progress/$workflowId").toOption.get
      )

      progressResponse <- ZIO.serviceWithZIO[Client](_.request(progressRequest))
      progressBody <- progressResponse.body.asString
      _ <- Console.printLine(s"Progress response: $progressBody")

      // 4. Wait for completion
      _ <- Console.printLine("\n4. Waiting for workflow to complete...")
      _ <- ZIO.sleep(10.seconds)

      // 5. Get final result
      _ <- Console.printLine("\n5. Getting final result...")
      resultRequest: Request = Request.get(
        url = URL.decode(s"http://localhost:8083/api/workflow/result/$workflowId").toOption.get
      )

      resultResponse <- ZIO.serviceWithZIO[Client](_.request(resultRequest))
      resultBody <- resultResponse.body.asString
      _ <- Console.printLine(s"Result response: $resultBody")

      // 6. Test complete
      _ <- Console.printLine("\nTest complete! Press Enter to exit...")
      _ <- ZIO.succeed(scala.io.StdIn.readLine())
    } yield ()

    // Run the test
    for {
      _ <- instructions
      serverFiber <- startServer
      _ <- testServer.catchAll(error => Console.printLine(s"Test failed: ${error.getMessage}")).provide(Client.default, Scope.default)
      _ <- serverFiber.interrupt
    } yield ExitCode.success
  }
}
