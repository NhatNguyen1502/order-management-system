# Architecture Overview

This document provides an in-depth overview of the Order Management System architecture.

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser                                 │
│                      (Angular 19 SPA)                           │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP/REST
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API Gateway                                │
│                     (Akka HTTP)                                 │
│                      Port: 8080                                 │
└───────┬──────────────────┬──────────────────┬───────────────────┘
        │ gRPC             │ gRPC             │ gRPC
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Order      │  │  Inventory   │  │   Product    │
│  Service     │  │   Service    │  │   Service    │
│  Port: 8081  │  │  Port: 8082  │  │  Port: 8083  │
└──────┬───────┘  └──────┬───────┘  └──────────────┘
       │                 │
       │ JDBC            │ JDBC
       ▼                 ▼
┌─────────────────────────────────────────────┐
│            PostgreSQL                       │
│          Port: 5432                         │
│  ┌─────────────┐    ┌──────────────┐        │
│  │  orderdb    │    │ inventorydb  │        │
│  └─────────────┘    └──────────────┘        │
└─────────────────────────────────────────────┘
```

## Component Details

### 1. Frontend (Angular 19)

**Technology Stack:**
- Angular 19 with standalone components
- Angular Material for UI components
- RxJS for reactive programming
- TypeScript for type safety
- SCSS for styling

**Key Features:**
- Standalone component architecture (no NgModules)
- Lazy-loaded routes for optimal performance
- Reactive forms with validation
- HTTP interceptors ready for auth (future)
- Material Design UI

**Structure:**
```
frontend/src/app/
├── components/          # UI Components
│   ├── product-list/   # Product catalog
│   ├── order-list/     # Order management
│   └── inventory-list/ # Inventory tracking
├── services/           # API Services
│   ├── product.service.ts
│   ├── order.service.ts
│   └── inventory.service.ts
├── models/             # TypeScript interfaces
│   └── models.ts
├── app.component.*     # Root component
├── app.config.ts       # App configuration
└── app.routes.ts       # Route definitions
```

### 2. API Gateway (Akka HTTP)

**Technology Stack:**
- Akka HTTP for REST endpoints
- Akka gRPC client for service communication
- Spray JSON for JSON serialization
- Akka HTTP CORS for cross-origin requests

**Responsibilities:**
- REST API exposure for frontend
- Request routing to microservices
- Protocol translation (HTTP → gRPC)
- CORS handling
- (Future) Authentication/Authorization
- (Future) Rate limiting
- (Future) Request logging

**Endpoints:**
```
/api/products      - Product operations
/api/orders        - Order operations
/api/inventory     - Inventory operations
```

### 3. Order Service

**Technology Stack:**
- Akka gRPC for service interface
- Akka Persistence for event sourcing
- Akka Cluster Sharding for entity distribution
- Akka Projection for read model updates
- PostgreSQL for event journal and read models

**Architecture Pattern: Event Sourcing + CQRS**

**Event Sourcing:**
- All state changes stored as events
- Complete audit trail
- Temporal queries possible
- Event replay capabilities

**CQRS (Command Query Responsibility Segregation):**
- Write side: Event-sourced entities
- Read side: Materialized views in PostgreSQL
- Akka Projection bridges write and read sides

**Entity Structure:**
```scala
OrderEntity
├── Commands (write operations)
│   ├── CreateOrder
│   ├── UpdateStatus
│   └── GetOrder
├── Events (what happened)
│   ├── OrderCreated
│   └── OrderStatusUpdated
└── State (current state)
    └── OrderState
```

**Database Schema:**
```sql
-- Event Store
journal(persistence_id, sequence_number, message)
snapshot(persistence_id, sequence_number, snapshot)

-- Read Models
orders(order_id, customer_id, status, total_amount, ...)
order_items(id, order_id, product_id, quantity, price)

-- Projections
akka_projection_offset_store(projection_name, current_offset)
```

**Akka Persistence Configuration:**
- Journal: JDBC (PostgreSQL)
- Snapshot Store: JDBC (PostgreSQL)
- Serialization: Jackson JSON

### 4. Inventory Service

**Technology Stack:**
- Akka gRPC for service interface
- Akka Persistence for event sourcing
- Akka Cluster Sharding
- PostgreSQL for persistence

**Architecture Pattern: Event Sourcing**

Similar to Order Service but focused on inventory management:

**Entity Structure:**
```scala
InventoryEntity
├── Commands
│   ├── CheckInventory
│   ├── ReserveInventory
│   ├── ReleaseInventory
│   └── UpdateInventory
├── Events
│   ├── InventoryReserved
│   ├── InventoryReleased
│   └── InventoryUpdated
└── State
    └── InventoryState
```

**Key Features:**
- Real-time inventory tracking
- Reservation management for orders
- Eventual consistency with order service
- Optimistic concurrency control

### 5. Product Service

**Technology Stack:**
- Akka gRPC for service interface
- In-memory storage (for skeleton)
- Stateless design

**Architecture Pattern: Simple CRUD**

Simpler than other services, demonstrates stateless microservice pattern.

**Features:**
- Product catalog management
- CRUD operations
- No persistence (skeleton uses in-memory storage)
- Easily scalable (stateless)

## Communication Patterns

### 1. Frontend ↔ API Gateway
- Protocol: HTTP/REST
- Format: JSON
- Style: Request-Response
- Authentication: (Future) JWT tokens

### 2. API Gateway ↔ Microservices
- Protocol: gRPC (HTTP/2)
- Format: Protocol Buffers
- Style: Request-Response
- Benefits:
  - Type-safe contracts
  - Better performance than REST
  - Built-in code generation
  - Streaming support (future)

### 3. Microservices ↔ Database
- Protocol: JDBC
- Pattern: Event Sourcing (Order, Inventory)
- Isolation: Separate databases per service
- Transactions: Single-service only (no distributed transactions)

## Data Flow Examples

### Creating an Order

```
1. User clicks "Create Order" in UI
   ↓
2. Angular sends POST /api/orders
   ↓
3. API Gateway receives REST request
   ↓
4. API Gateway calls OrderService.createOrder() via gRPC
   ↓
5. Order Service:
   - Validates request
   - Generates order ID
   - Persists OrderCreated event
   - Updates entity state
   - Returns response
   ↓
6. (Async) Akka Projection:
   - Reads OrderCreated event
   - Updates orders table (read model)
   - Updates order_items table
   ↓
7. API Gateway returns HTTP response to frontend
   ↓
8. UI updates to show new order
```

### Checking Inventory

```
1. Before order creation, check inventory
   ↓
2. API calls InventoryService.checkInventory()
   ↓
3. Inventory Service:
   - Queries entity state
   - Checks available quantity
   - Returns availability
   ↓
4. If available, proceed with order
   If not, show error to user
```

## Persistence Strategy

### Event Journal (Order & Inventory Services)

**Write Path:**
```
Command → Validate → Event → Persist → Update State → Response
```

**Read Path:**
```
Query → Load Events → Replay → Current State
```

**Benefits:**
- Complete audit trail
- Point-in-time recovery
- Debugging capabilities
- Event replay for new projections

### Read Models (CQRS)

**Purpose:**
- Optimized for queries
- Denormalized data
- Fast reads
- Eventually consistent

**Update Mechanism:**
```
Event Journal → Akka Projection → Read Model Tables
```

**Tables:**
- orders: Denormalized order data
- order_items: Order line items
- inventory: Current inventory levels
- inventory_reservations: Active reservations

## Scalability Considerations

### Horizontal Scaling

**Stateless Services (Product, API Gateway):**
- Can be scaled by adding more instances
- Load balancer distributes requests
- No coordination needed

**Stateful Services (Order, Inventory):**
- Use Akka Cluster Sharding
- Entities distributed across cluster nodes
- Sharding based on entity ID
- Automatic rebalancing

### Akka Cluster Sharding

```
Entity Distribution Example (3 nodes):

Node 1: Orders [A-F]
Node 2: Orders [G-M]
Node 3: Orders [N-Z]

Each order entity lives on exactly one node
Messages routed automatically to correct node
```

### Database Scaling

**Current Setup:**
- Single PostgreSQL instance
- Separate databases per service

**Future Options:**
- Read replicas for read models
- Write/Read separation
- Sharding by service
- Connection pooling (HikariCP already configured)

## Resilience Patterns

### Circuit Breaker
- (Future) Prevents cascade failures
- Fast-fail when service is down
- Automatic recovery

### Retry Logic
- (Future) Transient failure handling
- Exponential backoff
- Maximum retry limits

### Timeouts
- Akka Ask pattern timeouts (5 seconds)
- HTTP request timeouts
- Database query timeouts

### Supervision
- Akka supervision strategies
- Automatic actor restart on failure
- Isolated failure domains

## Security Considerations

### Current State (Skeleton)
- No authentication/authorization
- No encryption
- No input validation beyond type checking

### Future Enhancements Needed
1. **Authentication:**
   - JWT tokens
   - OAuth2/OIDC integration
   - Token refresh mechanism

2. **Authorization:**
   - Role-based access control (RBAC)
   - Service-level permissions
   - Resource-level permissions

3. **Transport Security:**
   - TLS for all communications
   - Certificate management
   - Mutual TLS for service-to-service

4. **Data Security:**
   - Encryption at rest
   - PII handling
   - Audit logging

5. **Input Validation:**
   - Request validation
   - SQL injection prevention
   - XSS prevention

## Monitoring & Observability

### Current State
- Basic logging (Logback)
- Console output

### Recommended Additions
1. **Metrics:**
   - Prometheus for metrics collection
   - Grafana for visualization
   - Key metrics: request rate, latency, errors

2. **Tracing:**
   - Jaeger or Zipkin
   - Distributed transaction tracing
   - Service dependency mapping

3. **Logging:**
   - Centralized logging (ELK stack)
   - Structured logging
   - Log aggregation

4. **Health Checks:**
   - Liveness probes
   - Readiness probes
   - Dependency health checks

## Deployment Architecture

### Development (Docker Compose)
```
Single Host
├── PostgreSQL container
├── Order Service container
├── Inventory Service container
├── Product Service container
├── API Gateway container
└── Frontend (Nginx) container
```

### Production (Future - Kubernetes)
```
Kubernetes Cluster
├── PostgreSQL StatefulSet
├── Order Service Deployment (3 replicas)
├── Inventory Service Deployment (3 replicas)
├── Product Service Deployment (3 replicas)
├── API Gateway Deployment (2 replicas)
├── Frontend Deployment (2 replicas)
├── Ingress Controller
└── Persistent Volumes
```

## Testing Strategy

### Unit Tests
- Individual component testing
- Mock dependencies
- Fast feedback

### Integration Tests
- Service-to-service communication
- Database interactions
- gRPC contract testing

### End-to-End Tests
- Full user flow testing
- Selenium/Cypress for UI
- Complete stack validation

### Performance Tests
- Load testing with Gatling
- Stress testing
- Capacity planning

## Technology Decisions

### Why Akka?
- Battle-tested for reactive systems
- Built-in clustering and sharding
- Excellent persistence support
- Actor model for concurrency
- Strong ecosystem

### Why gRPC?
- Better performance than REST
- Type-safe contracts
- Built-in code generation
- HTTP/2 benefits
- Streaming support

### Why Event Sourcing?
- Complete audit trail
- Temporal queries
- Event replay capabilities
- Natural fit for domain events
- CQRS support

### Why Angular?
- Strong TypeScript support
- Comprehensive framework
- Rich ecosystem
- Material Design components
- Enterprise-ready

### Why PostgreSQL?
- ACID compliance
- Rich feature set
- JSON support for flexibility
- Strong Akka Persistence support
- Battle-tested reliability

## Known Limitations (Skeleton)

1. **No distributed transactions** - Each service has independent database
2. **No saga pattern** - No coordinated multi-service transactions
3. **Simple error handling** - Needs circuit breakers, retries
4. **No authentication** - Security layer not implemented
5. **In-memory Product Service** - No persistence for products
6. **Basic read models** - Projection handlers not fully implemented
7. **No API versioning** - Breaking changes would affect clients
8. **Single database instance** - No replication or failover

## Extending the System

### Adding a New Service

1. Create new SBT module in build.sbt
2. Define gRPC proto file
3. Implement service logic
4. Add to docker-compose.yml
5. Update API Gateway routing
6. Add frontend integration

### Adding New Features

1. **Payment Service:**
   - Event-sourced payment processing
   - Integration with payment providers
   - PCI compliance considerations

2. **Shipping Service:**
   - Shipping provider integration
   - Tracking information
   - Delivery notifications

3. **Notification Service:**
   - Email notifications
   - SMS notifications
   - Push notifications

4. **Analytics Service:**
   - Event stream processing
   - Real-time dashboards
   - Business intelligence

## Conclusion

This architecture provides a solid foundation for a scalable, maintainable microservices system. While the current implementation is a skeleton, it demonstrates key patterns and practices that can be extended for production use.

The combination of event sourcing, CQRS, and microservices provides:
- **Scalability:** Through sharding and stateless services
- **Resilience:** Through isolation and supervision
- **Maintainability:** Through clear boundaries and separation of concerns
- **Auditability:** Through event sourcing
- **Flexibility:** Through loosely coupled services
