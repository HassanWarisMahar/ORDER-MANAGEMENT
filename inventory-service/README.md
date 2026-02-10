# Inventory Service

## Overview

Inventory Service manages stock levels and provides **atomic** decrease (reserve) operations. When multiple users order the same last unit, only one decrease succeeds; the other receives **409 Conflict** (Insufficient stock). Decrease uses a conditional UPDATE in the database (`WHERE available_stock >= :quantity`) so no distributed locks are needed.

## Technology Stack

- **Java 25** (LTS)
- **Spring Boot 3.4.10**
- **Spring Data JPA**
- **SQLite**
- **REST APIs**
- **OpenAPI/Swagger** for API documentation
- **JUnit 5** and **Mockito** for testing

## Architecture

The service follows a clean layered architecture:

```
┌─────────────────┐
│   Controller    │  REST API endpoints
├─────────────────┤
│    Service      │  Business logic
├─────────────────┤
│   Repository    │  Data access layer
├─────────────────┤
│     Entity      │  Domain models
└─────────────────┘
```

## API Endpoints

### GET /api/inventory/{itemCode}
Retrieves the available stock for a specific item code.

**Path Parameters:**
- `itemCode` (String): The unique item code identifier

**Response:** `200 OK`
```json
{
  "itemCode": "ITEM-001",
  "itemName": "Test Item",
  "availableStock": 10,
  "inStock": true
}
```

**Error Responses:**
- `404 Not Found`: Item not found with the provided code

### POST /api/inventory/decrease
**Atomically** decreases (reserves) stock. Uses a conditional UPDATE so only one concurrent request can succeed when stock is limited.

**Request Body:**
```json
{
  "itemCode": "ITEM-001",
  "quantity": 2
}
```

**Response:** `200 OK`
```json
{
  "itemCode": "ITEM-001",
  "itemName": "Test Item",
  "quantityDecreased": 2,
  "remainingStock": 8,
  "success": true
}
```

**Error Responses:**
- `409 Conflict`: Insufficient stock (e.g. concurrent order for last unit)
- `404 Not Found`: Item not found with the provided code

### POST /api/inventory/add
Increases the stock quantity for a specific item (e.g. restock or compensation after a failed multi-item order).

**Request Body:**
```json
{
  "itemCode": "ITEM-001",
  "quantity": 10
}
```

## Data Model

### InventoryItem Entity

```java
- id: Long (Primary Key)
- itemCode: String (Unique)
- itemName: String
- availableStock: Integer
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

## Error Handling

Global exception handling via `@ControllerAdvice`:

- **400 Bad Request**: Validation errors
- **404 Not Found**: Item not found
- **409 Conflict**: Insufficient stock (used for concurrent-order safety)
- **500 Internal Server Error**: Unexpected errors

## Local Development

### Prerequisites
- Java 25 JDK
- Maven 3.9+

### Running Locally

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd inventory-service
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
   - API Base URL: `http://localhost:8082`
   - Swagger UI: `http://localhost:8082/swagger-ui.html`
   - API Docs: `http://localhost:8082/api-docs`

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
docker build -t inventory-service:latest .
```

### Run Container

```bash
docker run -p 8082:8082 inventory-service:latest
```

## Docker Compose

The service is configured to run with Docker Compose. See the root `docker-compose.yml` for orchestration.

```bash
# From project root
docker-compose up inventory-service
```

## Configuration

### Application Properties

Key configuration in `application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:sqlite:./data/inventory-service.db
```

### Environment Variables

- `SERVER_PORT`: Server port (default: `8082`)

## Testing

### Unit Tests
- Service layer tests with Mockito
- Repository tests
- Edge cases (insufficient stock, item not found)

### Test Coverage
- Stock retrieval operations
- Stock decrease operations
- Validation and error handling

## Logging

Logging is configured at the service layer with appropriate log levels:
- `DEBUG`: Detailed service operations
- `INFO`: General application flow
- `WARN`: Warning messages (e.g., insufficient stock)
- `ERROR`: Error messages

## Swagger Documentation

API documentation is available at:
- **Swagger UI**: `http://localhost:8082/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8082/api-docs`

## Sample Data

To populate sample inventory items, you can use the following API calls:

```bash
# Create inventory items via direct database access or initialization script
# Example: Insert via SQL script
```

## Project Structure

```
inventory-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/microservices/inventoryservice/
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
- ✅ Unit tests with edge cases
- ✅ OpenAPI documentation
- ✅ Transaction management for stock operations

## Business Rules

1. **Atomic Decrease**: Decrease uses a single conditional UPDATE (`WHERE available_stock >= :quantity`); only one concurrent request succeeds when stock is limited.
2. **Stock Validation**: Stock cannot go below zero (enforced by the conditional UPDATE).
3. **Item Uniqueness**: Each item code must be unique.
4. **409 on Insufficient Stock**: When the conditional UPDATE affects 0 rows, the API returns 409 Conflict so callers (e.g. Order Service) can avoid creating orders and compensate if needed.

## License

This project is part of a microservices demonstration.
