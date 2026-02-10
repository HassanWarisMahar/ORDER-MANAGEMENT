# Microservices Project: Order Management and Inventory Service (ORDER-MANAGEMENT)

A production-ready microservices project demonstrating a multi-service system with a dedicated User Management Service and an API Gateway for centralized routing and authentication.
Repository directory: `ORDER-MANAGEMENT`.

## 🏗️ Architecture

```
                    ┌─────────────────────────────────────────┐
                    │     Service Registry / Eureka (8761)    │
                    └────────────────────┬────────────────────┘
                                         │
┌────────────────────────────────────────┼────────────────────────────────────────┐
│                      API Gateway (8080)│                                        │
│              - Route Management        │  - JWT Authentication                  │
└──────────────┬──────────────┬──────────┴────────────┬───────────────────────────┘
               │              │                       │             │
    ┌──────────▼──────┐  ┌────▼─────┐  ┌──────────────▼──────┐  ┌───▼──────────┐
    │  User Service   │  │  Order   │  │  Payment Service    │  │  Inventory   │
    │    (8083)       │  │ Service  │  │     (8084)          │  │  Service     │
    │ - Auth          │  │  (8081)  │  │ - Create payment    │  │   (8082)     │
    │ - User Mgmt     │  │ - Orders │  │ - Get by order      │  │ - Inventory  │
    │ - JWT Gen       │  │ - Reserve│  │                     │  │ - Products   │
    └─────────────────┘  └────┬─────┘  └─────────────────────┘  └──────────────┘
                              │                    │                     ▲
                              └────────────────────┴─────────────────────┘
                                    (Order → Gateway → Inventory / Payment)
```

## 📋 Services

### 0. Service Registry
- **Port**: 8761
- **Responsibilities**: Eureka server for service discovery; enables load-balanced routing via the API Gateway.

### 1. API Gateway
- **Port**: 8080
- **Responsibilities**:
  - Centralized routing to all services
  - JWT validation at the edge
  - Gateway-only access enforcement for downstream services
  - Circuit breaker protection for downstream calls
  - CORS handling
- **Key Features**:
  - Route-based authentication
  - Adds user/service headers to downstream services
  - Adds internal gateway token header to downstream services
  - Circuit breaker fallback responses

### 2. User Service
- **Port**: 8083
- **Responsibilities**:
  - User registration and authentication
  - JWT token generation
- **Key Features**:
  - BCrypt password hashing
  - Signup and login endpoints

### 3. Order Service
- **Port**: 8081
- **Responsibilities**:
  - Create and retrieve orders
  - Validate item availability by calling Inventory Service via API Gateway
  - Confirm orders only when sufficient stock exists
- **Key Features**:
  - RESTful APIs for order management
  - WebClient-based inter-service communication
  - Graceful error handling

### 4. Inventory Service
- **Port**: 8082
- **Responsibilities**:
  - Manage inventory stock levels
  - Provide item availability information
  - **Atomic** decrease when orders reserve stock (prevents overselling under concurrent requests)
- **Key Features**:
  - RESTful APIs for inventory management (get stock, decrease, add)
  - Conditional UPDATE in DB for concurrent-order safety
  - Returns 409 Conflict when stock is insufficient

### 5. Payment Service
- **Port**: 8084
- **Responsibilities**:
  - Create payment records when orders are placed (called by Order Service via Gateway)
  - Retrieve payment by order ID
- **Key Features**:
  - RESTful APIs for payments
  - JWT and gateway-token protection

## 🛠️ Technology Stack

- **Java 25** (LTS JDK)
- **Spring Boot 3.4.10**
- **Spring Data JPA**
- **SQLite**
- **Spring WebFlux** (WebClient)
- **Spring Cloud Gateway (Spring Cloud 2024.0.2)**
- **OpenAPI/Swagger** documentation
- **JUnit 5** and **Mockito** for testing
- **Docker** and **Docker Compose**

## 🚀 Quick Start

### Prerequisites

- Java 25 JDK
- Maven 3.9+
- Docker and Docker Compose (for containerized deployment)

### Option 1: Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd "ORDER-MANAGEMENT"
   ```

2. **Start all services**
   ```bash
   docker-compose up --build
   ```

3. **Access the services**
   - API Gateway: http://localhost:8080
   - Service Registry: http://localhost:8761
   - User Service: http://localhost:8083
   - Order Service: http://localhost:8081
   - Inventory Service: http://localhost:8082
   - Payment Service: http://localhost:8084
   - Gateway entrypoint for APIs: http://localhost:8080
   - Direct service access is blocked without the gateway token header

### Option 2: Local Development

Start **Service Registry** first, then the other services (see [RUN_LOCALLY.md](RUN_LOCALLY.md) for full steps including Payment Service).

#### Start User Service

```bash
cd user-service
mvn clean install
mvn spring-boot:run
```

#### Start Inventory Service

```bash
cd inventory-service
mvn clean install
mvn spring-boot:run
```

#### Start Order Service

```bash
cd order-service
mvn clean install
mvn spring-boot:run
```

#### Start Payment Service

```bash
cd payment-service
mvn clean install
mvn spring-boot:run
```

#### Start API Gateway

```bash
cd api-gateway
mvn clean install
mvn spring-boot:run
```

## 📡 API Endpoints

### User Service

- **POST** `/api/auth/signup` - Register a new user
- **POST** `/api/auth/login` - Authenticate and get JWT token

### Order Service

- **POST** `/api/orders` - Create a new order
- **GET** `/api/orders/{id}` - Get order by ID

### Inventory Service

- **GET** `/api/inventory/{itemCode}` - Get available stock
- **POST** `/api/inventory/decrease` - Decrease (reserve) item stock (atomic; returns 409 if insufficient)
- **POST** `/api/inventory/add` - Add item stock

### Payment Service

- **POST** `/api/payments` - Create a payment for an order
- **GET** `/api/payments/order/{orderId}` - Get payment by order ID

## 📖 API Documentation

All services provide OpenAPI/Swagger documentation (via Gateway or direct with `X-Gateway-Token`):

- **User Service**: http://localhost:8083/swagger-ui.html
- **Order Service**: http://localhost:8081/swagger-ui.html
- **Inventory Service**: http://localhost:8082/swagger-ui.html
- **Payment Service**: http://localhost:8084/swagger-ui.html

Note: direct access to service ports requires the `X-Gateway-Token` header.

## 🧪 Testing

### Run All Tests

```bash
# Order Service
cd order-service
mvn test

# Inventory Service
cd inventory-service
mvn test

# User Service
cd user-service
mvn test

# Payment Service
cd payment-service
mvn test
```

### Test Coverage

- ✅ Unit tests for service layer
- ✅ Integration tests for inter-service communication
- ✅ Edge case testing
- ✅ Error handling tests

## ✅ CI/CD (Jenkins)

This repo includes a `Jenkinsfile` that runs a full Maven build and tests for all services.

### Jenkins Setup (Pipeline from SCM)

1. Install Jenkins and configure a build agent with:
   - JDK 25
   - Maven 3.9+
2. Create a new Pipeline job:
   - Definition: "Pipeline script from SCM"
   - SCM: Git
   - Script Path: `Jenkinsfile`
3. Run the job. It executes:
   - `mvn -B -ntp clean verify`

Artifacts (`**/target/*.jar`) and test reports (`**/target/surefire-reports/*.xml`) are archived automatically.

## 🐳 Docker

### Build Individual Services

```bash
# API Gateway
cd api-gateway
docker build -t api-gateway:latest .

# User Service
cd user-service
docker build -t user-service:latest .

# Order Service
cd order-service
docker build -t order-service:latest .

# Inventory Service
cd inventory-service
docker build -t inventory-service:latest .

# Payment Service
cd payment-service
docker build -t payment-service:latest .
```

### Docker Compose

The `docker-compose.yml` file orchestrates all services:

```bash
# Start all services
docker-compose up

# Start in detached mode
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f
```

## 📁 Project Structure

```
.
├── api-gateway/           # Gateway, JWT, routes, circuit breaker
├── service-registry/      # Eureka server (8761)
├── user-service/         # Auth, signup, login, JWT (8083)
├── order-service/        # Orders, reserve stock, payment (8081)
├── inventory-service/    # Stock, atomic decrease, products (8082)
├── payment-service/      # Payments per order (8084)
├── observability/        # Prometheus, Filebeat configs
├── scripts/              # Auth config, observability helpers
├── docker-compose.yml
├── README.md
├── ARCHITECTURE.md
├── RUN_LOCALLY.md
└── SECURITY_IMPLEMENTATION.md
```

## 🔧 Configuration

### Environment Variables

**Service Registry:**
- `SERVER_PORT`: Server port (default: `8761`)

**API Gateway:**
- `SERVER_PORT`: Server port (default: `8080`)
- `GATEWAY_INTERNAL_TOKEN`: Shared gateway token for downstream access (default: `gombey-gateway-internal-token`)
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka URL (default: `http://localhost:8761/eureka/`)

**User Service:**
- `SERVER_PORT`: Server port (default: `8083`)
- `GATEWAY_INTERNAL_TOKEN`: Must match gateway token
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka URL (default: `http://localhost:8761/eureka/`)

**Order Service:**
- `INVENTORY_SERVICE_URL`: Inventory via Gateway (default: `http://localhost:8080`)
- `PAYMENT_SERVICE_URL`: Payment via Gateway (default: `http://localhost:8084` or `http://localhost:8080` when using gateway)
- `SERVER_PORT`: Server port (default: `8081`)
- `GATEWAY_INTERNAL_TOKEN`: Must match gateway token
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka URL (default: `http://localhost:8761/eureka/`)

**Inventory Service:**
- `SERVER_PORT`: Server port (default: `8082`)
- `GATEWAY_INTERNAL_TOKEN`: Must match gateway token
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka URL (default: `http://localhost:8761/eureka/`)

**Payment Service:**
- `SERVER_PORT`: Server port (default: `8084`)
- `GATEWAY_INTERNAL_TOKEN`: Must match gateway token
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka URL (default: `http://localhost:8761/eureka/`)

## 🎯 Features

### User Service
- ✅ Signup and login endpoints
- ✅ JWT token generation
- ✅ BCrypt password hashing

### Order Service
- ✅ Reserve stock first (atomic), then create order and payment
- ✅ Prevents overselling when multiple users order the same last unit (409 for second user)
- ✅ Compensation: if a later item fails reserve, earlier reserves are rolled back via add-stock
- ✅ Inter-service communication with WebClient (Inventory + Payment via Gateway)
- ✅ 409 Conflict for insufficient stock; OpenAPI documentation

### Inventory Service
- ✅ Atomic stock decrease (conditional UPDATE in DB; no race when concurrent orders)
- ✅ Stock availability and add-stock APIs; 409 Conflict when insufficient stock
- ✅ Validation and error handling; OpenAPI documentation

### Payment Service
- ✅ Create payment per order; get payment by order ID
- ✅ Gateway and JWT protection; OpenAPI documentation

## 📝 Example Usage

### 1. Register a User

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 2. Login and Get Token

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "password": "password123"
  }'
```

### 3. Check Stock Availability (via Gateway)

```bash
curl http://localhost:8080/api/inventory/ITEM-001 \
  -H "Authorization: Bearer <token>"
```

Note: direct access to service ports requires `X-Gateway-Token` and is blocked by default.

### 4. Create an Order (via Gateway)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "items": [
      {
        "itemCode": "ITEM-001",
        "quantity": 2
      }
    ]
  }'
```

### 5. Retrieve Order (via Gateway)

```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <token>"
```

## 🏗️ Architecture Principles

- **SOLID Principles**: Applied throughout the codebase
- **Clean Architecture**: Layered architecture with clear separation of concerns
- **Dependency Injection**: Constructor-based injection
- **Exception Handling**: Global exception handling with `@ControllerAdvice`
- **Logging**: Comprehensive logging at service layer
- **Testing**: Unit and integration tests

## 📚 Additional Documentation

- [ARCHITECTURE.md](ARCHITECTURE.md) – Services, auth flows, concurrent orders & inventory
- [RUN_LOCALLY.md](RUN_LOCALLY.md) – Run all services locally (including Payment Service)
- [SECURITY_IMPLEMENTATION.md](SECURITY_IMPLEMENTATION.md) – JWT and gateway security
- [Order Service README](order-service/README.md)
- [Inventory Service README](inventory-service/README.md)
- [Payment Service README](payment-service/README.md)

## 🔍 Monitoring

### Health Checks

All services expose health and metrics via Spring Actuator:
- API Gateway: http://localhost:8080/actuator/health
- Service Registry: http://localhost:8761/actuator/health
- User Service: http://localhost:8083/actuator/health
- Order Service: http://localhost:8081/actuator/health
- Inventory Service: http://localhost:8082/actuator/health
- Payment Service: http://localhost:8084/actuator/health

### Observability Stack (Docker Compose)

- Zipkin (tracing): http://localhost:9411
- Prometheus (metrics): http://localhost:9090
- Grafana (dashboards): http://localhost:3000
- OpenSearch (logs search API): http://localhost:9200
- OpenSearch Dashboards: http://localhost:5601

Prometheus scrapes `/actuator/prometheus` on each service. Logs are written to `/logs/app.log` in each container and shipped via Filebeat to OpenSearch.

### Local Observability Helpers (No Compose)

Use these PowerShell scripts when services run on localhost:

- `scripts/start-observability.ps1` (starts Zipkin, Prometheus, Grafana, OpenSearch, Filebeat)
- `scripts/set-observability-env.ps1 -ServiceName <name>` (sets `ZIPKIN_ENDPOINT` and log file)

Example:
```powershell
.\scripts\start-observability.ps1
.\scripts\set-observability-env.ps1 -ServiceName user-service
mvn -f .\user-service\pom.xml spring-boot:run
```

### Local Observability (Manual)

If you prefer running tools manually:

**Zipkin (local jar):**
```powershell
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/io/zipkin/zipkin-server/2.24.0/zipkin-server-2.24.0-exec.jar" `
  -OutFile "zipkin-server-2.24.0-exec.jar"
java -jar .\zipkin-server-2.24.0-exec.jar
```

**Prometheus + Grafana (Docker, no compose):**
```powershell
docker run -d --name prometheus -p 9090:9090 `
  -v "$pwd/observability/prometheus.local.yml:/etc/prometheus/prometheus.yml:ro" `
  prom/prometheus:latest

docker run -d --name grafana -p 3000:3000 grafana/grafana:latest
```

Then start each service with:
```powershell
.\scripts\set-observability-env.ps1 -ServiceName user-service
mvn -f .\user-service\pom.xml spring-boot:run
```

## 🤝 Contributing

This is a demonstration project showcasing microservices architecture best practices.

## 📄 License

This project is for educational and demonstration purposes.
