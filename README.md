# Order Management System

A microservices-based order management system built with Scala, Akka, and Angular.

## Architecture

This project implements a modern microservices architecture with the following components:

### Backend Services (Scala/Akka)

- **API Gateway** (Akka HTTP) - Port 8080
  - REST API gateway for frontend communication
  - Routes requests to appropriate microservices
  - CORS support for web applications

- **Order Service** (Akka gRPC + Akka Persistence + Akka Projection) - Port 8081
  - Event-sourced order management
  - CQRS with read models in PostgreSQL
  - gRPC interface for inter-service communication
  - Akka Persistence for event storage
  - Akka Projection for read model updates

- **Inventory Service** (Akka gRPC + Akka Persistence) - Port 8082
  - Event-sourced inventory tracking
  - Reservation management
  - Real-time inventory updates
  - PostgreSQL for persistence

- **Product Service** (Akka gRPC) - Port 8083
  - Stateless product catalog service
  - In-memory product storage (for skeleton)
  - gRPC interface

### Frontend (Angular 19+)

- Modern Angular application with standalone components
- Angular Material UI components
- Lazy-loaded routes for better performance
- HTTP client for API communication
- Proxy configuration for local development

### Infrastructure

- **PostgreSQL** - Port 5432
  - Separate databases for Order and Inventory services
  - Event journal storage
  - Read model storage
  - Projection offset tracking

- **Docker Compose**
  - Complete local development environment
  - Network isolation
  - Volume management for data persistence

## Project Structure

```
.
├── api-gateway/              # API Gateway service
│   └── src/main/scala/com/orderms/gateway/
├── order-service/            # Order microservice
│   └── src/main/
│       ├── protobuf/        # gRPC definitions
│       ├── scala/com/orderms/order/
│       └── resources/       # Configuration
├── inventory-service/        # Inventory microservice
│   └── src/main/
│       ├── protobuf/        # gRPC definitions
│       ├── scala/com/orderms/inventory/
│       └── resources/       # Configuration
├── product-service/          # Product microservice
│   └── src/main/
│       ├── protobuf/        # gRPC definitions
│       ├── scala/com/orderms/product/
│       └── resources/       # Configuration
├── frontend/                 # Angular frontend application
│   └── src/
│       └── app/
│           ├── components/  # UI components
│           ├── services/    # API services
│           └── models/      # TypeScript models
├── docker/                   # Docker configurations
│   ├── postgres/init/       # Database initialization
│   └── */Dockerfile         # Service Dockerfiles
├── build.sbt                 # SBT build configuration
├── docker-compose.yml        # Docker Compose setup
└── README.md
```

## Prerequisites

### For Local Development

- **JDK 11+** - For Scala services
- **SBT 1.9+** - Scala build tool
- **Node.js 20+** - For Angular frontend
- **npm 10+** - Node package manager
- **Docker & Docker Compose** - For containerized deployment

### For Docker-only Deployment

- **Docker 20+**
- **Docker Compose 2+**

## Getting Started

### Option 1: Local Development (without Docker)

#### 1. Start PostgreSQL

```bash
# Using Docker
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  -v $(pwd)/docker/postgres/init:/docker-entrypoint-initdb.d \
  postgres:15-alpine
```

#### 2. Build Scala Services

```bash
# Compile all services
sbt compile
```

#### 3. Run Services

In separate terminals:

```bash
# Terminal 1: Product Service
sbt "productService/run"

# Terminal 2: Inventory Service
sbt "inventoryService/run"

# NOTE: If server says "Create a new server? y/n (default y)", press Enter

# Terminal 3: Order Service
sbt "orderService/run"

# Terminal 4: API Gateway
sbt "apiGateway/run"

```

#### 4. Run Frontend

```bash
cd frontend
npm install
npm start
```

Access the application at: http://localhost:4200

### Option 2: Docker Compose (Recommended)

#### 1. Build Scala Services

```bash
# Build all services with assembly
sbt assembly
```

#### 2. Start All Services

```bash
docker-compose up -d
```

#### 3. Check Service Health

```bash
# View logs
docker-compose logs -f

# Check running containers
docker-compose ps
```

#### 4. Access the Application

- **Frontend**: http://localhost:4200
- **API Gateway**: http://localhost:8080
- **Order Service gRPC**: localhost:8081
- **Inventory Service gRPC**: localhost:8082
- **Product Service gRPC**: localhost:8083
- **PostgreSQL**: localhost:5432

#### 5. Stop Services

```bash
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

## API Endpoints

### Products API

- `GET /api/products` - List all products
  - Query params: `category`, `page`, `pageSize`
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create new product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

### Orders API

- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order by ID
- `PUT /api/orders/{id}/status` - Update order status

### Inventory API

- `GET /api/inventory/{productId}` - Get inventory for product
- `POST /api/inventory/update` - Update inventory quantity

## Development

### Building

```bash
# Compile all Scala services
sbt compile

# Run tests
sbt test

# Create fat JARs for deployment
sbt assembly
```

### Code Generation

gRPC code is automatically generated from `.proto` files during compilation.

### Database Migrations

Database schemas are automatically created on first startup via the initialization script in `docker/postgres/init/01-init.sh`.

## Technology Stack

### Backend
- Scala 2.13
- Akka 2.10.12 (Actor, Cluster, Persistence, HTTP)
- Akka gRPC 2.5.8
- Akka Projection 1.6.16
- PostgreSQL 15
- JDBC for persistence

### Frontend
- Angular 20
- Angular Material
- TypeScript
- RxJS
- SCSS

### Infrastructure
- Docker
- Docker Compose
- Nginx (for frontend serving)

## Configuration

### Scala Services

Configuration is managed via Typesafe Config (application.conf files in each service).

Key configuration areas:
- Database connection settings
- Akka cluster configuration
- gRPC server settings
- Persistence journal and snapshot store
- Projection settings

### Angular Frontend

Configuration in:
- `angular.json` - Angular CLI configuration
- `proxy.conf.json` - Development proxy settings
- Environment variables (future enhancement)

## Future Enhancements

This is a skeleton implementation. Consider adding:

1. **Security**
   - Authentication and authorization (JWT, OAuth2)
   - API rate limiting
   - Input validation and sanitization

2. **Observability**
   - Distributed tracing (Jaeger, Zipkin)
   - Metrics collection (Prometheus)
   - Centralized logging (ELK stack)
   - Health checks and monitoring

3. **Resilience**
   - Circuit breakers
   - Retry policies
   - Bulkheads
   - Service mesh (Istio)

4. **Testing**
   - Unit tests for all components
   - Integration tests
   - End-to-end tests
   - Load testing

5. **CI/CD**
   - GitHub Actions workflows
   - Automated testing
   - Container registry integration
   - Kubernetes deployment

6. **Features**
   - User management
   - Payment integration
   - Shipping integration
   - Email notifications
   - Advanced search and filtering
   - Real-time updates (WebSocket)

## Troubleshooting

### Port Conflicts

If ports are already in use, modify the port mappings in `docker-compose.yml`.

### Database Connection Issues

Ensure PostgreSQL is running and accessible. Check connection settings in service configuration files.

### Dependency Resolution Issues

If you encounter "dependency not found" errors:

1. **Ensure Maven repositories are accessible:**
   - The build requires access to Maven Central and Akka's Maven repository
   - Check your network connection and proxy settings if behind a corporate firewall

2. **Check SBT version:**
   - This project requires SBT 1.9.7 or higher
   - Run `sbt --version` to verify
   - Update SBT if needed: https://www.scala-sbt.org/download.html

3. **Verify Java version:**
   - JDK 11 or higher is required
   - Run `java -version` to check

### Docker Issues

```bash
# Remove all containers and start fresh
docker-compose down -v
docker-compose up --build

# Check Docker logs
docker-compose logs [service-name]
```

## License

This is a sample project for demonstration purposes.

## Contributing

This is a skeleton project. Feel free to extend and customize it for your needs.
