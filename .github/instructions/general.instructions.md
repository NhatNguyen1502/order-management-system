---
description: 'General guidelines for the Order Management System repository'
applyTo: '**/*'
---

# Order Management System - General Guidelines

This repository contains a microservices-based order management system built with Scala, Akka, and Angular.

## Repository Structure

```
.
├── api-gateway/              # API Gateway service (Akka HTTP)
├── order-service/            # Order microservice (Akka gRPC + Persistence + Projection)
├── inventory-service/        # Inventory microservice (Akka gRPC + Persistence)
├── product-service/          # Product microservice (Akka gRPC)
├── frontend/                 # Angular 20 frontend application
├── docker/                   # Docker configurations
├── build.sbt                 # SBT build configuration
└── docker-compose.yml        # Docker Compose setup
```

## Technology Stack

### Backend
- Scala 2.13.17
- Akka 2.10.12 (Actor, Cluster, Persistence, HTTP)
- Akka gRPC 2.5.8
- Akka Projection 1.6.16
- PostgreSQL 15
- JDBC for persistence

### Frontend
- Angular 20 with standalone components
- Angular Material
- TypeScript
- RxJS
- SCSS

## Development Workflow

### Building Scala Services

```bash
# Compile all services
sbt compile

# Run tests
sbt test

# Create fat JARs for deployment
sbt assembly
```

### Running Services Locally

In separate terminals:

```bash
# Terminal 1: Product Service
sbt "productService/run"

# Terminal 2: Inventory Service
sbt "inventoryService/run"

# Terminal 3: Order Service
sbt "orderService/run"

# Terminal 4: API Gateway
sbt "apiGateway/run"
```

### Frontend Development

```bash
cd frontend
npm install
npm start
```

### Docker Compose

```bash
# Build and start all services
sbt assembly
docker-compose up -d

# Stop services
docker-compose down
```

## Code Organization Principles

1. **Domain-Driven Design**: Organize code by feature/domain, not by technical layer
2. **Separation of Concerns**: Keep business logic separate from infrastructure
3. **Immutability**: Prefer immutable data structures
4. **Type Safety**: Use strong typing throughout the codebase
5. **Testability**: Write testable code with clear dependencies

## Architecture Patterns

### Backend Services

- **Event Sourcing**: Order and Inventory services store all state changes as events
- **CQRS**: Separate write (commands) and read (queries) models
- **Actor Model**: Akka actors for concurrency and state management
- **gRPC**: Type-safe inter-service communication

### Communication

- **Frontend ↔ API Gateway**: HTTP/REST with JSON
- **API Gateway ↔ Services**: gRPC with Protocol Buffers
- **Services ↔ Database**: JDBC with connection pooling

## Testing Strategy

- Unit tests for individual components
- Integration tests for service-to-service communication
- End-to-end tests for full user flows
- Always run existing tests before and after making changes

## Security Considerations

- Never commit secrets or credentials
- Use environment variables for sensitive configuration
- Validate all inputs at API boundaries
- Use prepared statements for database queries
- Follow principle of least privilege

## Documentation

- Keep README.md and ARCHITECTURE.md up to date
- Document significant design decisions
- Add comments for complex business logic
- Update API documentation when endpoints change

## Common Commands

### SBT
```bash
sbt compile           # Compile all services
sbt test             # Run all tests
sbt clean            # Clean build artifacts
sbt assembly         # Create fat JARs
sbt "project orderService" run  # Run specific service
```

### Docker
```bash
docker-compose up -d        # Start all services
docker-compose logs -f      # View logs
docker-compose down         # Stop all services
docker-compose down -v      # Stop and remove volumes
```

### Frontend
```bash
npm install          # Install dependencies
npm start            # Start dev server (port 4200)
npm run build        # Build for production
npm test             # Run tests
npm run lint         # Lint code
```

## Troubleshooting

### Port Conflicts
Modify port mappings in `docker-compose.yml` if needed.

### Database Connection Issues
- Ensure PostgreSQL is running
- Check connection settings in service configuration files
- Verify database schemas are initialized

### Dependency Resolution
- Ensure Maven repositories are accessible
- Check SBT version (requires 1.9.7+)
- Verify Java version (requires JDK 11+)

## Contributing

1. Create a feature branch from main
2. Make minimal, focused changes
3. Run tests to ensure nothing breaks
4. Update documentation if needed
5. Submit a pull request with clear description
