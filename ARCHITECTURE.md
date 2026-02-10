# Microservices Architecture with API Gateway

## Overview

This project implements a microservices architecture with a dedicated User Management Service and an API Gateway for centralized routing and authentication.
Repository directory: `ORDER-MANAGEMENT`.

## Architecture Diagram

```
                    ┌─────────────────────────────────────────┐
                    │     Service Registry / Eureka (8761)     │
                    └────────────────────┬────────────────────┘
                                         │
┌────────────────────────────────────────┼────────────────────────────────────────┐
│                      API Gateway (8080) │                                         │
│              - Route Management         │  - JWT Authentication                   │
└──────────────┬──────────────┬───────────┴───────────┬──────────────┬──────────────┘
               │              │                       │              │
    ┌──────────▼──────┐  ┌────▼─────┐  ┌──────────────▼──────┐  ┌───▼──────────┐
    │  User Service   │  │  Order   │  │  Payment Service    │  │  Inventory   │
    │    (8083)       │  │ Service  │  │     (8084)           │  │  Service     │
    │ - Auth          │  │  (8081)  │  │ - Create payment    │  │   (8082)     │
    │ - User Mgmt     │  │ - Orders │  │ - Get by order      │  │ - Inventory  │
    │ - JWT Gen       │  │ - Reserve│  │                     │  │ - Products   │
    └─────────────────┘  └────┬─────┘  └─────────────────────┘  └──────────────┘
                               │                    │                     ▲
                               └────────────────────┴─────────────────────┘
                                     (Order → Gateway → Inventory / Payment)
```

## Services

### 0. Service Registry (Port 8761)
**Technology:** Spring Cloud Netflix Eureka Server

**Responsibilities:**
- Service discovery for all microservices
- Enables load-balanced routing via the API Gateway

### 1. API Gateway (Port 8080)
**Technology:** Spring Cloud Gateway

**Responsibilities:**
- Centralized routing for all microservices
- JWT token validation at gateway level
- Coarse-grained authorization by route group
- Gateway-only access enforcement (internal token)
- Circuit breaker protection for downstream calls
- CORS handling
- Load balancing via service discovery (Eureka)

**Routes:**
- `/api/auth/**` → User Service (public)
- `/api/users/**` → User Service (public)
- `/api/orders/**` → Order Service (authenticated)
- `/api/inventory/**` → Inventory Service (authenticated)
- `/api/products/**` → Inventory Service (authenticated)
- `/api/payments/**` → Payment Service (authenticated)

**Features:**
- JWT validation filter for protected routes
- Automatic token validation before forwarding requests
- Adds user information headers (`X-User-Name`, `X-Service-Name`) to downstream services
- Adds `X-Gateway-Token` to enforce gateway-only access
- Circuit breaker fallback responses

### 2. User Service (Port 8083)
**Technology:** Spring Boot 3.4.10, Spring Security, JWT

**Responsibilities:**
- User registration (signup)
- User authentication (login)
- JWT token generation
- User management
- Password encryption (BCrypt)

**Endpoints:**
- `POST /api/auth/signup` - Register new user
- `POST /api/auth/login` - Authenticate and get JWT token
- `POST /api/auth/service-token` - Generate service-to-service token

**Database:** SQLite

### 3. Order Service (Port 8081)
**Technology:** Spring Boot 3.4.10, Spring Security, WebFlux

**Responsibilities:**
- Order creation and management
- Inventory validation via API Gateway

**Endpoints:**
- `POST /api/orders` - Create new order (authenticated)
- `GET /api/orders/{id}` - Get order by ID (authenticated)

**Database:** SQLite

### 4. Inventory Service (Port 8082)
**Technology:** Spring Boot 3.4.10, Spring Security

**Responsibilities:**
- Inventory stock management
- Product management
- **Atomic** stock decrease (conditional UPDATE; prevents overselling under concurrent orders)
- Returns 409 Conflict when insufficient stock

**Endpoints:**
- `GET /api/inventory/{itemCode}` - Get available stock (authenticated)
- `POST /api/inventory/decrease` - Decrease (reserve) stock atomically (authenticated; 409 if insufficient)
- `POST /api/inventory/add` - Add stock (authenticated)
- `GET /api/products` - Get all products (authenticated)
- `POST /api/products` - Create product (authenticated)

**Database:** SQLite

### 5. Payment Service (Port 8084)
**Technology:** Spring Boot 3.4.10, Spring Security

**Responsibilities:**
- Create payment records when orders are placed (called by Order Service via Gateway)
- Retrieve payment by order ID

**Endpoints:**
- `POST /api/payments` - Create payment (authenticated)
- `GET /api/payments/order/{orderId}` - Get payment by order ID (authenticated)

**Database:** SQLite

## Authentication Flow

### User Registration & Login Flow

```
1. Client → API Gateway → User Service
   POST /api/auth/signup
   { username, email, password }
   
2. User Service validates and creates user
   Returns: { id, username, email, createdAt }
   
3. Client → API Gateway → User Service
   POST /api/auth/login
   { username, password }
   
4. User Service validates credentials
   Returns: { token, tokenType: "Bearer", expiresIn: 86400 }
```

### Protected API Call Flow

```
1. Client → API Gateway
   GET /api/orders/1
   Header: Authorization: Bearer <token>
   
2. API Gateway validates JWT token
   - Extracts username and service info
   - Adds X-User-Name and X-Service-Name headers
   - Adds X-Gateway-Token for downstream access
   
3. API Gateway → Order Service
   Forwards request with user headers
   
4. Order Service accepts request from gateway (direct access blocked)
   Processes request and returns response
   
5. API Gateway → Client
   Returns response
```

### Service-to-Service Communication via Gateway

```
Order Service → API Gateway → Inventory Service
- Order Service calls Inventory through gateway
- Gateway adds X-Gateway-Token to downstream request
- Inventory blocks direct access without gateway token
```

### Concurrent Orders and Inventory (No Overselling)

When multiple users (e.g. Shayan and Hassan) submit orders for the **same product** and only **one unit** is in stock, the system ensures only one order succeeds and the other gets a clear failure:

1. **Inventory Service – atomic decrease**  
   Stock is decreased with a single conditional UPDATE in the database:  
   `UPDATE ... SET available_stock = available_stock - :qty WHERE item_code = :code AND available_stock >= :qty`.  
   Only one concurrent request can succeed; the other gets 0 rows updated and returns **409 Conflict** (Insufficient stock).

2. **Order Service – reserve first, then create order**  
   For each line item, Order Service calls Inventory to **decrease (reserve)** stock first. Only after **all** items are successfully reserved does it create the order and payment. If any reserve fails (409), it **compensates** by adding back stock for items already decreased, then returns 409 to the user. No order is created when stock is insufficient.

3. **Result**  
   One user gets **Order submitted**; the other gets **409** with a message like "Insufficient stock for item: ITEM-001. Available: 0, Requested: 2". No distributed locks; the database conditional update is the single source of truth.

## JWT Token Structure

```json
{
  "sub": "username",
  "service": "user-service",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Claims:**
- `sub`: Username (subject)
- `service`: Service name that generated the token
- `role`: Coarse-grained role for gateway authorization
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp (24 hours)

## Configuration

### JWT Secret
All services share the same JWT secret for token validation:
```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-must-be-at-least-32-characters-long-for-production-use}
  expiration: ${JWT_EXPIRATION:86400000} # 24 hours
gateway:
  internal-token: ${GATEWAY_INTERNAL_TOKEN:gombey-gateway-internal-token}
```

**Important:** In production, set `JWT_SECRET` as an environment variable with a strong, randomly generated secret key.

## Running the Services

### Using Docker Compose (Recommended)

```bash
docker-compose up --build
```

This will start all services:
- Service Registry: http://localhost:8761
- API Gateway: http://localhost:8080
- User Service: http://localhost:8083
- Order Service: http://localhost:8081
- Inventory Service: http://localhost:8082
- Payment Service: http://localhost:8084

### Using API Gateway

All requests should go through the API Gateway:

**Signup:**
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "password": "password123"
  }'
```

**Create Order (with token):**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "items": [{"itemCode": "ITEM-001", "quantity": 2}]
  }'
```

## Security Features

✅ **Centralized Authentication** - All authentication handled by User Service
✅ **API Gateway Security** - JWT validation at gateway level
✅ **Gateway-only Access** - Direct service access blocked without gateway token
✅ **Password Encryption** - BCrypt hashing
✅ **Token Expiration** - 24-hour token lifetime
✅ **Service-to-Service Auth** - Through gateway with internal token
✅ **CORS Support** - Configured for cross-origin requests

## Observability Stack

**Tracing:** Zipkin  
**Metrics:** Prometheus + Grafana  
**Logs:** OpenSearch + OpenSearch Dashboards (Filebeat shipping)

Each service exposes `/actuator/health` and `/actuator/prometheus`. Traces are exported to Zipkin and logs are written to `/logs/app.log` in Docker.

## CI/CD Pipeline

Jenkins provides a CI pipeline via the repository `Jenkinsfile`:
- Runs `mvn -B -ntp clean verify` to build and test all modules.
- Archives `**/target/*.jar` and publishes test reports from `**/target/surefire-reports/*.xml`.

## Benefits of This Architecture

1. **Separation of Concerns** - User management isolated in dedicated service
2. **Centralized Routing** - Single entry point via API Gateway
3. **Security** - JWT validation at gateway and service levels
4. **Scalability** - Each service can be scaled independently
5. **Maintainability** - Clear service boundaries
6. **Flexibility** - Easy to add new services or modify existing ones

## Future Enhancements

- Load Balancing
- Rate Limiting
- API Versioning
- Circuit Breaker (Resilience4j)
- Distributed Tracing (Zipkin/Jaeger)
- Centralized Logging (ELK Stack)
- Configuration Server (Spring Cloud Config)
