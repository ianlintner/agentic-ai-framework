# Langchain4j Integration Tests

This directory contains integration tests for the Langchain4j module. These tests verify that the Langchain4j integration works correctly with actual LLM API calls.

## Test Structure

The integration tests are organized by LLM provider:

- `ClaudeIntegrationSpec.scala`: Tests for the Claude model from Anthropic
- `VertexAIGeminiIntegrationSpec.scala`: Tests for the Gemini model via Vertex AI
- `GoogleAIGeminiIntegrationSpec.scala`: Tests for the Gemini model via Google AI API

## Running the Tests

To run the integration tests, you need to set the appropriate environment variables for the LLM provider you want to test:

### Claude Tests

```bash
export CLAUDE_API_KEY=your-claude-api-key
export CLAUDE_MODEL_NAME=claude-3-haiku-20240307  # Optional, defaults to claude-3-haiku-20240307
sbt "it/testOnly com.agenticai.core.llm.langchain.ClaudeIntegrationSpec"
```

### Vertex AI Tests

```bash
export GOOGLE_CLOUD_PROJECT=your-gcp-project-id
export VERTEX_LOCATION=us-central1  # Optional, defaults to us-central1
export VERTEX_MODEL_NAME=gemini-1.0-pro  # Optional, defaults to gemini-1.0-pro
sbt "it/testOnly com.agenticai.core.llm.langchain.VertexAIGeminiIntegrationSpec"
```

Note: The tests use the local credentials from `gcloud auth login` by default. If you haven't authenticated with gcloud, run:

```bash
gcloud auth login
gcloud config set project your-gcp-project-id
```

Alternatively, you can explicitly set the credentials path:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json
```

### Google AI Tests

```bash
export GOOGLE_API_KEY=your-google-api-key
export GOOGLE_MODEL_NAME=gemini-1.0-pro  # Optional, defaults to gemini-1.0-pro
sbt "it/testOnly com.agenticai.core.llm.langchain.GoogleAIGeminiIntegrationSpec"
```

### Running All Tests

To run all integration tests (only those with available credentials will execute):

```bash
sbt it/test
```

## Test Configuration

The tests use the `IntegrationTestConfig` object to manage environment variables and test configuration. If the required environment variables are not set, the tests will be skipped.

## Test Cases

Each integration test suite includes the following test cases:

1. **Basic Response**: Tests that the model can generate a basic response to a simple prompt.
2. **Streaming**: Tests that the model can stream responses.
3. **Multi-turn Conversations**: Tests that the model can maintain context across multiple turns.
4. **Reasoning**: Tests that the model can perform basic reasoning tasks.
5. **Error Handling**: Tests that the system handles errors gracefully.

## Adding New Tests

To add tests for a new LLM provider:

1. Create a new test file in the `it/src/test/scala/com/agenticai/core/llm/langchain` directory.
2. Add the necessary configuration to `IntegrationTestConfig.scala`.
3. Implement the test cases following the pattern in the existing tests.
4. Update this README with instructions for running the new tests.
