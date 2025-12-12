---
description: 'Guidelines for gRPC and Protocol Buffer definitions'
applyTo: '**/*.proto'
---

# gRPC and Protocol Buffer Guidelines

## General Principles

- Use Protocol Buffers version 3 (proto3)
- Follow Google's Protocol Buffer style guide
- Keep service definitions focused and cohesive
- Design for backward compatibility

## File Structure

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.orderms.<service>.grpc";
option java_outer_classname = "<Service>Proto";

package <service>name;

// Service definition
service <Service>Service {
  rpc Operation(Request) returns (Response);
}

// Message definitions
message Request {
  // fields
}

message Response {
  // fields
}
```

## Naming Conventions

### Services
- Use PascalCase for service names (e.g., `OrderService`, `ProductService`)
- Service name should match the domain entity
- Keep one service per proto file

### RPC Methods
- Use PascalCase for RPC method names
- Use verb + noun pattern (e.g., `CreateOrder`, `GetProduct`, `UpdateInventory`)
- Common verbs: Create, Get, Update, Delete, List, Search

### Messages
- Use PascalCase for message names
- Request messages should end with `Request`
- Response messages should end with `Response`
- Domain entities should use their business name (e.g., `Order`, `Product`)

### Fields
- Use snake_case for field names (e.g., `customer_id`, `order_date`)
- Use singular names unless the field is truly a collection

## Field Numbering

- Never reuse field numbers (even if fields are deleted)
- Reserve numbers 1-15 for frequently used fields (more efficient encoding)
- Use numbers 16+ for less common fields
- Reserve deleted field numbers to prevent reuse:
  ```protobuf
  reserved 2, 15, 9 to 11;
  reserved "old_field_name";
  ```

## Data Types

### Choosing Types
- Use `string` for text, IDs, enums as strings
- Use `int32` for small numbers
- Use `int64` for large numbers, timestamps (milliseconds)
- Use `double` for currency, precise decimals
- Use `bool` for boolean values
- Use `bytes` for binary data
- Use `repeated` for arrays/lists

### Timestamps
- Use `int64` for Unix timestamps (milliseconds since epoch)
- Alternative: Import `google/protobuf/timestamp.proto` for richer timestamp type

### Money/Currency
- Use `double` for amount
- Include separate string field for currency code (ISO 4217)
- Example:
  ```protobuf
  message Price {
    double amount = 1;
    string currency_code = 2;  // e.g., "USD", "EUR"
  }
  ```

## Best Practices

### Request/Response Design
- Always include request/response messages even if empty (for future extensibility)
- Include pagination fields in list requests:
  ```protobuf
  message ListRequest {
    int32 page = 1;
    int32 page_size = 2;
  }
  
  message ListResponse {
    repeated Item items = 1;
    int32 total = 2;
    int32 page = 3;
    int32 page_size = 4;
  }
  ```

### Error Handling
- Use standard gRPC status codes
- Include error details in responses:
  ```protobuf
  message Response {
    bool success = 1;
    string error_message = 2;
    string error_code = 3;
  }
  ```

### Nested Messages
- Define nested messages when they're only used by the parent
- Extract to top-level when reused across multiple messages

### Enums
- Always provide a zero-value default (e.g., `UNKNOWN`, `UNSPECIFIED`)
- Use UPPER_SNAKE_CASE for enum values
  ```protobuf
  enum OrderStatus {
    ORDER_STATUS_UNKNOWN = 0;
    ORDER_STATUS_PENDING = 1;
    ORDER_STATUS_CONFIRMED = 2;
    ORDER_STATUS_SHIPPED = 3;
    ORDER_STATUS_DELIVERED = 4;
    ORDER_STATUS_CANCELLED = 5;
  }
  ```

### Optional Fields
- In proto3, all fields are optional by default
- Use `optional` keyword when you need to distinguish between not-set and default value
- Use `repeated` for collections

### Deprecation
- Mark deprecated fields with `[deprecated = true]`
- Add comment explaining why and what to use instead
  ```protobuf
  string old_field = 5 [deprecated = true];  // Use new_field instead
  string new_field = 6;
  ```

## Code Generation

### Scala/Akka
- gRPC code is automatically generated during `sbt compile`
- Generated code location: `target/scala-2.13/src_managed/main/`
- Never manually edit generated code

### Configuration
- gRPC settings in `project/plugins.sbt`:
  ```scala
  addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.5.8")
  ```

## Service Implementation

### In Scala
```scala
import com.orderms.order.grpc._
import scala.concurrent.Future

class OrderServiceImpl extends OrderService {
  override def createOrder(request: CreateOrderRequest): Future[CreateOrderResponse] = {
    // Implementation
  }
  
  override def getOrder(request: GetOrderRequest): Future[GetOrderResponse] = {
    // Implementation
  }
}
```

### Best Practices for Implementation
- Validate all request parameters
- Use proper error handling with gRPC status codes
- Set appropriate timeouts
- Log requests and responses (except sensitive data)
- Use async/Future for all operations

## Testing

- Test proto files compile successfully
- Test generated code is correct
- Write integration tests for service implementations
- Test error conditions and edge cases

## Versioning

### API Versioning Strategy
- Add new fields rather than modifying existing ones
- Add new RPC methods rather than changing signatures
- Use separate proto files for major version changes
- Example: `v1/order_service.proto`, `v2/order_service.proto`

### Backward Compatibility
- Never remove required fields
- Never change field types
- Never change field numbers
- Always add, never modify

## Documentation

- Add comments to services, methods, and messages
- Document field constraints and validation rules
- Explain business logic and domain concepts
- Example:
  ```protobuf
  // Service for managing customer orders
  service OrderService {
    // Creates a new order for a customer
    // Returns the created order ID and initial status
    rpc CreateOrder(CreateOrderRequest) returns (CreateOrderResponse);
  }
  
  message Order {
    // Unique identifier for the order (UUID format)
    string order_id = 1;
    
    // Customer ID (must be valid and active)
    string customer_id = 2;
    
    // Order items (must contain at least one item)
    repeated OrderItem items = 3;
    
    // Total order amount in the store's currency
    double total_amount = 5;
  }
  ```

## Common Patterns

### CRUD Operations
```protobuf
service EntityService {
  rpc Create(CreateRequest) returns (CreateResponse);
  rpc Get(GetRequest) returns (GetResponse);
  rpc Update(UpdateRequest) returns (UpdateResponse);
  rpc Delete(DeleteRequest) returns (DeleteResponse);
  rpc List(ListRequest) returns (ListResponse);
}
```

### Bulk Operations
```protobuf
message BulkCreateRequest {
  repeated CreateRequest requests = 1;
}

message BulkCreateResponse {
  repeated CreateResponse responses = 1;
  repeated string errors = 2;
}
```

### Streaming (Future Enhancement)
```protobuf
// Server streaming
rpc Subscribe(SubscribeRequest) returns (stream Event);

// Client streaming  
rpc Upload(stream Chunk) returns (UploadResponse);

// Bidirectional streaming
rpc Chat(stream Message) returns (stream Message);
```

## Resources

- [Protocol Buffers Documentation](https://protobuf.dev/)
- [Akka gRPC Documentation](https://doc.akka.io/docs/akka-grpc/)
- [gRPC Best Practices](https://grpc.io/docs/guides/performance/)
