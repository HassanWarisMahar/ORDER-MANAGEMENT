# JWT Security Implementation

## Overview
JWT (JSON Web Token) authentication is centralized in the API Gateway, which validates tokens and enforces coarse-grained authorization. Downstream services are protected by a gateway-only access filter.

## Implementation Details

### 1. Dependencies Added
Services include:
- `spring-boot-starter-security` - Spring Security framework
- `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.12.3) - JWT token handling
- `spring-cloud-starter-gateway` - API Gateway (gateway service only)

### 2. Security Components

#### JWT Utility (`JwtUtil.java`)
- Token generation for users and services
- Token validation and expiration checking
- Claims extraction (username, service name, role, expiration)
- Located in: `{service}/src/main/java/com/microservices/{service}/security/`

#### API Gateway JWT Filter (`JwtAuthenticationFilter.java`)
- Intercepts gateway requests for protected routes
- Validates JWT token and role claims
- Adds user/service headers to downstream services
- Enforces coarse-grained authorization by route group

#### Gateway Access Filter (`GatewayAccessFilter.java`)
- Blocks direct access to services unless `X-Gateway-Token` is present
- Ensures API Gateway is the single entry point

#### Security Configuration (`SecurityConfig.java`)
- Services allow requests only via the gateway token filter
- Gateway handles JWT validation and authorization
- Stateless session management (no server-side sessions)

### 3. Authentication Endpoints

#### POST `/api/auth/signup`
Registers a new user account.

**Request:**
```json
{
  "username": "user123",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "createdAt": "2024-01-15T10:30:00",
  "message": "User registered successfully"
}
```

**Validation Rules:**
- Username: 3-50 characters, must be unique
- Email: Valid email format, must be unique
- Password: Minimum 6 characters

**Error Responses:**
- `400 Bad Request`: Username or email already exists, or validation errors

#### POST `/api/auth/login`
Authenticates user credentials and generates JWT token.

**Request:**
```json
{
  "username": "user123",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**Error Responses:**
- `400 Bad Request`: Invalid username or password
- `404 Not Found`: User not found

#### POST `/api/auth/service-token?serviceName=order-service`
Generates JWT token for service-to-service communication.

### 4. Service-to-Service Authentication via Gateway

Order Service calls Inventory Service through the API Gateway. The gateway injects `X-Gateway-Token` into downstream requests to enforce gateway-only access.

### 5. Configuration

JWT settings in `application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-must-be-at-least-32-characters-long-for-production-use}
  expiration: ${JWT_EXPIRATION:86400000} # 24 hours in milliseconds
gateway:
  internal-token: ${GATEWAY_INTERNAL_TOKEN:gombey-gateway-internal-token}
```

**Important:** In production, set `JWT_SECRET` as an environment variable with a strong, randomly generated secret key (at least 32 characters).

### 6. Using the API

#### Step 1: Register a New User (Sign Up)
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user123",
    "email": "user@example.com",
    "password": "password123"
  }'
```

#### Step 2: Get Authentication Token (Login)
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user123", "password": "password123"}'
```

#### Step 3: Use Token in API Calls
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token-here>" \
  -d '{
    "customerName": "John Doe",
    "customerEmail": "john@example.com",
    "items": [{"itemCode": "ITEM-001", "quantity": 2}]
  }'
```

### 7. Security Features

✅ **Token-based Authentication** - Stateless JWT tokens
✅ **Gateway-only Access** - Direct service access blocked without gateway token
✅ **Token Expiration** - Tokens expire after 24 hours (configurable)
✅ **Role-based Access** - Supports user and service roles
✅ **CORS Support** - Configured for cross-origin requests
✅ **Secure Secret Management** - Uses environment variables for secrets
✅ **Coarse-grained Authorization** - Enforced at the gateway by route group
✅ **User Registration** - Signup endpoint with validation
✅ **Password Encryption** - BCrypt password hashing
✅ **User Database** - Persistent user storage with JPA

### 8. Security Best Practices Implemented

1. **Stateless Authentication** - No server-side session storage
2. **Token Expiration** - Tokens automatically expire
3. **HMAC-SHA256 Signing** - Secure token signing algorithm
4. **Environment-based Secrets** - JWT secret from environment variables
5. **Gateway-only Access** - Internal gateway token blocks direct access
6. **Filter-based Validation** - Token validation happens at filter level
7. **Password Hashing** - BCrypt password encoding (one-way hashing)
8. **Input Validation** - Comprehensive validation on signup/login requests
9. **Unique Constraints** - Username and email uniqueness enforced at database level
10. **Error Handling** - Proper error messages without exposing sensitive information

### 9. Production Recommendations

1. **Change Default Secret** - Use a strong, randomly generated secret key
2. **Use HTTPS** - Always use HTTPS in production
3. **Token Refresh** - Implement token refresh mechanism for long-lived sessions
4. **Rate Limiting** - Add rate limiting to authentication endpoints (prevent brute force)
5. **Token Blacklisting** - Consider implementing token revocation for logout
6. **Audit Logging** - Log authentication attempts and failures
7. **Email Verification** - Add email verification for new signups
8. **Password Strength** - Enforce stronger password requirements
9. **Account Lockout** - Implement account lockout after failed login attempts
10. **Two-Factor Authentication** - Consider adding 2FA for enhanced security

### 10. Testing

To test the security:
1. Try accessing `/api/orders` without a token - should return 401 Unauthorized
2. Get a token from `/api/auth/login`
3. Use the token in the `Authorization: Bearer <token>` header
4. Access should now be granted

## Key Files

- `api-gateway/src/main/java/.../filter/JwtAuthenticationFilter.java` - Gateway JWT validation + coarse authorization
- `api-gateway/src/main/java/.../filter/GatewayTokenFilter.java` - Injects gateway token header
- `user-service/src/main/java/.../controller/AuthController.java` - Signup/login and token issuance
- `*/src/main/java/.../security/GatewayAccessFilter.java` - Blocks direct access without gateway token
