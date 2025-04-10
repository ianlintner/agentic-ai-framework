# Agentic AI Framework Dashboard - Docker Setup

This directory contains Docker configurations for running the Agentic AI Framework dashboard in a containerized environment. This setup provides several benefits:

- Avoids CORS issues when connecting to backend services
- Provides a production-ready deployment option
- Simplifies running the dashboard alongside other services
- Ensures consistent behavior across different environments


## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Quick Start

To build and run the dashboard containers:

```bash
# Navigate to the dashboard directory
cd modules/dashboard

# Build and start the containers
docker-compose up -d

# To stop the containers
docker-compose down
```

The dashboard will be available at [http://localhost](http://localhost).

## Configuration Options

### Frontend Configuration

The frontend (nginx) container is configured via the `nginx.conf` file. Key customization points:

- **Server Name**: Change the `server_name` directive in `nginx.conf` if you're hosting with a specific domain name
- **CORS Settings**: The configuration includes headers for Cross-Origin Resource Sharing
- **Cache Control**: Adjust cache settings for better performance in production environments

### API Server Configuration

The backend API server is configured via the `docker-compose.yml` file. By default, it runs on port 8081 and is accessed by the frontend through the `/api` endpoint.

To connect the dashboard to a different backend:

1. Modify the `api-server` service in `docker-compose.yml` to point to your backend implementation
2. Update the startup command for your specific backend needs
3. Adjust the proxy settings in `nginx.conf` if the API paths need customization

### Connecting to External Backends

If you want to connect to an external backend service instead of running one in a container:

1. Comment out or remove the `api-server` service in `docker-compose.yml`
2. Update the `proxy_pass` directive in `nginx.conf` to point to your external service:

```
location /api/ {
    proxy_pass http://your-external-service-url:port/;
    # Other proxy settings
}
```

## Advanced Usage

### Using with Different Environments

You can create environment-specific configuration files for different deployment scenarios:

1. Create separate docker-compose override files:
   - `docker-compose.dev.yml`
   - `docker-compose.staging.yml`
   - `docker-compose.prod.yml`

2. Run using the specific environment:
   ```bash
   docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
   ```

### Building a Standalone Image

To build a standalone image of just the dashboard:

```bash
# Navigate to the dashboard directory
cd modules/dashboard

# Build the image
docker build -t agentic-dashboard:latest .

# Run the container
docker run -d -p 80:80 --name agentic-dashboard agentic-dashboard:latest
```

### Security Considerations

For production deployments:

1. Consider using HTTPS by:
   - Adding SSL certificates to the nginx configuration
   - Updating the `nginx.conf` to handle HTTPS connections
   - Using a reverse proxy like Traefik or Nginx Proxy Manager

2. Restrict access to sensitive endpoints in the `nginx.conf` file

## Troubleshooting

### CORS Issues

If you encounter CORS issues:

1. Verify that the backend service is correctly specified in the `proxy_pass` directive
2. Check that the CORS headers are properly configured in `nginx.conf`
3. Ensure that your backend service is accepting requests from the dashboard origin

### Connection Refused

If the dashboard cannot connect to the backend:

1. Verify that the backend service is running
2. Check network connectivity between containers using `docker network inspect agentic-network`
3. Verify port configurations in `docker-compose.yml`

## Development Workflow

For active development:

1. Mount the source directory to see changes in real-time:
   ```yaml
   volumes:
     - ./src/main/resources/public:/usr/share/nginx/html
   ```

2. Use the browser's developer tools to debug frontend issues and monitor network requests