# Quick Start Guide

Get the Order Management System up and running in minutes!

## Prerequisites Check

Before starting, ensure you have:

```bash
# Check Docker
docker --version
# Should show: Docker version 20.x or higher

# Check Docker Compose
docker-compose --version
# Should show: docker-compose version 2.x or higher

# Check Java (for local development only)
java -version
# Should show: openjdk version "11" or higher

# Check SBT (for local development only)
sbt --version
# Should show: sbt version 1.9.x or higher

# Check Node.js (for local development only)
node --version
# Should show: v20.x or higher

# Check npm (for local development only)
npm --version
# Should show: 10.x or higher
```

## Option A: Docker Compose (Fastest)

### Step 1: Clone the Repository

```bash
git clone https://github.com/NhatNguyen1502/order-management-system.git
cd order-management-system
```

### Step 2: Build Scala Services

```bash
# This will compile all services and create fat JARs
sbt assembly
```

**Note:** First build may take 5-10 minutes as it downloads dependencies.

Expected output:
```
[success] Total time: 300 s (05:00), completed ...
```

### Step 3: Start All Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database
- Order Service
- Inventory Service
- Product Service
- API Gateway
- Frontend (Nginx)

### Step 4: Verify Services

```bash
# Check all containers are running
docker-compose ps

# Expected output shows all services as "Up"
# NAME                    STATUS
# api-gateway             Up
# frontend                Up
# inventory-service       Up
# order-service           Up
# product-service         Up
# order-management-postgres Up
```

### Step 5: View Logs (Optional)

```bash
# Follow all logs
docker-compose logs -f

# Follow specific service logs
docker-compose logs -f api-gateway
docker-compose logs -f order-service
```

### Step 6: Access the Application

Open your browser and navigate to:
- **Frontend UI:** http://localhost:4200
- **API Gateway:** http://localhost:8080

### Step 7: Test the API

```bash
# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "category": "Electronics"
  }'

# List products
curl http://localhost:8080/api/products

# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-1",
    "items": [
      {
        "productId": "product-1",
        "productName": "Laptop",
        "quantity": 1,
        "price": 999.99
      }
    ]
  }'
```

### Step 8: Stop Services

```bash
# Stop all services
docker-compose down

# Stop and remove all data
docker-compose down -v
```

---

## Option B: Local Development

For development with hot-reload capabilities.

### Step 1: Clone and Setup

```bash
git clone https://github.com/NhatNguyen1502/order-management-system.git
cd order-management-system
```

### Step 2: Start PostgreSQL

```bash
docker run -d \
  --name postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -v $(pwd)/docker/postgres/init:/docker-entrypoint-initdb.d \
  postgres:15-alpine
```

Wait a few seconds for initialization, then verify:

```bash
docker logs postgres
# Should see: "database system is ready to accept connections"
```

### Step 3: Compile Services

```bash
# Compile all services (generates gRPC code)
sbt compile
```

### Step 4: Start Backend Services

Open **4 separate terminals** and run:

**Terminal 1 - Product Service:**
```bash
cd order-management-system
sbt "product-service/run"
# Wait for: "Product gRPC server bound to 0.0.0.0:8083"
```

**Terminal 2 - Inventory Service:**
```bash
cd order-management-system
sbt "inventory-service/run"
# Wait for: "Inventory gRPC server bound to 0.0.0.0:8082"
```

**Terminal 3 - Order Service:**
```bash
cd order-management-system
sbt "order-service/run"
# Wait for: "Order gRPC server bound to 0.0.0.0:8081"
```

**Terminal 4 - API Gateway:**
```bash
cd order-management-system
sbt "api-gateway/run"
# Wait for: "API Gateway online at http://0.0.0.0:8080/"
```

### Step 5: Start Frontend

Open another terminal:

```bash
cd order-management-system/frontend
npm install
npm start
```

Wait for: "Application bundle generation complete"

### Step 6: Access the Application

- **Frontend:** http://localhost:4200
- **API Gateway:** http://localhost:8080

### Step 7: Making Changes

With this setup:
- **Scala changes:** Stop service (Ctrl+C), then restart with `sbt "service-name/run"`
- **Angular changes:** Automatically reload on save
- **Proto changes:** Run `sbt compile` to regenerate code

---

## Troubleshooting

### Problem: Port already in use

**Solution:**
```bash
# Check what's using the port
lsof -i :8080  # or any other port
kill -9 <PID>  # Kill the process
```

Or change ports in `docker-compose.yml` or application configs.

### Problem: Docker Compose fails to start

**Solution:**
```bash
# Clean up everything
docker-compose down -v
docker system prune -f

# Rebuild and start
docker-compose up --build
```

### Problem: SBT build errors or dependency not found

**Solution:**
```bash
# Clean and rebuild
sbt clean compile

# If persistent, clear caches and update dependencies
rm -rf ~/.sbt ~/.ivy2/cache
sbt clean update compile
```

**Common causes:**
- Network issues preventing access to Maven repositories
- Proxy or firewall blocking access to https://repo.akka.io/maven
- Outdated SBT version (requires 1.9.7+)
- Missing Java 11+ installation

**Verify your environment:**
```bash
# Check SBT version (should be 1.9.7+)
sbt --version

# Check Java version (should be 11+)
java -version

# Test Maven repository access
curl -I https://repo.akka.io/maven
```

### Problem: Database connection errors

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check logs
docker logs order-management-postgres

# Restart PostgreSQL
docker restart order-management-postgres
```

### Problem: Frontend won't start

**Solution:**
```bash
cd frontend

# Clear node_modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Clear Angular cache
rm -rf .angular

# Try again
npm start
```

### Problem: Services can't connect to each other

**Solution:**

For Docker Compose:
```bash
# Check network
docker network inspect order-management-system_order-management-network

# Restart all services
docker-compose restart
```

For local development:
- Ensure all services are running on correct ports
- Check service URLs in API Gateway config
- Verify no firewall blocking

---

## Common Development Tasks

### View Database Tables

```bash
# Connect to PostgreSQL
docker exec -it order-management-postgres psql -U postgres

# Switch to database
\c orderdb

# List tables
\dt

# Query orders
SELECT * FROM orders;

# Exit
\q
```

### View Service Logs

```bash
# Docker Compose
docker-compose logs -f service-name

# Local Development
# Logs are in the terminal where you ran sbt
```

### Rebuild a Single Service

```bash
# Docker Compose
docker-compose up -d --build service-name

# Local Development
sbt "service-name/compile"
sbt "service-name/run"
```

### Reset Database

```bash
# Stop services
docker-compose down -v

# This removes all data
# Start fresh
docker-compose up -d
```

---

## Next Steps

Now that you have the system running:

1. **Explore the UI**
   - Navigate to http://localhost:4200
   - Try creating products
   - Explore order management
   - Check inventory tracking

2. **Test the API**
   - Use the curl examples above
   - Try Postman or Insomnia
   - Test different scenarios

3. **Read the Documentation**
   - [README.md](README.md) - Full documentation
   - [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture details
   - Scala code comments

4. **Modify and Extend**
   - Add new endpoints
   - Implement missing features
   - Add authentication
   - Enhance the UI

5. **Deploy to Production**
   - Consider Kubernetes
   - Add monitoring
   - Implement security
   - Scale services

---

## Getting Help

If you encounter issues:

1. Check the logs (Docker or console)
2. Review the [README.md](README.md) troubleshooting section
3. Check the [ARCHITECTURE.md](ARCHITECTURE.md) for design details
4. Verify all prerequisites are met
5. Try a clean restart

---

## Success Indicators

You'll know everything is working when:

✅ All Docker containers show "Up" status
✅ Frontend loads at http://localhost:4200
✅ You can create a product via UI
✅ API returns valid responses
✅ Database contains your test data
✅ No error messages in logs

Enjoy building with the Order Management System! 🚀
