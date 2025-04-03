# Claude 3.7 on Vertex AI Integration Testing Guide

This guide provides instructions for testing the integration of Claude 3.7 Haiku with Google Vertex AI in the Agentic AI Framework.

## Prerequisites

Before running any integration tests, ensure you have:

1. A Google Cloud Platform (GCP) account with billing enabled
2. Access to the Vertex AI API and Claude models
3. The Google Cloud SDK installed locally
4. Authentication configured (Application Default Credentials or service account)
5. The SBT build tool installed

## Setup Authentication

### Option 1: Application Default Credentials

Run this command to authenticate with your Google account:

```bash
gcloud auth application-default login
```

### Option 2: Service Account

1. Create a service account in the Google Cloud Console
2. Grant it the necessary permissions for Vertex AI
3. Download the JSON key file
4. Set the environment variable:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/your-key-file.json
```

## Environment Configuration

Set these environment variables for testing:

```bash
# Required
export GOOGLE_CLOUD_PROJECT="your-gcp-project-id"

# Optional (defaults shown)
export VERTEX_LOCATION="us-central1"
export CLAUDE_MODEL_ID="claude-3-7-haiku-20240307"
```

## Simple Connection Test

To test just the connectivity to Vertex AI and Claude models, run:

```bash
sbt "testVertexConnection"
```

This simple test will attempt to:
1. Authenticate with Google Cloud
2. Connect to the Vertex AI API
3. Verify that the Claude model is available
4. Make a simple request to the model

The test output will indicate success or failure and provide troubleshooting information.

## Testing the Demo Application

We've included a simplified demo application that doesn't depend heavily on the ZIO library, which should compile and run even if there are other dependency issues:

```bash
sbt "runMain com.agenticai.examples.VertexAIClaudeDemo"
```

This demo:
- Connects to Vertex AI
- Sends example prompts to Claude 3.7
- Prints simulated responses (or real ones if properly configured)

## Testing with curl (Manual Verification)

For manual testing and verification, you can use curl to make a direct request to the Vertex AI API:

```bash
# First, get your access token
ACCESS_TOKEN=$(gcloud auth print-access-token)

# Make a request to Claude
curl -X POST \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  https://us-central1-aiplatform.googleapis.com/v1/projects/YOUR_PROJECT_ID/locations/us-central1/publishers/anthropic/models/claude-3-7-haiku-20240307:predict \
  -d '{
    "instances": [
      {
        "prompt": "Human: Tell me about yourself\n\nAssistant:"
      }
    ],
    "parameters": {
      "temperature": 0.2,
      "maxOutputTokens": 256,
      "topK": 40,
      "topP": 0.95
    }
  }'
```

Replace `YOUR_PROJECT_ID` with your actual GCP project ID.

## Integration Tests

The full integration test suite can be run with:

```bash
sbt "it:test"
```

These tests verify:
- Authentication works correctly
- The VertexAIClient can successfully connect to the API
- Requests and responses are properly formatted
- Error handling behaves as expected

## Troubleshooting

### Common Issues

1. **Authentication errors**:
   - Verify that you have the correct credentials
   - Ensure the service account has the necessary permissions

2. **Access denied**:
   - Check that the Vertex AI API is enabled for your project
   - Verify your service account has the correct IAM roles

3. **Model not found**:
   - Confirm that Claude models are available in your region
   - Make sure you're using the correct model name

4. **Compilation issues**:
   - If you encounter ZIO-related compilation errors, try using the simplified demo
   - Check that all dependencies are correctly specified in build.sbt

### Getting Logs

To enable detailed logging:

```bash
export AGENTIC_LOG_LEVEL=DEBUG
```

Then run the tests or demo again to see more detailed information.

## Next Steps

After confirming the integration works, you can:

1. Build your own agents using the Claude 3.7 model
2. Customize the prompt templates for your specific use case
3. Integrate with other components of the Agentic AI Framework
4. Experiment with different parameter settings (temperature, top-p, etc.)

## Support

If you encounter issues not covered by this guide, please:

1. Check the Google Cloud Vertex AI documentation
2. Review the Anthropic Claude documentation
3. Open an issue in the Agentic AI Framework repository