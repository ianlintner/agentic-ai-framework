package com.agenticai.core.processing

import zio._
import java.util.concurrent.TimeoutException
import com.agenticai.core.memory.{MemorySystem, MemoryError}

/**
 * Core trait for parallel processing functionality.
 */
trait ParallelProcessor {
  def executeAll[R, E, A](tasks: Iterable[ZIO[R, E, A]]): ZIO[R, E, List[A]]
  
  def executeWithTimeout[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    timeout: Duration
  ): ZIO[R, Either[E, TimeoutException], List[A]]
  
  def executeWithCancellation[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    cancellationToken: Ref[Boolean]
  ): ZIO[R, E, List[A]]
  
  def executeMemoryAware[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    memoryPerTask: Long
  ): ZIO[R with MemorySystem, E, List[A]]
  
  def executeWithStrategy[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    strategy: ExecutionStrategy
  ): ZIO[R, E, List[A]]
}

/**
 * Execution strategies for parallel processing.
 */
sealed trait ExecutionStrategy
object ExecutionStrategy {
  case class FixedPool(threadCount: Int) extends ExecutionStrategy
  case object WorkStealing extends ExecutionStrategy
  case class Batched(batchSize: Int) extends ExecutionStrategy
  case class MemoryBased(maxMemoryBytes: Long) extends ExecutionStrategy
}

object ParallelProcessor {
  def apply(): ParallelProcessor = new DefaultParallelProcessor()
  def memoryAware(memorySystem: MemorySystem): ParallelProcessor = 
    new MemoryAwareParallelProcessor(memorySystem)
}

/**
 * Default implementation of ParallelProcessor.
 */
private class DefaultParallelProcessor extends ParallelProcessor {
  override def executeAll[R, E, A](tasks: Iterable[ZIO[R, E, A]]): ZIO[R, E, List[A]] = 
    ZIO.foreachPar(tasks.toList)(identity)
  
  override def executeWithTimeout[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    timeout: Duration
  ): ZIO[R, Either[E, TimeoutException], List[A]] = {
    val program = ZIO.foreachPar(tasks.toList)(identity)
    
    program.foldZIO(
      // Convert task errors to Left
      err => ZIO.fail(scala.util.Left(err)),
      success => ZIO.succeed(success)
    ).timeout(timeout).flatMap {
      case Some(result) => ZIO.succeed(result)
      case None => ZIO.fail(scala.util.Right(new TimeoutException("Execution timed out")))
    }
  }
  
  override def executeWithCancellation[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    cancellationToken: Ref[Boolean]
  ): ZIO[R, E, List[A]] = {
    for {
      // Start tasks in a fiber
      fiber <- ZIO.foreachPar(tasks.toList)(identity).fork
      
      // Start a cancellation watcher fiber
      watcherFiber <- ZIO.succeed {
        val watcher = for {
          shouldCancel <- cancellationToken.get
          _ <- if (shouldCancel) fiber.interrupt else ZIO.sleep(Duration.fromMillis(50))
        } yield shouldCancel
        
        watcher.repeatUntil(identity)
      }.fork
      
      // Wait for tasks to complete
      result <- fiber.join
      _ <- watcherFiber.interrupt
    } yield result
  }
  
  override def executeMemoryAware[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    memoryPerTask: Long
  ): ZIO[R with MemorySystem, E, List[A]] = {
    for {
      memorySystem <- ZIO.service[MemorySystem]
      cells <- memorySystem.getAllCells.fold(_ => List.empty, identity)
      
      // Simple heuristic - base on cell count and task memory size
      // Limit parallelism based on number of cells (assumed to correlate with memory pressure)
      adaptiveParallelism = math.max(1, 10 - cells.size / 10)
      
      // Further adjust based on memory per task (larger tasks get less parallelism)
      finalParallelism = if (memoryPerTask > 10 * 1024 * 1024) {
        math.max(1, adaptiveParallelism / 2) // Half parallelism for large tasks
      } else {
        adaptiveParallelism
      }
      
      result <- ZIO.foreachPar(tasks.toList)(identity).withParallelism(finalParallelism)
    } yield result
  }
  
  override def executeWithStrategy[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    strategy: ExecutionStrategy
  ): ZIO[R, E, List[A]] = {
    strategy match {
      case ExecutionStrategy.FixedPool(threadCount) =>
        ZIO.foreachPar(tasks.toList)(identity).withParallelism(threadCount)
        
      case ExecutionStrategy.WorkStealing =>
        ZIO.foreachPar(tasks.toList)(identity)
        
      case ExecutionStrategy.Batched(batchSize) =>
        ZIO.foldLeft(tasks.grouped(batchSize).toList)(List.empty[A]) { (acc, batch) =>
          ZIO.foreachPar(batch)(identity).map(acc ++ _)
        }
        
      case ExecutionStrategy.MemoryBased(maxMemoryBytes) =>
        val estimatedTasksCount = math.max(1, (maxMemoryBytes / (1024 * 1024)).toInt)
        executeWithStrategy(tasks, ExecutionStrategy.FixedPool(estimatedTasksCount))
    }
  }
}

/**
 * Memory-aware implementation of ParallelProcessor.
 */
private class MemoryAwareParallelProcessor(memorySystem: MemorySystem) extends DefaultParallelProcessor {
  override def executeMemoryAware[R, E, A](
    tasks: Iterable[ZIO[R, E, A]],
    memoryPerTask: Long
  ): ZIO[R with MemorySystem, E, List[A]] = {
    for {
      cells <- memorySystem.getAllCells.fold(_ => List.empty, identity)
      
      // Calculate optimal parallelism based on current memory cells
      cellBasedParallelism = math.max(1, 20 - cells.size / 5)
      
      // Adjust based on memory per task (in MB)
      memoryPerTaskMB = memoryPerTask / (1024 * 1024)
      memoryBasedAdjustment = (memoryPerTaskMB / 10).toInt // Reduce parallelism for larger tasks
      
      // Final parallelism is cell-based with memory adjustment
      finalParallelism = math.max(1, cellBasedParallelism - memoryBasedAdjustment)
      
      // Execute tasks with calculated parallelism
      result <- ZIO.foreachPar(tasks.toList)(identity).withParallelism(finalParallelism)
    } yield result
  }
}