# Running Services Locally (Without Docker)

Run all services locally using Maven. API Gateway is the single entry point.

## Prerequisites

- Java 25 JDK (already installed ✓)
- Maven 3.9+ (already installed ✓)
- SQLite (file-based, auto-created on startup)

## Step-by-Step Instructions

### Option 1: Run Services in Separate Terminals

#### Terminal 0 - Start Service Registry

```bash
cd service-registry
mvn spring-boot:run
```

Wait for the message: `Started ServiceRegistryApplication in X.XXX seconds`

#### Terminal 1 - Start User Service

```bash
cd user-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
mvn spring-boot:run
```

Wait for the message: `Started UserServiceApplication in X.XXX seconds`

#### Terminal 2 - Start Inventory Service

```bash
cd inventory-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
mvn spring-boot:run
```

Wait for the message: `Started InventoryServiceApplication in X.XXX seconds`

#### Terminal 3 - Start Order Service

```bash
cd order-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
mvn spring-boot:run
```

Wait for the message: `Started OrderServiceApplication in X.XXX seconds`

#### Terminal 4 - Start API Gateway

```bash
cd api-gateway
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
mvn spring-boot:run
```

Wait for the message: `Started ApiGatewayApplication in X.XXX seconds`

### Option 2: Run Services in Background (Windows)

#### Start Service Registry in Background

```bash
cd service-registry
start "Service Registry" cmd /k "mvn spring-boot:run"
```

#### Start User Service in Background

```bash
cd user-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
start "User Service" cmd /k "mvn spring-boot:run"
```

#### Start Inventory Service in Background

```bash
cd inventory-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
start "Inventory Service" cmd /k "mvn spring-boot:run"
```

#### Start Order Service in Background

```bash
cd order-service
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
start "Order Service" cmd /k "mvn spring-boot:run"
```

#### Start API Gateway in Background

```bash
cd api-gateway
set GATEWAY_INTERNAL_TOKEN=gombey-gateway-internal-token
start "API Gateway" cmd /k "mvn spring-boot:run"
```

## Verify Services are Running

1. **Service Registry**: http://localhost:8761
2. **API Gateway**: http://localhost:8080
3. **User Service**: http://localhost:8083 (direct access blocked without gateway token)
4. **Inventory Service**: http://localhost:8082 (direct access blocked without gateway token)
5. **Order Service**: http://localhost:8081 (direct access blocked without gateway token)

## Test the Services

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/signup ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user123\",\"email\":\"user@example.com\",\"password\":\"password123\"}"
```

### 2. Login and Get Token

```bash
curl -X POST http://localhost:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"user123\",\"password\":\"password123\"}"
```

### 3. Check Inventory Stock

```bash
curl http://localhost:8080/api/inventory/ITEM-001 ^
  -H "Authorization: Bearer <token>"
```

### 4. Create an Order

```bash
curl -X POST http://localhost:8080/api/orders ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer <token>" ^
  -d "{\"customerName\":\"John Doe\",\"customerEmail\":\"john@example.com\",\"items\":[{\"itemCode\":\"ITEM-001\",\"quantity\":2}]}"
```

### 5. Get Order by ID

```bash
curl http://localhost:8080/api/orders/1 ^
  -H "Authorization: Bearer <token>"
```

## Sample Data

The Inventory Service automatically loads sample data from `data.sql`:
- ITEM-001: Laptop (50 units)
- ITEM-002: Mouse (100 units)
- ITEM-003: Keyboard (75 units)
- ITEM-004: Monitor (30 units)
- ITEM-005: Headphones (60 units)

## Troubleshooting

### Port Already in Use

If you get a "port already in use" error:

1. **Find the process using the port:**
   ```bash
   netstat -ano | findstr :8081
   netstat -ano | findstr :8082
   ```

2. **Kill the process:**
   ```bash
   taskkill /PID <process_id> /F
   ```

### Service Not Starting

- Check Java version: `java -version` (should be 25)
- Check Maven: `mvn -version`
- Clean and rebuild: `mvn clean install`

## Stopping Services

Press `Ctrl+C` in each terminal window to stop the services.
