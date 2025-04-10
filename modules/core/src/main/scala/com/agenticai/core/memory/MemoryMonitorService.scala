package com.agenticai.core.memory

import zio.*
import java.time.{Duration as JavaDuration, Instant}

/** Memory statistics over time
  */
case class MemoryStatistics(
    averageSize: Double,
    minSize: Long,
    maxSize: Long,
    averageCount: Double,
    minCount: Int,
    maxCount: Int,
    collectionCount: Int,
    startTime: Instant,
    endTime: Instant
)

/** Service for monitoring memory systems
  */
trait MemoryMonitorService:
  /** Get the current memory metrics
    */
  def getMetrics: ZIO[Any, MemoryError, MemoryMetrics]

  /** Get historical metrics over a time period
    */
  def getHistoricalMetrics(
      from: Instant,
      to: Instant
  ): ZIO[Any, MemoryError, List[MemoryMetrics]]

  /** Enable automatic metrics collection
    */
  def enablePeriodicCollection(interval: JavaDuration): ZIO[Any, MemoryError, Unit]

  /** Disable automatic metrics collection
    */
  def disablePeriodicCollection: ZIO[Any, MemoryError, Unit]

  /** Register a memory system to monitor
    */
  def registerMemorySystem(system: MemorySystem): ZIO[Any, MemoryError, Unit]

  /** Unregister a memory system
    */
  def unregisterMemorySystem(system: MemorySystem): ZIO[Any, MemoryError, Unit]

  /** Set a size threshold for alerts
    */
  def setSizeThreshold(maxTotalSize: Long): ZIO[Any, MemoryError, Unit]

  /** Set a count threshold for alerts
    */
  def setCountThreshold(maxCellCount: Int): ZIO[Any, MemoryError, Unit]

  /** Get memory statistics
    */
  def getStatistics: ZIO[Any, MemoryError, MemoryStatistics]

/** Implementation of MemoryMonitorService
  */
class DefaultMemoryMonitorService(monitor: MemoryMonitor) extends MemoryMonitorService:

  override def getMetrics: ZIO[Any, MemoryError, MemoryMetrics] =
    monitor.getMetrics

  override def getHistoricalMetrics(
      from: Instant,
      to: Instant
  ): ZIO[Any, MemoryError, List[MemoryMetrics]] =
    monitor.getHistoricalMetrics(from, to)

  override def enablePeriodicCollection(interval: JavaDuration): ZIO[Any, MemoryError, Unit] =
    monitor.enablePeriodicCollection(interval)

  override def disablePeriodicCollection: ZIO[Any, MemoryError, Unit] =
    monitor.disablePeriodicCollection

  override def registerMemorySystem(system: MemorySystem): ZIO[Any, MemoryError, Unit] =
    monitor.registerMemorySystem(system)

  override def unregisterMemorySystem(system: MemorySystem): ZIO[Any, MemoryError, Unit] =
    monitor.unregisterMemorySystem(system)

  override def setSizeThreshold(maxTotalSize: Long): ZIO[Any, MemoryError, Unit] =
    monitor.setSizeThreshold(maxTotalSize)

  override def setCountThreshold(maxCellCount: Int): ZIO[Any, MemoryError, Unit] =
    monitor.setCountThreshold(maxCellCount)

  override def getStatistics: ZIO[Any, MemoryError, MemoryStatistics] =
    for
      // Get all historical metrics
      metrics <- monitor.getHistoricalMetrics(Instant.EPOCH, Instant.now())

      // Calculate statistics if we have metrics
      stats <- ZIO.succeed {
        if metrics.isEmpty then
          MemoryStatistics(
            averageSize = 0.0,
            minSize = 0L,
            maxSize = 0L,
            averageCount = 0.0,
            minCount = 0,
            maxCount = 0,
            collectionCount = 0,
            startTime = Instant.now(),
            endTime = Instant.now()
          )
        else {
          // Calculate size statistics
          val sizes       = metrics.map(_.totalSize)
          val totalSize   = sizes.sum
          val averageSize = totalSize.toDouble / metrics.size
          val minSize     = sizes.min
          val maxSize     = sizes.max

          // Calculate count statistics
          val counts       = metrics.map(_.totalCells)
          val totalCount   = counts.sum
          val averageCount = totalCount.toDouble / metrics.size
          val minCount     = counts.min
          val maxCount     = counts.max

          // Find start and end times
          val timestamps = metrics.map(_.timestamp)
          val startTime  = timestamps.min
          val endTime    = timestamps.max

          MemoryStatistics(
            averageSize = averageSize,
            minSize = minSize,
            maxSize = maxSize,
            averageCount = averageCount,
            minCount = minCount,
            maxCount = maxCount,
            collectionCount = metrics.size,
            startTime = startTime,
            endTime = endTime
          )
        }
      }
    yield stats

/** Companion object for creating memory monitor service
  */
object MemoryMonitorService:

  /** Create a new memory monitor service
    */
  def make: ZIO[Any, Nothing, MemoryMonitorService] =
    for monitor <- MemoryMonitor.make
    yield new DefaultMemoryMonitorService(monitor)

  /** Create a memory monitor service and register a memory system
    */
  def makeWithSystem(system: MemorySystem): ZIO[Any, MemoryError, MemoryMonitorService] =
    for
      service <- make
      _       <- service.registerMemorySystem(system)
    yield service

  /** Create a memory monitor service with automatic collection
    */
  def makeWithPeriodicCollection(
      interval: JavaDuration
  ): ZIO[Any, MemoryError, MemoryMonitorService] =
    for
      service <- make
      _       <- service.enablePeriodicCollection(interval)
    yield service

  /** Create a complete memory monitoring setup
    */
  def makeComplete(
      system: MemorySystem,
      collectionInterval: JavaDuration = java.time.Duration.ofMinutes(1),
      sizeThreshold: Option[Long] = None,
      countThreshold: Option[Int] = None
  ): ZIO[Any, MemoryError, MemoryMonitorService] =
    for
      service <- make
      _       <- service.registerMemorySystem(system)
      _       <- service.enablePeriodicCollection(collectionInterval)
      _ <- ZIO.whenCase(sizeThreshold) { case Some(threshold) =>
        service.setSizeThreshold(threshold)
      }
      _ <- ZIO.whenCase(countThreshold) { case Some(threshold) =>
        service.setCountThreshold(threshold)
      }
    yield service
