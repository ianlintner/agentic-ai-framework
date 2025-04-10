package com.agenticai.mesh.protocol

/** Location of an agent in the mesh.
  */
case class AgentLocation(address: String, port: Int):
  /** Get the full URI for this location.
    *
    * @return
    *   URI string
    */
  def uri: String = s"http://$address:$port"

  /** Check if this location is local.
    *
    * @return
    *   True if this is a local location
    */
  def isLocal: Boolean =
    address == "localhost" || address == "127.0.0.1"

  /** Create a string representation.
    *
    * @return
    *   String representation
    */
  override def toString: String = uri

object AgentLocation:

  /** Create a local location with the given port.
    *
    * @param port
    *   Port number
    * @return
    *   Local agent location
    */
  def local(port: Int): AgentLocation =
    AgentLocation("localhost", port)

  /** Parse a location from a URI string.
    *
    * @param uri
    *   URI string
    * @return
    *   Location if valid
    */
  def fromUri(uri: String): Option[AgentLocation] =
    try
      // Use URI first, then convert to URL to avoid deprecation warning
      val javaUri = new java.net.URI(uri)
      val url     = javaUri.toURL()
      Some(AgentLocation(url.getHost, url.getPort))
    catch case _: Exception => None
