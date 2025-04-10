#!/bin/bash

# Run the monitoring stack with docker-compose
cd "$(dirname "$0")"
docker-compose up -d

echo "Monitoring stack started:"
echo "- Prometheus: http://localhost:9090"
echo "- Jaeger: http://localhost:16686"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- OTLP Collector: http://localhost:4317 (gRPC) and http://localhost:4318 (HTTP)"
echo ""
echo "To stop the monitoring stack, run: docker-compose down"