# Payment Service

Microservice for creating and retrieving payments linked to orders.

## Port

**8084**

## Responsibilities

- Create payment records when orders are placed (called by Order Service via API Gateway)
- Retrieve payment by order ID

## API Endpoints

- **POST** `/api/payments` – Create a payment (orderId, amount, currency, paymentMethod)
- **GET** `/api/payments/order/{orderId}` – Get payment for an order

## Technology

- Spring Boot 3.4.10, Spring Data JPA, SQLite
- Spring Security, JWT (gateway token), Eureka client
- OpenAPI/Swagger at `/swagger-ui.html`

## Run locally

```bash
mvn spring-boot:run
```

Uses gateway token and JWT config; call via API Gateway (e.g. `http://localhost:8080/api/payments`) with a valid JWT.
