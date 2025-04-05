import sbt._

object Langchain4jDependencies {
  // Langchain4j versions
  val langchain4jVersion = "1.0.0-beta2"
  
  // Langchain4j dependencies
  val langchain4jCore = "dev.langchain4j" % "langchain4j" % langchain4jVersion
  val langchain4jAnthropic = "dev.langchain4j" % "langchain4j-anthropic" % langchain4jVersion
  val langchain4jVertexAi = "dev.langchain4j" % "langchain4j-vertex-ai" % langchain4jVersion
  val langchain4jGoogleAiGemini = "dev.langchain4j" % "langchain4j-google-ai-gemini" % langchain4jVersion
  val langchain4jVertexAiGemini = "dev.langchain4j" % "langchain4j-vertex-ai-gemini" % langchain4jVersion
  val langchain4jOpenAi = "dev.langchain4j" % "langchain4j-open-ai" % langchain4jVersion
  val langchain4jHttpClient = "dev.langchain4j" % "langchain4j-http-client" % langchain4jVersion
  val langchain4jHttpClientJdk = "dev.langchain4j" % "langchain4j-http-client-jdk" % langchain4jVersion
  
  // All Langchain4j dependencies
  val langchain4jDependencies = Seq(
    langchain4jCore,
    langchain4jAnthropic,
    langchain4jVertexAi,
    langchain4jGoogleAiGemini,
    langchain4jVertexAiGemini,
    langchain4jOpenAi,
    langchain4jHttpClient,
    langchain4jHttpClientJdk
  )
}
