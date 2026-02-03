# Order Service

## Overview

Order Service is a microservice responsible for creating and managing orders. It validates item availability by communicating with the Inventory Service and confirms orders only when sufficient stock exists.

## Technology Stack

- **Java 25** (LTS)
- **Spring Boot 3.3.5**
- **Spring Data JPA**
- **SQLite**
- **Spring WebFlux** (WebClient for inter-service communication)
- **OpenAPI/Swagger** for API documentation
- **JUnit 5** and **Mockito** for testing

## Architecture

The service follows a clean layered architecture:

```
┌─────────────────┐
│   Controller    │  REST API endpoints
├─────────────────┤
│    Service      │  Business logic & inter-service communication
├─────────────────┤
│   Repository    │  Data access layer
├─────────────────┤
│     Entity      │  Domain models
└─────────────────┘
```

## API Endpoints

### POST /api/orders
Creates a new order after validating and decreasing stock in the Inventory Service.

**Request Body:**
```json
{
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "items": [
    {
      "itemCode": "ITEM-001",
      "quantity": 2
    }
  ]
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "totalAmount": 20.00,
  "status": "CONFIRMED",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "items": [
    {
      "id": 1,
      "itemCode": "ITEM-001",
      "itemName": "Test Item",
      "quantity": 2,
      "price": 10.00,
      "subtotal": 20.00
    }
  ]
}
```

### GET /api/orders/{id}
Retrieves an order by its ID.

**Response:** `200 OK`
```json
{
  "id": 1,
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "totalAmount": 20.00,
  "status": "CONFIRMED",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "items": [...]
}
```

## Inter-Service Communication

The Order Service communicates with the Inventory Service using **WebClient** (reactive HTTP client) to:

1. **Validate Stock Availability**: Before creating an order, it checks if sufficient stock is available
2. **Decrease Stock**: After validation, it decreases the stock quantity in the Inventory Service

**Configuration:**
- Default Inventory Service URL: `http://inventory-service:8082`
- Configurable via `inventory.service.url` property or `INVENTORY_SERVICE_URL` environment variable

## Error Handling

The service includes global exception handling via `@ControllerAdvice`:

- **400 Bad Request**: Validation errors
- **404 Not Found**: Order not found
- **500 Internal Server Error**: Unexpected errors

## Local Development

### Prerequisites
- Java 25 JDK
- Maven 3.9+

### Running Locally

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd order-service
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8081`
   - Swagger UI: `http://localhost:8081/swagger-ui.html`
   - API Docs: `http://localhost:8081/api-docs`

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

## Docker Deployment

### Build Docker Image

```bash
docker build -t order-service:latest .
```

### Run Container

```bash
docker run -p 8081:8081 \
  -e INVENTORY_SERVICE_URL=http://inventory-service:8082 \
  order-service:latest
```

## Docker Compose

The service is configured to run with Docker Compose. See the root `docker-compose.yml` for orchestration.

```bash
# From project root
docker-compose up order-service
```

## Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:sqlite:./data/order-service.db

inventory:
  service:
    url: http://inventory-service:8082
```

### Environment Variables

- `INVENTORY_SERVICE_URL`: Inventory Service URL (default: `http://inventory-service:8082`)
- `SERVER_PORT`: Server port (default: `8081`)

## Testing

### Unit Tests
- Service layer tests with Mockito
- WebClient mocking for inter-service communication

### Integration Tests
- Order ↔ Inventory Service communication tests
- Database integration tests

## Logging

Logging is configured at the service layer with appropriate log levels:
- `DEBUG`: Detailed service operations
- `INFO`: General application flow
- `WARN`: Warning messages
- `ERROR`: Error messages

## Swagger Documentation

API documentation is available at:
- **Swagger UI**: `http://localhost:8081/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8081/api-docs`

## Project Structure

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/microservices/orderservice/
│   │   │       ├── config/          # Configuration classes
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── exception/       # Exception handlers
│   │   │       ├── model/           # Entity models
│   │   │       ├── repository/      # JPA repositories
│   │   │       └── service/         # Business logic
│   │   └── resources/
│   │       └── application.yml      # Configuration
│   └── test/
│       └── java/                    # Test classes
├── Dockerfile
└── pom.xml
```

## Best Practices

- ✅ Constructor-based dependency injection
- ✅ SOLID principles
- ✅ Clean layered architecture
- ✅ Global exception handling
- ✅ Comprehensive logging
- ✅ Unit and integration tests
- ✅ OpenAPI documentation

## License

This project is part of a microservices demonstration.
