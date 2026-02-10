# Payment Service

Microservice for creating and retrieving payments linked to orders. Called by **Order Service** via the API Gateway after an order is created.

## Port

**8084**

## Responsibilities

- Create payment records when orders are placed (Order Service calls after reserving stock and saving the order)
- Retrieve payment by order ID

## API Endpoints

- **POST** `/api/payments` – Create a payment  
  Body: `{ "orderId", "amount", "currency", "paymentMethod" }`
- **GET** `/api/payments/order/{orderId}` – Get payment for an order

## Technology

- Spring Boot 3.4.10, Spring Data JPA, SQLite
- Spring Security, JWT (gateway token), Eureka client
- OpenAPI/Swagger at `/swagger-ui.html`

## Configuration

- `SERVER_PORT`: 8084
- `GATEWAY_INTERNAL_TOKEN`: Must match API Gateway
- `JWT_SECRET`: Shared with other services
- `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`: Eureka (e.g. `http://localhost:8761/eureka/`)

## Run locally

```bash
mvn spring-boot:run
```

Use the API via the Gateway (e.g. `http://localhost:8080/api/payments`) with a valid JWT. Order Service calls this internally with a service token.
