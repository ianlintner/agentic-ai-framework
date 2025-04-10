package com.agenticai.telemetry.mesh

import com.agenticai.mesh.discovery.{AgentDirectory, AgentInfo, AgentMetadata, AgentStatus, DirectoryEvent, TypedAgentQuery}
import com.agenticai.mesh.protocol.RemoteAgentRef
import com.agenticai.telemetry.core.TelemetryProvider
import zio.*

import java.time.Instant
import java.util.UUID

/**
 * Decorates an AgentDirectory implementation with telemetry instrumentation.
 * This provides visibility into agent discovery operations without modifying
 * the underlying implementation.
 */
class InstrumentedAgentDirectory(
  underlying: AgentDirectory
) extends AgentDirectory {

  /**
   * Register an agent with telemetry instrumentation
   */
  override def registerAgent[I, O](
    ref: RemoteAgentRef[I, O],
    metadata: AgentMetadata
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    val sourceId = "local-" + UUID.randomUUID().toString.take(8)
    val agentId = ref.id.toString
    
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Perform the registration
      result <- underlying.registerAgent(ref, metadata)
        .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("register"))
        
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Record node health metric for the agent's location
      _ <- MeshTelemetry.recordNodeHealth(
        ref.location.toString,
        1, // Single agent registered
        "ACTIVE", 
        metadata.initialLoad.getOrElse(0.0)
      )
    } yield result
  }

  /**
   * Unregister an agent with telemetry instrumentation
   */
  override def unregisterAgent(
    agentId: UUID
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    underlying.unregisterAgent(agentId)
      .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("unregister"))
  }

  /**
   * Discover agents with telemetry instrumentation
   */
  override def discoverAgents(
    query: TypedAgentQuery
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, List[AgentInfo]] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Perform the discovery query
      results <- underlying.discoverAgents(query)
        .pipe(MeshTelemetryAspect.instrumentAgentDiscovery(
          s"query.${query.getClass.getSimpleName}"
        ))
        
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Record discovery metrics
      _ <- MeshTelemetry.recordAgentDiscovery(
        query.getClass.getSimpleName,
        results.size,
        endTime - startTime
      )
    } yield results
  }

  /**
   * Get agent info with telemetry instrumentation
   */
  override def getAgentInfo(
    agentId: UUID
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Option[AgentInfo]] = {
    underlying.getAgentInfo(agentId)
      .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("get.info"))
  }

  /**
   * Subscribe to directory events
   */
  override def subscribeToEvents(): Stream[DirectoryEvent] = {
    underlying.subscribeToEvents()
    // Note: We're not wrapping the stream in telemetry as it would require modifying
    // the consumer which is outside our control
  }

  /**
   * Update agent status with telemetry instrumentation
   */
  override def updateAgentStatus(
    agentId: UUID,
    status: AgentStatus
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    for {
      // Get current info before update to capture node location
      maybeInfo <- underlying.getAgentInfo(agentId)
      
      // Perform update
      _ <- underlying.updateAgentStatus(agentId, status)
        .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("update.status"))
      
      // Update node health metric if we have the info
      _ <- ZIO.foreach(maybeInfo) { info =>
        MeshTelemetry.recordNodeHealth(
          info.ref.location.toString,
          1, // Single agent
          status.toString,
          info.loadFactor
        )
      }
    } yield ()
  }

  /**
   * Get all agents with telemetry instrumentation
   */
  override def getAllAgents(): ZIO[MeshTelemetry & TelemetryProvider, Throwable, List[AgentInfo]] = {
    for {
      startTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Get all agents
      results <- underlying.getAllAgents()
        .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("get.all"))
        
      endTime <- Clock.currentTime(TimeUnit.MILLISECONDS)
      
      // Record discovery metrics
      _ <- MeshTelemetry.recordAgentDiscovery(
        "get.all",
        results.size,
        endTime - startTime
      )
    } yield results
  }

  /**
   * Update agent metadata with telemetry instrumentation
   */
  def updateAgentMetadata(
    agentId: UUID,
    metadata: AgentMetadata
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    underlying.updateAgentMetadata(agentId, metadata)
      .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("update.metadata"))
  }

  /**
   * Update agent load factor with telemetry instrumentation
   */
  def updateAgentLoadFactor(
    agentId: UUID,
    loadFactor: Double
  ): ZIO[MeshTelemetry & TelemetryProvider, Throwable, Unit] = {
    for {
      // Get current info before update to capture node location
      maybeInfo <- underlying.getAgentInfo(agentId)
      
      // Perform update
      _ <- underlying.updateAgentLoadFactor(agentId, loadFactor)
        .pipe(MeshTelemetryAspect.instrumentAgentDiscovery("update.load"))
      
      // Update node health metric if we have the info
      _ <- ZIO.foreach(maybeInfo) { info =>
        MeshTelemetry.recordNodeHealth(
          info.ref.location.toString,
          1, // Single agent
          info.status.toString,
          loadFactor
        )
      }
    } yield ()
  }
}

object InstrumentedAgentDirectory {
  /**
   * Create an instrumented version of an agent directory
   * 
   * @param directory The underlying directory to instrument
   * @return A new directory with telemetry instrumentation
   */
  def apply(directory: AgentDirectory): InstrumentedAgentDirectory =
    new InstrumentedAgentDirectory(directory)
}