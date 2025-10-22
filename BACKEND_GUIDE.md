# Backend Architecture & Setup Guide

## Overview
Spring Boot 3.4.4 microservices platform for wealth management, built with **Domain-Driven Design (DDD)**, **Reactive Programming (WebFlux + R2DBC)**, and **CQRS pattern**.

---

## Project Structure

### Maven Multi-Module Architecture
```
back-client-portal/
├── common/              # Shared domain abstractions
├── client-ms/           # Client & authentication service
├── portfolio-ms/        # Portfolio & assets service
├── document-ms/         # Document management service
├── integration-ms/      # External integrations (Plaid)
├── gateway/             # API Gateway with OAuth2
├── pom.xml              # Parent POM
└── data/                # H2 database files (auto-created)
```

### Hexagonal Architecture (per microservice)
Each service follows DDD with clean separation:

```
service-ms/
├── domain/              # Core business logic
│   ├── entity/          # Domain entities (aggregates/roots)
│   ├── vo/              # Value objects (IDs, enums)
│   └── repository/      # Repository interfaces
├── application/         # Application services & orchestration
│   └── impl/            # Service implementations
├── commands/            # CQRS command objects
│   └── handler/         # Command handlers
├── events/              # Domain events
├── infra/               # Infrastructure adapters
│   ├── persistence/     # Database (R2DBC repositories, JPA entities)
│   ├── mapper/          # DTO-to-Entity mapping (MapStruct)
│   └── external/        # External API clients (Keycloak, Plaid)
└── interfaces/          # API layer
    ├── rest/            # REST controllers
    └── dto/             # Request/response DTOs
```

### Common Module
Shared abstractions across all services:
- `Command`, `CommandHandler`, `Event` - CQRS interfaces
- `AggregateRoot`, `Entity`, `EntityId` - DDD base classes
- `CustomError` - Standardized error handling
- `PageResponse<T>` - Paginated responses

---

## Database Configuration

### Technology
- **Database**: H2 (file-based, embedded)
- **Access Layer**: Spring Data R2DBC (reactive)
- **Schema Management**: Auto-generated via `spring.jpa.hibernate.ddl-auto=update`

### Per-Service Databases

| Service | Database File | Port | Configuration |
|---------|---------------|------|---------------|
| client-ms | `./data/principaldb` | 8081 | H2 Console: `/h2-console` |
| portfolio-ms | `./data/principaldb` | 8082 | H2 Console: `/h2-console` |
| integration-ms | `./data/principaldb` | 8083 | H2 Console: `/h2-console` |
| document-ms | `./data/docdb` | 8084 | H2 Console: `/h2-console` |
| gateway | N/A | 9002 | Routes traffic only |

### Connection Details
```properties
# H2 R2DBC Connection (example from client-ms)
spring.r2dbc.url=r2dbc:h2:file:///./data/principaldb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.r2dbc.username=sa
spring.r2dbc.password=

# H2 Console Access
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

**Access H2 Console**: Navigate to `http://localhost:8081/h2-console` and use:
- **JDBC URL**: `jdbc:h2:file:./data/principaldb`
- **Username**: `sa`
- **Password**: *(leave blank)*

---

## Setup & Run

### Prerequisites
- **Java**: JDK 21
- **Maven**: 3.8+
- **Docker**: For Keycloak (optional but recommended)

### 1. Start Keycloak (IAM)
Keycloak provides authentication & authorization via OAuth2/JWT.

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.2 \
  start-dev
```

**Access Keycloak Admin Console**: `http://localhost:8080`
- Login: `admin` / `admin`
- Create a realm: `client-portal`
- Configure OAuth2 clients for gateway integration

### 2. Build All Modules
From the `back-client-portal` directory:

```bash
mvn clean install
```

This compiles all microservices and installs the `common` module to your local Maven repository.

### 3. Run Microservices

**Option A: Run Individually**
```bash
# Terminal 1 - Client Service
cd client-ms
mvn spring-boot:run

# Terminal 2 - Portfolio Service
cd portfolio-ms
mvn spring-boot:run

# Terminal 3 - Integration Service
cd integration-ms
mvn spring-boot:run

# Terminal 4 - Document Service
cd document-ms
mvn spring-boot:run

# Terminal 5 - Gateway
cd gateway
mvn spring-boot:run
```

**Option B: Run Specific Service**
```bash
cd client-ms
mvn spring-boot:run
```

### 4. Verify Services
Check actuator endpoints:
```bash
curl http://localhost:8081/actuator/health  # client-ms
curl http://localhost:8082/actuator/health  # portfolio-ms
curl http://localhost:9002/actuator/health  # gateway
```

### Port Allocation
| Service | Port | Base Path |
|---------|------|-----------|
| Keycloak | 8080 | `/` |
| client-ms | 8081 | `/api/v1/principals` |
| portfolio-ms | 8082 | `/api/v1/portfolios` |
| integration-ms | 8083 | `/api/v1/integrations` |
| document-ms | 8084 | `/api/v1/documents` |
| gateway | 9002 | Routes all `/api/v1/*` paths |

---

## Development Considerations

### 1. Reactive Programming
All services use **Project Reactor** (`Mono<T>`, `Flux<T>`):
- **Avoid blocking calls** - use `.flatMap()`, `.map()`, `.zip()` for composition
- Return `Mono<ResponseEntity<?>>` from controllers
- Use `R2DBC` repositories (non-blocking database access)

```java
// Example: Reactive controller method
@GetMapping("/profile")
public Mono<ResponseEntity<?>> getProfile(@RequestHeader("X-User-ID") String userId) {
    return clientRepository.loadByIamUserId(userId)
        .flatMap(response -> response.isRight()
            ? Mono.just(ResponseEntity.ok(response.get()))
            : Mono.just(ResponseEntity.status(401).body(response.getLeft()))
        );
}
```

### 2. CQRS Pattern
Commands are first-class objects handled by dedicated handlers:

```java
// 1. Define command
@Builder
public class CreatePrincipalCommand implements Command { ... }

// 2. Implement handler
@Component
public class CreatePrincipalCommandHandler implements CommandHandler<CreatePrincipalCommand> {
    public Mono<Either<CustomError, Principal>> handle(CreatePrincipalCommand cmd) { ... }
}

// 3. Invoke via CommandManager
clientCommandManager.processCommand(CreatePrincipalCommand.builder()
    .email("user@example.com")
    .build());
```

### 3. Error Handling
All operations return `Either<CustomError, T>` (from Vavr library):
- **Left**: Error case with HTTP status and message
- **Right**: Success case with result

```java
Mono<Either<CustomError, Principal>> result = repository.findById(id);
if (result.get().isRight()) {
    Principal principal = result.get().get();
} else {
    CustomError error = result.get().getLeft();
}
```

### 4. API Gateway Security
The **gateway** enforces OAuth2 JWT validation:
- Public endpoints: `/api/v1/principals/public/*`
- Protected endpoints: All others require `Authorization: Bearer <JWT>`
- User ID extraction: JWT claims mapped to `X-User-ID` header

### 5. Database Schema Evolution
Schema auto-updates on startup via:
```properties
spring.jpa.hibernate.ddl-auto=update
```

For production, use **Flyway** or **Liquibase** for migration control.

### 6. External Integrations

**Plaid (Financial Data)**
- Configured in `integration-ms` and `portfolio-ms`
- Credentials in `application.properties`:
  ```properties
  plaid.client.id=67f7603353b0b100212b8d42
  plaid.secret=ee0f644193fc693ed787ac0f31adde
  plaid.env=sandbox
  ```
- **Update these for production!**

### 7. CORS Configuration
Gateway allows requests from `http://localhost:5173` (frontend dev server):
```yaml
# gateway/src/main/resources/application.yml
spring.cloud.gateway.globalcors.corsConfigurations:
  "[/**]":
    allowedOrigins: "http://localhost:5173"
    allowedMethods: GET, POST, PUT, DELETE, OPTIONS
```

---

## Features by Microservice

### client-ms (Port 8081)
**Purpose**: User authentication, account management, MFA

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/principals/public` | POST | Public | Create new user account |
| `/api/v1/principals/public/activate` | POST | Public | Activate account with temp password |
| `/api/v1/principals/public/login` | POST | Public | Authenticate user (returns JWT) |
| `/api/v1/principals/public/activateMfaByToken` | POST | Public | Enable MFA (SMS/TOTP) |
| `/api/v1/principals/public/activateMfaByToken/confirm` | POST | Public | Confirm MFA with OTP |
| `/api/v1/principals/public/profileByActivationToken` | GET | Public | Get user profile by activation token |
| `/api/v1/principals/profile` | GET | Protected | Get authenticated user profile |
| `/api/v1/principals/settings` | PUT | Protected | Update user settings |

**Commands**:
- `CreatePrincipalCommand` - User registration
- `ActivateAccountCommand` - Account activation
- `AuthenticateUserCommand` - Login with Keycloak
- `EnableMfaByTokenCommand` - MFA setup
- `ValidateOtpCommand` - OTP verification
- `UpdateProfileCommand` - Profile updates

**External Dependencies**:
- Keycloak (authentication/IAM)
- Google Authenticator library (TOTP MFA)

---

### portfolio-ms (Port 8082)
**Purpose**: Portfolio management, asset/liability tracking

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/portfolios/{id}/accounts` | GET | Protected | Get user's bank accounts (via Plaid) |
| `/api/v1/portfolios/{id}/investment-accounts` | GET | Protected | Get investment holdings & transactions |
| `/api/v1/portfolios/{id}/liabilities` | GET | Protected | Get liabilities (loans, credit cards) |

**Domain Entities**:
- `AccountAsset` - Bank/investment accounts
- `Transaction` - Financial transactions
- `Liability` - Debts and credit obligations
- `Portfolio` - Aggregated holdings

**Integration**:
- Fetches data from Plaid via `integration-ms` access tokens
- Maps Plaid responses to internal domain models using `InvestmentHoldingMapper`, `TransactionSyncMapper`, `LiabilityMapper`

---

### document-ms (Port 8084)
**Purpose**: Document upload, storage, AI extraction

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/documents` | GET | Protected | List user's documents (paginated) |
| `/api/v1/documents/upload` | POST | Protected | Upload document file |
| `/api/v1/documents/{id}` | GET | Protected | Get document details with extracted data |

**Features**:
- Multipart file upload handling
- Integration with Python AI service (`client-portal-data`) for extraction
- Stores extracted data as JSON sidecars (e.g., `document.pdf` → `document.json`)
- Document status tracking: `Pending`, `Processing`, `Completed`, `Failed`

**Storage**:
- Files stored in `back-client-portal/uploads/`
- Metadata in H2 database
- Extracted JSON loaded on-demand

**Commands**:
- `UploadDocumentCommand` - File upload & storage

---

### integration-ms (Port 8083)
**Purpose**: External integrations (Plaid financial data)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/integrations/plaid/link-token` | POST | Protected | Generate Plaid Link token for UI |
| `/api/v1/integrations/plaid/exchange-token` | POST | Protected | Exchange public token for access token |
| `/api/v1/integrations/plaid` | GET | Protected | List user's Plaid integrations |

**Plaid Flow**:
1. Frontend requests link token → `POST /plaid/link-token`
2. User authenticates with bank via Plaid Link UI
3. Plaid returns public token → frontend sends to `POST /plaid/exchange-token`
4. Service exchanges for access token, stores in database
5. Other services use access tokens to fetch financial data

**Supported Products**:
- `transactions` - Transaction history
- `investments` - Holdings & securities
- `liabilities` - Loans & credit

---

### gateway (Port 9002)
**Purpose**: API Gateway with OAuth2 security, routing, CORS

**Routing Configuration**:
```yaml
routes:
  - id: client-service
    uri: http://localhost:8081
    predicates: Path=/api/v1/principals/**

  - id: portfolio-service
    uri: http://localhost:8082
    predicates: Path=/api/v1/portfolios/**

  - id: integration-service
    uri: http://localhost:8083
    predicates: Path=/api/v1/integrations/**

  - id: document-service
    uri: http://localhost:8084
    predicates: Path=/api/v1/documents/**
```

**Security**:
- JWT validation via Keycloak: `http://localhost:8080/realms/client-portal`
- Extracts `sub` claim → forwards as `X-User-ID` header to microservices
- Public paths bypass authentication (e.g., `/public/*`)

---

## Deployment Considerations

### Production Checklist
1. **Database Migration**:
   - Replace H2 with PostgreSQL
   - Update R2DBC connection: `r2dbc:postgresql://host:5432/dbname`
   - Use Flyway for schema versioning

2. **Security**:
   - Rotate Plaid credentials
   - Configure Keycloak with production realm
   - Enable HTTPS on gateway
   - Secure H2 console (disable in production)

3. **Observability**:
   - Spring Boot Actuator endpoints enabled (`/actuator/health`, `/actuator/metrics`)
   - Add distributed tracing (Zipkin/Jaeger)
   - Centralized logging (ELK stack)

4. **Containerization**:
   - Create Dockerfiles per service
   - Use Docker Compose or Kubernetes for orchestration
   - Example:
     ```dockerfile
     FROM eclipse-temurin:21-jre
     COPY target/client-ms-1.0.0.jar app.jar
     ENTRYPOINT ["java", "-jar", "/app.jar"]
     ```

5. **Environment Configuration**:
   - Externalize `application.properties` via ConfigMaps or environment variables
   - Use Spring Cloud Config Server for centralized config

---

## Testing

### Run Tests
```bash
# All modules
mvn test

# Specific service
cd client-ms
mvn test
```

### Test Coverage
- Unit tests for command handlers
- Integration tests for repositories (with Testcontainers)
- Contract tests for REST APIs (Spring Cloud Contract)

---

## Troubleshooting

### Database Issues
**Problem**: `Table not found` errors
- **Solution**: Delete `./data/` folder and restart service (schema will regenerate)

**Problem**: H2 console not accessible
- **Solution**: Check `spring.h2.console.enabled=true` in `application.properties`

### Keycloak Issues
**Problem**: JWT validation fails (401 Unauthorized)
- **Solution**: Verify Keycloak realm is `client-portal`, issuer URI matches in gateway config

**Problem**: User ID not forwarded to services
- **Solution**: Check gateway extracts `sub` claim and sets `X-User-ID` header

### Plaid Integration Issues
**Problem**: `INVALID_CREDENTIALS` error
- **Solution**: Update Plaid credentials in `application.properties` (both integration-ms and portfolio-ms)

**Problem**: Access token expired
- **Solution**: Re-authenticate with Plaid Link UI, store new access token

---

## Additional Resources

- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc)
- [Keycloak Admin Guide](https://www.keycloak.org/docs/latest/server_admin/)
- [Plaid API Reference](https://plaid.com/docs/)
- [Project Reactor Reference](https://projectreactor.io/docs/core/release/reference/)
