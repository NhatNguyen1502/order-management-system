---
description: 'Guidelines for Docker, Docker Compose, and infrastructure configuration'
applyTo: '**/Dockerfile,**/docker-compose*.yml,**/docker-compose*.yaml'
---

# Docker and Infrastructure Guidelines

## Docker Best Practices

### Dockerfile Structure

#### Multi-stage Builds
- Use multi-stage builds to reduce image size
- Separate build and runtime stages
- Copy only necessary artifacts to final image

#### Example Structure
```dockerfile
# Build stage
FROM hseeberger/scala-sbt:11.0.12_1.9.7_2.13.10 AS builder
WORKDIR /build
COPY . .
RUN sbt assembly

# Runtime stage
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=builder /build/target/scala-2.13/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Base Images
- Use official images from Docker Hub
- Prefer Alpine or slim variants for smaller size
- Pin specific versions for reproducibility
- Examples:
  - `postgres:15-alpine`
  - `openjdk:11-jre-slim`
  - `node:20-alpine`

### Layer Optimization
- Order commands from least to most frequently changing
- Combine related RUN commands to reduce layers
- Use `.dockerignore` to exclude unnecessary files
- Example order:
  1. Install system dependencies
  2. Copy dependency files (package.json, build.sbt)
  3. Install dependencies
  4. Copy source code
  5. Build application

### Security
- Never include secrets in images
- Run as non-root user when possible:
  ```dockerfile
  RUN addgroup -g 1000 appuser && \
      adduser -u 1000 -G appuser -s /bin/sh -D appuser
  USER appuser
  ```
- Scan images for vulnerabilities
- Keep base images updated

### .dockerignore
Always include:
```
.git
.github
node_modules
target
*.log
.env
.env.*
*.md
.DS_Store
.idea
.vscode
```

## Docker Compose

### Service Definition Structure

```yaml
services:
  service-name:
    build:
      context: ./service-directory
      dockerfile: Dockerfile
    container_name: service-name
    environment:
      - ENV_VAR=value
    ports:
      - "host:container"
    depends_on:
      - dependency-service
    networks:
      - network-name
    volumes:
      - volume-name:/path/in/container
    restart: unless-stopped
```

### Environment Variables
- Use environment variables for configuration
- Define defaults in docker-compose.yml
- Override with .env file for local development
- Never commit .env files with secrets

### Networking
- Use custom networks for service isolation
- Use service names for DNS resolution
- Example:
  ```yaml
  networks:
    app-network:
      driver: bridge
  ```

### Volumes
- Use named volumes for data persistence
- Use bind mounts for development only
- Example:
  ```yaml
  volumes:
    postgres-data:
    redis-cache:
  ```

### Dependencies
- Use `depends_on` to define startup order
- Note: `depends_on` doesn't wait for service readiness
- Implement health checks or retry logic in applications

### Health Checks
```yaml
services:
  database:
    healthcheck:
      test: ["CMD", "pg_isready", "-U", "postgres"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## Order Management System Specifics

### Service Ports
- PostgreSQL: 5433 (host) → 5432 (container)
- API Gateway: 8080
- Order Service: 8081 (gRPC), 2551 (Akka cluster)
- Inventory Service: 8082 (gRPC), 2552 (Akka cluster)
- Product Service: 8083 (gRPC)
- Frontend: 4200 (host) → 80 (container)

### Database Services
- PostgreSQL for event journal and read models
- Separate databases per service (orderdb, inventorydb)
- Initialization scripts in `docker/postgres/init/`
- Use named volumes for data persistence

### Scala Services
- Build with `sbt assembly` first
- Use OpenJDK 11 runtime
- Expose gRPC and Akka cluster ports
- Pass database config via environment variables

### Frontend Service
- Build Angular app in Dockerfile
- Serve with Nginx
- Configure nginx for SPA routing
- Proxy API calls to gateway

## Common Commands

### Building
```bash
# Build all services
docker-compose build

# Build specific service
docker-compose build service-name

# Build without cache
docker-compose build --no-cache
```

### Running
```bash
# Start all services
docker-compose up -d

# Start specific service
docker-compose up -d service-name

# View logs
docker-compose logs -f [service-name]

# Follow logs from all services
docker-compose logs -f
```

### Management
```bash
# List running containers
docker-compose ps

# Stop all services
docker-compose down

# Stop and remove volumes
docker-compose down -v

# Restart service
docker-compose restart service-name

# Execute command in container
docker-compose exec service-name command
```

### Debugging
```bash
# Enter container shell
docker-compose exec service-name /bin/sh

# View container logs
docker-compose logs service-name

# Inspect container
docker inspect container-name

# View resource usage
docker stats
```

## Development Workflow

### Local Development with Docker
1. Make code changes
2. Rebuild affected service:
   ```bash
   sbt assembly  # for Scala services
   docker-compose build service-name
   ```
3. Restart service:
   ```bash
   docker-compose up -d service-name
   ```
4. View logs:
   ```bash
   docker-compose logs -f service-name
   ```

### Hot Reload (Development)
For faster development without Docker:
1. Run PostgreSQL in Docker only
2. Run services locally with SBT
3. Run frontend with npm start

### Database Access
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres

# Connect to specific database
docker-compose exec postgres psql -U orderuser -d orderdb

# Run SQL file
docker-compose exec -T postgres psql -U postgres < script.sql
```

## Troubleshooting

### Port Already in Use
- Check if port is in use: `lsof -i :PORT` or `netstat -an | grep PORT`
- Stop conflicting service or change port in docker-compose.yml

### Service Won't Start
- Check logs: `docker-compose logs service-name`
- Verify dependencies are running
- Check environment variables
- Ensure ports aren't conflicting

### Database Connection Issues
- Verify database is running: `docker-compose ps postgres`
- Check connection string in service config
- Verify database initialization scripts ran
- Check network connectivity between services

### Out of Disk Space
- Remove unused images: `docker image prune -a`
- Remove unused volumes: `docker volume prune`
- Remove unused containers: `docker container prune`
- Full cleanup: `docker system prune -a --volumes`

### Networking Issues
- Verify services are on same network
- Use service names, not localhost
- Check firewall rules
- Restart Docker daemon if needed

## Production Considerations

### Image Optimization
- Use multi-stage builds
- Minimize layers
- Remove build tools from runtime images
- Use specific version tags, not `latest`

### Resource Limits
```yaml
services:
  service-name:
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          memory: 256M
```

### Logging
- Use structured logging
- Configure log rotation
- Use logging drivers for centralization
- Consider ELK stack or similar

### Monitoring
- Add health check endpoints
- Use Prometheus for metrics
- Implement readiness and liveness probes
- Monitor container resource usage

### Security
- Use secrets management (Docker secrets, Vault)
- Scan images regularly
- Update base images
- Run containers as non-root
- Use read-only filesystems where possible

## CI/CD Integration

### GitHub Actions Example
```yaml
- name: Build Docker images
  run: |
    sbt assembly
    docker-compose build

- name: Run tests
  run: docker-compose up -d && docker-compose run tests

- name: Push to registry
  run: |
    docker tag image:latest registry/image:${{ github.sha }}
    docker push registry/image:${{ github.sha }}
```

## Migration to Kubernetes

When ready for production Kubernetes deployment:
- Convert services to Kubernetes Deployments
- Use StatefulSets for databases
- Configure Ingress for external access
- Use ConfigMaps and Secrets
- Implement proper health checks
- Use Persistent Volume Claims
- Configure resource requests/limits
