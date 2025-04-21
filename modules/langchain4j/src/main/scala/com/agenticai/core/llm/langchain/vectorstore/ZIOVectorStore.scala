package com.agenticai.core.llm.langchain.vectorstore

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.{EmbeddingMatch, EmbeddingStore, EmbeddingStoreIngestor}
import com.agenticai.core.llm.langchain.embedding.ZIOEmbeddingModel
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import zio.*

import java.util
import scala.jdk.CollectionConverters.*
import scala.annotation.nowarn

/**
 * A trait representing a ZIO-compatible vector store for embeddings.
 * Provides ZIO-wrapped methods for storing and retrieving embeddings.
 */
trait ZIOVectorStore:
  /**
   * Adds a text segment with its embedding to the store.
   *
   * @param segment The text segment to add
   * @param embedding The embedding of the text segment
   * @return A ZIO effect that completes when the segment is added
   */
  def addEmbedding(segment: TextSegment, embedding: Embedding): ZIO[Any, Throwable, Unit]

  /**
   * Adds multiple text segments with their embeddings to the store.
   *
   * @param segments The text segments to add
   * @param embeddings The embeddings of the text segments
   * @return A ZIO effect that completes when all segments are added
   */
  def addEmbeddings(segments: Iterable[TextSegment], embeddings: Iterable[Embedding]): ZIO[Any, Throwable, Unit]

  /**
   * Adds text segments to the store, calculating embeddings on the fly.
   *
   * @param segments The text segments to add
   * @param embeddingModel The embedding model to use for generating embeddings
   * @return A ZIO effect that completes with the list of segment IDs
   */
  def addTextSegments(segments: List[TextSegment], embeddingModel: ZIOEmbeddingModel): ZIO[Any, Throwable, List[String]]

  /**
   * Finds similar text segments for a given query.
   *
   * @param query The query text
   * @param embeddingModel The embedding model to use for generating the query embedding
   * @param maxResults The maximum number of results to return
   * @param minScore The minimum relevance score for results
   * @return A ZIO effect that completes with the relevant segments
   */
  def findSimilar(query: String, embeddingModel: ZIOEmbeddingModel, maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[EmbeddingMatch[TextSegment]]]

  /**
   * Finds the most relevant text segments for a query embedding.
   *
   * @param queryEmbedding The query embedding
   * @param maxResults The maximum number of results to return
   * @param minScore The minimum relevance score for results
   * @return A ZIO effect that completes with the matching segments
   */
  def findRelevant(queryEmbedding: Embedding, maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[EmbeddingMatch[TextSegment]]]

  /**
   * Finds the most relevant text segments for multiple query embeddings.
   *
   * @param queryEmbeddings The query embeddings
   * @param maxResults The maximum number of results to return per query
   * @param minScore The minimum relevance score for results
   * @return A ZIO effect that completes with the matching segments for each query
   */
  def findRelevantBatch(queryEmbeddings: Iterable[Embedding], maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[List[EmbeddingMatch[TextSegment]]]]

/**
 * Factory object for ZIOVectorStore implementations.
 */
object ZIOVectorStore:
  /**
   * Creates a ZIOVectorStore implementation backed by a provided EmbeddingStore.
   *
   * @param store The embedding store to use
   * @return A ZIOVectorStore implementation
   */
  def fromEmbeddingStore(store: EmbeddingStore[TextSegment]): ZIOVectorStore = new ZIOVectorStore:
    override def addEmbedding(segment: TextSegment, embedding: Embedding): ZIO[Any, Throwable, Unit] =
      ZIO.attempt {
        store.add(embedding, segment)
        ()
      }

    override def addEmbeddings(segments: Iterable[TextSegment], embeddings: Iterable[Embedding]): ZIO[Any, Throwable, Unit] =
      ZIO.attempt {
        val embeddingsList = embeddings.toList.asJava
        val segmentsList = segments.toList.asJava
        store.addAll(embeddingsList, segmentsList)
        ()
      }

    override def addTextSegments(segments: List[TextSegment], embeddingModel: ZIOEmbeddingModel): ZIO[Any, Throwable, List[String]] = {
      // Create a ZIO computation that extracts texts from segments
      val textsZIO = ZIO.succeed(segments.map(_.text()))
      
      // Use flatMap to chain operations
      textsZIO.flatMap { texts =>
        embeddingModel.embedAll(texts).flatMap { embeddings =>
          addEmbeddings(segments, embeddings).map { _ =>
            segments.map(_ => java.util.UUID.randomUUID().toString)
          }
        }
      }
    }

    override def findSimilar(query: String, embeddingModel: ZIOEmbeddingModel, maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[EmbeddingMatch[TextSegment]]] = {
      embeddingModel.embed(query).flatMap { queryEmbedding =>
        findRelevant(queryEmbedding, maxResults, minScore)
      }
    }

    @nowarn("cat=deprecation")
    override def findRelevant(queryEmbedding: Embedding, maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[EmbeddingMatch[TextSegment]]] =
      ZIO.attempt {
        // Using Scala's nowarn annotation to suppress deprecation warnings
        val matches = store.findRelevant(queryEmbedding, maxResults, minScore)
        matches.asScala.toList
      }

    @nowarn("cat=deprecation")
    override def findRelevantBatch(queryEmbeddings: Iterable[Embedding], maxResults: Int, minScore: Double = 0.0): ZIO[Any, Throwable, List[List[EmbeddingMatch[TextSegment]]]] =
      ZIO.attempt {
        queryEmbeddings.map { embedding =>
          // Using Scala's nowarn annotation to suppress deprecation warnings
          val matches = store.findRelevant(embedding, maxResults, minScore)
          matches.asScala.toList
        }.toList
      }

  /**
   * Creates a ZLayer for ZIOVectorStore from an EmbeddingStore.
   *
   * @param store The embedding store to use
   * @return A ZLayer that provides a ZIOVectorStore
   */
  def layer(store: EmbeddingStore[TextSegment]): ZLayer[Any, Nothing, ZIOVectorStore] =
    ZLayer.succeed(fromEmbeddingStore(store))

  /**
   * Creates an in-memory ZIOVectorStore.
   * 
   * @return A ZIO effect that completes with a new in-memory ZIOVectorStore
   */
  def createInMemory(): UIO[ZIOVectorStore] = 
    ZIO.succeed {
      // Using InMemoryEmbeddingStore constructor directly
      val store = new InMemoryEmbeddingStore[TextSegment]()
      fromEmbeddingStore(store)
    }