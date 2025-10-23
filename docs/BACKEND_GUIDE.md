# Backend Architecture & Developer Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Module Breakdown](#module-breakdown)
5. [Dependencies & Libraries](#dependencies--libraries)
6. [Database Configuration](#database-configuration)
7. [Development Patterns](#development-patterns)
8. [Integration with AI Service](#integration-with-ai-service)
9. [API Gateway Security](#api-gateway-security)
10. [Configuration](#configuration)

---

## Overview

### Purpose
**back-client-portal** is a Spring Boot 3.4 microservices platform that provides the backend infrastructure for the Invictus wealth management SaaS application. It handles:

- **User Management** - Account creation, authentication, MFA, profile management
- **Portfolio Management** - Real-time financial data, assets, liabilities
- **Document Processing** - Upload, storage, AI-powered data extraction
- **External Integrations** - Plaid (financial accounts), Keycloak (authentication)
- **API Gateway** - OAuth2 security, routing, CORS

### Role in the Platform
This backend serves as the **business logic layer** between the React frontend and external services. It provides:

```
┌──────────────┐      ┌──────────────┐      ┌──────────────────┐
│   Frontend   │      │   Backend    │      │  External        │
│  (React)     │─────▶│ Microservices│─────▶│  Services        │
│              │      │  (Spring)    │      │  (Keycloak,      │
│              │◀─────│              │◀─────│   Plaid, AI)     │
└──────────────┘      └──────────────┘      └──────────────────┘
   HTTP/REST          Gateway (OAuth2)       HTTP/gRPC APIs
```

### Technology Stack Summary
- **Language**: Java 21
- **Framework**: Spring Boot 3.4.4
- **Reactive**: Spring WebFlux + Project Reactor
- **Database**: Spring Data R2DBC + H2 (development)
- **Security**: Spring Security OAuth2 + Keycloak
- **Build**: Maven (multi-module)
- **Architecture**: Domain-Driven Design (DDD) + CQRS

---

## Architecture

### Design Patterns

#### 1. **Domain-Driven Design (DDD)**
**What**: Organize code around business domains (Client, Portfolio, Document, Integration)
**Why**:
- **Business clarity**: Code structure mirrors real-world concepts
- **Bounded contexts**: Each microservice owns its domain
- **Ubiquitous language**: Shared terminology between developers and stakeholders

**Example Structure**:
```
client-ms/
├── domain/              # Core business rules
│   ├── entity/          # Principal, FamilyRelationship (aggregates)
│   ├── vo/              # Value objects (PrincipalId, Email)
│   └── repository/      # Repository interfaces (ports)
├── application/         # Use cases (CreatePrincipal, AuthenticateUser)
├── infra/               # Infrastructure adapters
│   ├── persistence/     # R2DBC implementations
│   └── external/        # Keycloak API client
└── interfaces/          # API layer (REST controllers, DTOs)
```

#### 2. **Hexagonal Architecture (Ports & Adapters)**
**What**: Separate core business logic from infrastructure concerns
**Why**:
- **Testability**: Core logic can be tested without databases/APIs
- **Flexibility**: Swap infrastructure without changing business rules
- **Maintainability**: Clear separation of concerns

```
┌─────────────────────────────────────────────────┐
│              Application Core                   │
│  ┌───────────────────────────────────────┐     │
│  │        Domain Layer                   │     │
│  │  • Entities                           │     │
│  │  • Value Objects                      │     │
│  │  • Business Rules                     │     │
│  └───────────────────────────────────────┘     │
│                                                 │
│  ┌───────────────────────────────────────┐     │
│  │     Application Services (Use Cases)  │     │
│  │  • CreatePrincipalService             │     │
│  │  • AuthenticateUserService            │     │
│  └───────────────────────────────────────┘     │
│                    │                            │
└────────────────────┼────────────────────────────┘
                     │
       ┌─────────────┼─────────────┐
       │             │             │
       ▼             ▼             ▼
 ┌─────────┐   ┌─────────┐   ┌─────────┐
 │  REST   │   │Database │   │External │
 │   API   │   │  (R2DBC)│   │  APIs   │
 │(Adapter)│   │(Adapter)│   │(Adapter)│
 └─────────┘   └─────────┘   └─────────┘
```

#### 3. **CQRS (Command Query Responsibility Segregation)**
**What**: Separate write operations (commands) from read operations (queries)
**Why**:
- **Clarity**: Explicit intent for each operation
- **Scalability**: Optimize reads and writes independently
- **Validation**: Centralized command validation logic

**How it works**:
```java
// 1. Define Command (immutable request)
@Builder
public class CreatePrincipalCommand implements Command {
    private final String alias;
    private final String email;
    private final String mainPlatformId;
}

// 2. Implement CommandHandler (business logic)
@Component
public class CreatePrincipalCommandHandler implements CommandHandler<CreatePrincipalCommand> {

    @Override
    public Mono<Either<CustomError, Principal>> handle(CreatePrincipalCommand cmd) {
        // Validate, create entity, persist
        return principalRepository.save(Principal.builder()
            .alias(cmd.getAlias())
            .email(cmd.getEmail())
            .build());
    }
}

// 3. Invoke via CommandManager (orchestration)
commandManager.processCommand(CreatePrincipalCommand.builder()
    .alias("John Doe")
    .email("john@example.com")
    .mainPlatformId("user123")
    .build());
```

**Benefits**:
- **Single Responsibility**: Each handler does one thing
- **Testability**: Test handlers independently
- **Audit Trail**: Commands are explicit actions that can be logged

#### 4. **Reactive Programming**
**What**: Asynchronous, non-blocking operations using Mono/Flux (Project Reactor)
**Why**:
- **Scalability**: Handle more concurrent requests with fewer threads
- **Performance**: No thread blocking = better resource utilization
- **Backpressure**: Built-in flow control for data streams

**Key Concepts**:
```java
// Mono<T> - 0 or 1 element (like CompletableFuture)
Mono<Principal> findOne = repository.findById(id);

// Flux<T> - 0 to N elements (like Stream)
Flux<Principal> findAll = repository.findAll();

// Composition (chaining operations)
Mono<ResponseEntity<?>> result = repository.findById(id)
    .flatMap(principal -> updateService.update(principal))  // Async operation
    .map(updated -> ResponseEntity.ok(updated))             // Transform
    .defaultIfEmpty(ResponseEntity.notFound().build());     // Handle empty
```

**Reactive Rules**:
1. ❌ **Never block**: Don't use `.block()`, `Thread.sleep()`, or blocking I/O
2. ✅ **Use operators**: `.flatMap()`, `.map()`, `.zip()`, `.filter()`
3. ✅ **Return Mono/Flux**: Controllers and services return reactive types
4. ✅ **Subscribe once**: Framework subscribes, don't call `.subscribe()` manually

---

## Project Structure

### Maven Multi-Module Architecture

```
back-client-portal/
├── pom.xml                    # Parent POM (dependency management)
├── common/                    # Shared domain abstractions
│   └── src/main/java/com/asbitech/common/
│       ├── command/           # Command, CommandHandler, CommandManager
│       ├── entity/            # AggregateRoot, Entity, EntityId
│       ├── event/             # Event interface
│       ├── exception/         # CustomError (Either pattern)
│       └── response/          # PageResponse<T>
├── client-ms/                 # User management microservice
│   ├── pom.xml                # Depends on common
│   └── src/main/java/com/asbitech/client_ms/
│       ├── domain/            # Business entities
│       ├── application/       # Use cases
│       ├── commands/          # CQRS commands
│       ├── events/            # Domain events
│       ├── infra/             # Infrastructure
│       └── interfaces/        # REST API
├── portfolio-ms/              # Portfolio management (stateless)
├── document-ms/               # Document upload & AI extraction
├── integration-ms/            # External integrations (Plaid)
├── gateway/                   # API Gateway (OAuth2 security)
└── data/                      # H2 database files (auto-created)
```

**Why Multi-Module?**
- **Shared Code**: `common` module provides base classes and interfaces
- **Independent Deployment**: Each microservice can be deployed separately
- **Consistent Versioning**: Parent POM manages dependency versions
- **Type Safety**: Modules can depend on each other with compile-time safety

### Build Order
```bash
# 1. Build common module first (provides base classes)
cd common && mvn install

# 2. Build all microservices (depend on common)
cd .. && mvn clean install
```

Maven resolves dependencies automatically when you run from the parent directory.

---

## Module Breakdown

### 1. **common/** - Shared Domain Abstractions

**Purpose**: Provide base classes and interfaces used by all microservices.

#### Key Components

##### `Command` Interface
**Purpose**: Marker interface for CQRS command objects
```java
public interface Command {
    // Marker interface - all commands implement this
}
```

**Usage**:
```java
@Builder
public class CreatePrincipalCommand implements Command {
    private final String alias;
    private final String email;
}
```

##### `CommandHandler<T>` Interface
**Purpose**: Define contract for command processing
```java
public interface CommandHandler<T extends Command> {
    Mono<Either<CustomError, ?>> handle(T command);
}
```

**Why generic?** Type safety - each handler is specific to one command type.

##### `CommandManager` Class
**Purpose**: Route commands to their handlers
```java
@Component
public class CommandManager {
    private Map<Class<? extends Command>, CommandHandler> handlers;

    public <T extends Command> Mono<Either<CustomError, ?>> processCommand(T command) {
        CommandHandler<T> handler = handlers.get(command.getClass());
        return handler.handle(command);
    }
}
```

##### `EntityId` Abstract Class
**Purpose**: Type-safe entity identifiers
```java
public abstract class EntityId {
    private final UUID value;

    // Ensures IDs are immutable and type-safe
    protected EntityId(UUID value) {
        this.value = Objects.requireNonNull(value);
    }
}

// Usage
public class PrincipalId extends EntityId {
    public PrincipalId(UUID value) { super(value); }
}
```

**Why not just String/UUID?**
- Type safety: Can't accidentally use PrincipalId where PortfolioId is expected
- Domain clarity: IDs are first-class domain concepts

##### `CustomError` Class
**Purpose**: Standardized error representation with HTTP status
```java
@Builder
public class CustomError {
    private final int code;           // HTTP status code
    private final String message;     // Error message
    private final String details;     // Additional context
}
```

**Used with Vavr Either**:
```java
// Success: Right(value)
Either<CustomError, Principal> success = Either.right(principal);

// Error: Left(error)
Either<CustomError, Principal> error = Either.left(CustomError.builder()
    .code(404)
    .message("Principal not found")
    .build());
```

##### `PageResponse<T>` Class
**Purpose**: Standardized pagination for list endpoints
```java
@Builder
public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

**Why separate from Spring's Page?**
- Framework independence: Domain doesn't depend on Spring Data
- Consistent DTO structure across all services

---

### 2. **client-ms** (Port 8081) - User Management & Authentication

**Purpose**: Handle user accounts, authentication, MFA, and profile management.

#### Domain Model

##### `Principal` Entity
**Core aggregate** representing a user account:
```java
@Entity
@Builder
public class Principal extends AggregateRoot {
    private PrincipalId id;
    private String alias;              // Display name
    private String email;
    private String mainPlatformId;     // External system ID
    private String iamUserId;          // Keycloak user ID
    private String activationToken;    // One-time activation token
    private boolean activated;
    private LocalDateTime createdAt;
}
```

##### `FamilyRelationship` Entity
**Represents relationships between users** (advisor-client, spouse, etc.):
```java
@Entity
public class FamilyRelationship {
    private Long id;
    private String fromPrincipalId;    // Source user
    private String toPrincipalId;      // Target user
    private String relationshipType;   // ADVISOR, SPOUSE, DEPENDENT
}
```

#### Application Services

##### `PrincipalService`
**Orchestrates user management use cases**:
- `createPrincipal()` - Register new user in Keycloak and database
- `activateAccount()` - Activate account with temporary password
- `authenticate()` - Login with credentials, return JWT
- `enableMfa()` - Setup MFA (SMS or TOTP)
- `validateOtp()` - Verify OTP code
- `updateProfile()` - Update user settings

#### Commands & Handlers

**Example: User Registration Flow**

1. **CreatePrincipalCommand**:
```java
@Builder
public class CreatePrincipalCommand implements Command {
    private final String alias;
    private final String mainPlatformId;
    private final String mail;
}
```

2. **CreatePrincipalCommandHandler**:
```java
@Component
public class CreatePrincipalCommandHandler implements CommandHandler<CreatePrincipalCommand> {

    private final KeycloakService keycloakService;
    private final PrincipalRepository principalRepository;

    @Override
    public Mono<Either<CustomError, Principal>> handle(CreatePrincipalCommand cmd) {
        // 1. Generate activation token
        String token = UUID.randomUUID().toString();

        // 2. Create user in Keycloak (IAM system)
        return keycloakService.createUser(cmd.getMail(), cmd.getAlias())
            .flatMap(keycloakUserId -> {
                // 3. Save Principal entity in database
                Principal principal = Principal.builder()
                    .id(PrincipalId.create())
                    .alias(cmd.getAlias())
                    .email(cmd.getMail())
                    .mainPlatformId(cmd.getMainPlatformId())
                    .iamUserId(keycloakUserId)
                    .activationToken(token)
                    .activated(false)
                    .build();

                return principalRepository.save(principal);
            })
            .map(Either::<CustomError, Principal>right)
            .onErrorResume(ex -> Mono.just(Either.left(CustomError.builder()
                .code(500)
                .message("Failed to create principal")
                .details(ex.getMessage())
                .build())));
    }
}
```

3. **REST Controller**:
```java
@RestController
@RequestMapping("/api/v1/principals")
public class PrincipalController {

    private final CommandManager commandManager;

    @PostMapping("/public")
    public Mono<ResponseEntity<?>> createPrincipal(@RequestBody CreatePrincipalRequest request) {
        return commandManager.processCommand(CreatePrincipalCommand.builder()
                .alias(request.getAlias())
                .mail(request.getMail())
                .mainPlatformId(request.getMainPlatformId())
                .build())
            .map(either -> either.isRight()
                ? ResponseEntity.ok(either.get())
                : ResponseEntity.status(either.getLeft().getCode())
                    .body(either.getLeft()));
    }
}
```

#### External Dependencies

##### **Keycloak Integration**
**Purpose**: Authentication and IAM
**Implementation**: `KeycloakServiceImpl`

**Key Operations**:
- `createUser()` - Create user account in Keycloak
- `authenticate()` - Exchange credentials for JWT token
- `updatePassword()` - Set user password
- `getUserInfo()` - Retrieve user details

**Why Keycloak?**
- **OAuth2/OpenID Connect**: Industry-standard authentication
- **JWT tokens**: Stateless authentication across microservices
- **SSO**: Single sign-on across multiple applications
- **User federation**: Can integrate with LDAP/Active Directory
- **Admin API**: Programmatic user management

**Configuration**:
```java
// KeycloakServiceImpl.java
private final String keycloakBaseUrl = "http://localhost:8080";
private final String realm = "client-portal";
private final String clientId = "admin-client";
private final String clientSecret = "YOUR_CLIENT_SECRET";
```

##### **Google Authenticator (TOTP MFA)**
**Purpose**: Time-based One-Time Password for 2FA
**Library**: `warrenstrange:googleauth`

**Flow**:
1. User enables MFA → Generate secret key
2. User scans QR code with authenticator app
3. User enters 6-digit code to confirm
4. Future logins require OTP + password

#### API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/principals/public` | POST | Public | Create new user account |
| `/api/v1/principals/public/activate` | POST | Public | Activate account with temp password |
| `/api/v1/principals/public/login` | POST | Public | Authenticate user (returns JWT) |
| `/api/v1/principals/public/activateMfaByToken` | POST | Public | Enable MFA (SMS/TOTP) |
| `/api/v1/principals/public/activateMfaByToken/confirm` | POST | Public | Confirm MFA with OTP |
| `/api/v1/principals/profile` | GET | Protected | Get authenticated user profile |
| `/api/v1/principals/settings` | PUT | Protected | Update user settings |

**Public vs Protected**:
- **Public**: No JWT required (registration, login)
- **Protected**: Requires `Authorization: Bearer <JWT>` header (gateway validates)

---

### 3. **portfolio-ms** (Port 8082) - Portfolio Management

**Purpose**: Provide real-time financial data (accounts, investments, liabilities) from Plaid.

#### Architecture - Stateless Design

**Important**: portfolio-ms has **no database**. It fetches all data in real-time from Plaid via integration-ms.

**Why stateless?**
- **Fresh data**: Always returns latest account balances and transactions
- **No sync**: No need to sync Plaid data to local database
- **Simplicity**: No schema management, no data consistency issues
- **Scalability**: No database connections, easier to scale horizontally

#### Data Flow

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Frontend   │      │ portfolio-ms │      │integration-ms│
│              │─────▶│              │─────▶│              │
│              │  1   │  (no DB)     │  2   │   (has DB)   │
└──────────────┘      └──────────────┘      └──────────────┘
  Request portfolio        Get access             Return
  for user XYZ             tokens for            access tokens
                           user XYZ
                                │
                                │ 3. Use tokens to
                                │    fetch from Plaid
                                ▼
                          ┌──────────────┐
                          │   Plaid API  │
                          │   (External) │
                          └──────────────┘
                                │
                                │ 4. Return real-time data
                                ▼
                          ┌──────────────┐
                          │ portfolio-ms │
                          │ transforms & │
                          │ returns data │
                          └──────────────┘
```

#### Application Services

##### `PortfolioService`
**Orchestrates portfolio data retrieval**:
```java
@Service
public class PortfolioService {

    private final IntegrationServiceClient integrationClient;
    private final PlaidClient plaidClient;

    public Mono<List<Account>> getAccounts(String userId) {
        // 1. Get user's Plaid access tokens from integration-ms
        return integrationClient.getPlaidIntegrations(userId)
            .flatMapMany(Flux::fromIterable)
            .flatMap(integration -> {
                // 2. Fetch accounts from Plaid for each integration
                return plaidClient.getAccounts(integration.getAccessToken());
            })
            .collectList();  // 3. Combine results from all integrations
    }
}
```

**Why this approach?**
- **Single source of truth**: Plaid is the authoritative data source
- **No data staleness**: Users always see current balances
- **Reduced complexity**: No ETL pipelines, no sync jobs

#### Integration with Plaid

**Plaid Products Used**:
- `accounts` - Account balances and details
- `transactions` - Transaction history
- `investments` - Investment holdings and securities
- `liabilities` - Loans, credit cards, mortgages

**Example: Fetching Investment Holdings**
```java
public Mono<List<InvestmentHolding>> getInvestmentHoldings(String userId) {
    return integrationClient.getPlaidIntegrations(userId)
        .flatMapMany(Flux::fromIterable)
        .flatMap(integration -> {
            // Call Plaid Investments API
            InvestmentsHoldingsGetRequest request = new InvestmentsHoldingsGetRequest()
                .accessToken(integration.getAccessToken());

            return plaidClient.investmentsHoldingsGet(request);
        })
        .map(investmentHoldingMapper::toInvestmentHolding)
        .collectList();
}
```

#### Mappers

**Purpose**: Transform Plaid API responses to domain models

- `InvestmentHoldingMapper` - Plaid holdings → `InvestmentHolding`
- `TransactionSyncMapper` - Plaid transactions → `Transaction`
- `LiabilityMapper` - Plaid liabilities → `Liability`

**Example Mapper**:
```java
@Mapper(componentModel = "spring")
public interface InvestmentHoldingMapper {

    @Mapping(source = "security.name", target = "securityName")
    @Mapping(source = "security.ticker_symbol", target = "ticker")
    @Mapping(source = "quantity", target = "quantity")
    @Mapping(source = "institution_price", target = "currentPrice")
    InvestmentHolding toInvestmentHolding(Holding plaidHolding);
}
```

**Why MapStruct?**
- **Type-safe**: Compile-time validation of mappings
- **Performance**: Generates bytecode (no reflection)
- **Maintainability**: Declarative mapping configuration

#### API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/portfolios/{id}/accounts` | GET | Protected | Get user's bank accounts (via Plaid) |
| `/api/v1/portfolios/{id}/investment-accounts` | GET | Protected | Get investment holdings & transactions |
| `/api/v1/portfolios/{id}/liabilities` | GET | Protected | Get liabilities (loans, credit cards) |

---

### 4. **document-ms** (Port 8084) - Document Management & AI Extraction

**Purpose**: Handle document uploads, storage, and AI-powered data extraction.

#### Domain Model

##### `Document` Entity
```java
@Entity
@Builder
public class Document {
    private DocumentId id;
    private String userId;              // Owner
    private String fileName;
    private String filePath;            // Storage location
    private String fileType;            // PDF, JPEG, PNG
    private long fileSize;              // Bytes
    private String category;            // Bank statement, capital call, etc.
    private String status;              // PENDING, PROCESSING, COMPLETED, FAILED
    private LocalDateTime uploadedAt;
    private String extractedDataPath;   // Path to JSON results
}
```

#### Application Services

##### `DocumentUploadService`
**Handles file upload and storage**:
```java
@Service
public class DocumentUploadService {

    private final DocumentRepository repository;
    private final StorageService storageService;
    private final AIExtractionClient aiClient;

    public Mono<Document> uploadDocument(String userId, FilePart filePart) {
        // 1. Save file to disk
        return storageService.save(filePart)
            .flatMap(savedPath -> {
                // 2. Create Document entity
                Document doc = Document.builder()
                    .id(DocumentId.create())
                    .userId(userId)
                    .fileName(filePart.filename())
                    .filePath(savedPath)
                    .fileSize(/* from file */)
                    .status("PENDING")
                    .build();

                // 3. Save to database
                return repository.save(doc);
            })
            .flatMap(doc -> {
                // 4. Trigger AI extraction (async)
                return aiClient.processDocument(doc.getFilePath())
                    .thenReturn(doc);
            });
    }
}
```

#### Storage Strategy

**File System Storage** (not database):
```
back-client-portal/
└── uploads/
    ├── user-abc-123/
    │   ├── bank_statement_2025.pdf
    │   ├── bank_statement_2025.json       # AI extraction results
    │   ├── capital_call_notice.pdf
    │   └── capital_call_notice.json
    └── user-def-456/
        └── ...
```

**Why file system?**
- **Performance**: Databases aren't optimized for large binary files
- **Simplicity**: Direct file access, no BLOB handling
- **Integration**: AI service can read files directly
- **Scalability**: Easy to move to S3/cloud storage later

**Storage Service**:
```java
@Service
public class StorageService {

    @Value("${storage.base.path}")
    private String basePath;  // e.g., ./uploads

    public Mono<String> save(FilePart filePart) {
        String filename = filePart.filename();
        Path targetPath = Paths.get(basePath, filename);

        return filePart.transferTo(targetPath)
            .thenReturn(targetPath.toString());
    }
}
```

#### Integration with AI Service (client-portal-data)

**Purpose**: Extract structured data from uploaded documents using Vision-Language Models.

**API Contract**:
```http
POST http://localhost:8000/process
Content-Type: application/json

{
  "file_path": "uploads/user-123/bank_statement.pdf"
}

Response: 202 Accepted
{
  "message": "Extraction started",
  "file_path": "uploads/user-123/bank_statement.pdf"
}
```

**Implementation**:
```java
@Service
public class AIExtractionClient {

    private final WebClient webClient;

    @Value("${ai.service.url}")
    private String aiServiceUrl;  // http://localhost:8000

    public Mono<Void> processDocument(String filePath) {
        return webClient.post()
            .uri(aiServiceUrl + "/process")
            .bodyValue(Map.of("file_path", filePath))
            .retrieve()
            .bodyToMono(Void.class);
    }
}
```

**Polling for Results**:
```java
public Mono<DocumentExtractionResult> getExtractionResults(String documentPath) {
    String jsonPath = documentPath.replace(".pdf", ".json");

    // Poll every 2 seconds, max 60 attempts (2 minutes)
    return Mono.delay(Duration.ofSeconds(2))
        .repeat(60)
        .filter(i -> Files.exists(Paths.get(jsonPath)))
        .next()
        .flatMap(i -> {
            // Read and parse JSON results
            String json = Files.readString(Paths.get(jsonPath));
            return Mono.just(objectMapper.readValue(json, DocumentExtractionResult.class));
        });
}
```

**Extracted Data Structure**:
```json
{
  "num_pages": 2,
  "category": "bank_statement",
  "extracted_data": [
    {
      "image_url": "https://...",
      "extracted_data": {
        "account_holder": {
          "value": "John Doe",
          "bbox": [120.5, 80.3, 320.8, 95.7],
          "confidence": 0.98
        },
        "account_number": {
          "value": "1234567890",
          "bbox": [150.2, 120.5, 250.3, 135.8],
          "confidence": 0.95
        }
      }
    }
  ]
}
```

**Why this architecture?**
- **Separation of concerns**: Python AI service is independent
- **Language choice**: Python for ML, Java for business logic
- **Scalability**: AI service can run on GPU-equipped servers
- **Flexibility**: Easy to swap AI models or providers

#### Commands & Handlers

**UploadDocumentCommand**:
```java
@Builder
public class UploadDocumentCommand implements Command {
    private final String userId;
    private final FilePart filePart;
}

@Component
public class UploadDocumentCommandHandler implements CommandHandler<UploadDocumentCommand> {

    private final DocumentUploadService uploadService;

    @Override
    public Mono<Either<CustomError, Document>> handle(UploadDocumentCommand cmd) {
        return uploadService.uploadDocument(cmd.getUserId(), cmd.getFilePart())
            .map(Either::<CustomError, Document>right)
            .onErrorResume(ex -> Mono.just(Either.left(CustomError.builder()
                .code(500)
                .message("Upload failed")
                .details(ex.getMessage())
                .build())));
    }
}
```

#### API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/documents` | GET | Protected | List user's documents (paginated) |
| `/api/v1/documents/upload` | POST | Protected | Upload document file (multipart) |
| `/api/v1/documents/{id}` | GET | Protected | Get document details + extracted data |

---

### 5. **integration-ms** (Port 8083) - External Integrations

**Purpose**: Manage integrations with external services, primarily Plaid for financial data.

#### Domain Model

##### `PlaidIntegration` Entity
```java
@Entity
@Builder
public class PlaidIntegration {
    private PlaidIntegrationId id;
    private String userId;
    private String accessToken;       // Plaid access token (encrypted)
    private String institutionId;     // Bank identifier
    private String institutionName;   // "Chase", "Bank of America", etc.
    private List<String> products;    // ["transactions", "investments", "liabilities"]
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncedAt;
}
```

#### Plaid Integration Flow

**Step 1: Generate Link Token**
```java
@PostMapping("/plaid/link-token")
public Mono<ResponseEntity<?>> createLinkToken(@RequestHeader("X-User-ID") String userId) {
    return plaidClient.linkTokenCreate(LinkTokenCreateRequest.builder()
            .user(LinkTokenCreateRequestUser.builder()
                .clientUserId(userId)
                .build())
            .clientName("Invictus Wealth Management")
            .products(List.of("transactions", "investments", "liabilities"))
            .countryCodes(List.of(CountryCode.US))
            .language("en")
            .build())
        .map(response -> ResponseEntity.ok(Map.of(
            "link_token", response.getLinkToken()
        )));
}
```

**Step 2: Frontend Opens Plaid Link**
```javascript
// Frontend code (React)
const plaid = Plaid.create({
  token: linkToken,
  onSuccess: (public_token, metadata) => {
    // Send public_token to backend
    fetch('/api/v1/integrations/plaid/exchange-token', {
      method: 'POST',
      body: JSON.stringify({ public_token })
    });
  }
});

plaid.open();
```

**Step 3: Exchange Public Token**
```java
@PostMapping("/plaid/exchange-token")
public Mono<ResponseEntity<?>> exchangeToken(
    @RequestHeader("X-User-ID") String userId,
    @RequestBody ExchangeTokenRequest request) {

    // Exchange public token for access token
    return plaidClient.itemPublicTokenExchange(ItemPublicTokenExchangeRequest.builder()
            .publicToken(request.getPublicToken())
            .build())
        .flatMap(response -> {
            // Save PlaidIntegration entity
            PlaidIntegration integration = PlaidIntegration.builder()
                .id(PlaidIntegrationId.create())
                .userId(userId)
                .accessToken(response.getAccessToken())  // Store securely!
                .institutionId(request.getInstitutionId())
                .institutionName(request.getInstitutionName())
                .products(request.getProducts())
                .connectedAt(LocalDateTime.now())
                .build();

            return integrationRepository.save(integration);
        })
        .map(integration -> ResponseEntity.ok(Map.of(
            "message", "Integration successful",
            "integration_id", integration.getId().toString()
        )));
}
```

**Step 4: Use Access Token**
```java
// portfolio-ms fetches this access token
public Mono<List<PlaidIntegration>> getPlaidIntegrations(String userId) {
    return integrationRepository.findByUserId(userId);
}
```

#### Plaid Configuration

**application.properties**:
```properties
plaid.client.id=YOUR_PLAID_CLIENT_ID
plaid.secret=YOUR_PLAID_SECRET
plaid.env=sandbox  # or 'development', 'production'
plaid.api.url=https://sandbox.plaid.com
```

**Environment Options**:
- **sandbox**: Fake data, free, for development
- **development**: Real data, limited institutions, free
- **production**: Real data, all institutions, paid

#### API Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/v1/integrations/plaid/link-token` | POST | Protected | Generate Plaid Link token for UI |
| `/api/v1/integrations/plaid/exchange-token` | POST | Protected | Exchange public token for access token |
| `/api/v1/integrations/plaid` | GET | Protected | List user's Plaid integrations |

---

### 6. **gateway** (Port 9002) - API Gateway

**Purpose**: Single entry point for all API traffic with OAuth2 security, routing, and CORS.

#### Architecture

```
┌──────────────┐
│   Frontend   │
│ (localhost:  │
│    5173)     │
└──────┬───────┘
       │
       │ HTTP Request
       ▼
┌──────────────────────────────────────────────┐
│            API Gateway (9002)                │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │     JWT Validation (OAuth2)            │ │
│  │  • Verify token with Keycloak          │ │
│  │  • Extract user ID from JWT claims     │ │
│  │  • Add X-User-ID header                │ │
│  └─────────────┬────────────────────────────┘ │
│                │                              │
│  ┌─────────────▼────────────────────────────┐ │
│  │           Routing Logic                  │ │
│  │  /api/v1/principals/**   → client-ms    │ │
│  │  /api/v1/portfolios/**   → portfolio-ms │ │
│  │  /api/v1/integrations/** → integration-ms│ │
│  │  /api/v1/documents/**    → document-ms  │ │
│  └─────────────┬────────────────────────────┘ │
└────────────────┼──────────────────────────────┘
                 │
         ┌───────┴────────┐
         │                │
         ▼                ▼
    ┌─────────┐      ┌─────────┐
    │client-ms│      │document │
    │ (8081)  │  ... │  -ms    │
    └─────────┘      └─────────┘
```

#### Routing Configuration

**application.yml**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        # Client Service
        - id: client-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/v1/principals/**
          filters:
            - StripPrefix=0

        # Portfolio Service
        - id: portfolio-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/v1/portfolios/**
          filters:
            - StripPrefix=0

        # Integration Service
        - id: integration-service
          uri: http://localhost:8083
          predicates:
            - Path=/api/v1/integrations/**
          filters:
            - StripPrefix=0

        # Document Service
        - id: document-service
          uri: http://localhost:8084
          predicates:
            - Path=/api/v1/documents/**
          filters:
            - StripPrefix=0
```

**How routing works**:
1. Request arrives: `http://localhost:9002/api/v1/principals/profile`
2. Gateway matches predicate: `/api/v1/principals/**`
3. Forwards to: `http://localhost:8081/api/v1/principals/profile`

#### Security Configuration

**JWT Validation**:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/client-portal
          jwk-set-uri: http://localhost:8080/realms/client-portal/protocol/openid-connect/certs
```

**SecurityConfig.java**:
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                // Public endpoints (no JWT required)
                .pathMatchers("/api/v1/principals/public/**").permitAll()

                // Protected endpoints (JWT required)
                .pathMatchers("/api/v1/**").authenticated()

                // Actuator endpoints
                .pathMatchers("/actuator/**").permitAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        return jwt -> {
            // Extract user ID from JWT 'sub' claim
            String userId = jwt.getClaimAsString("sub");

            // Create authentication with authorities
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, userId));
        };
    }
}
```

**User ID Extraction**:
```java
@Component
public class UserIdExtractorFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMap(auth -> {
                String userId = auth.getToken().getClaimAsString("sub");

                // Add X-User-ID header for downstream services
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-ID", userId)
                    .build();

                return chain.filter(exchange.mutate().request(request).build());
            });
    }
}
```

**Why extract user ID?**
- Microservices don't need to parse JWT
- Consistent user identification across services
- Simplified authorization logic in downstream services

#### CORS Configuration

**application.yml**:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "http://localhost:5173"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders: "*"
            allowCredentials: true
```

**Why CORS?**
- Frontend (localhost:5173) and backend (localhost:9002) are different origins
- Browsers enforce Same-Origin Policy
- CORS headers tell browser to allow cross-origin requests

---

## Dependencies & Libraries

### Core Spring Framework

#### **Spring Boot 3.4.4**
**Purpose**: Application framework and auto-configuration
**Why**:
- **Convention over configuration**: Minimal setup required
- **Production-ready**: Built-in metrics, health checks, security
- **Ecosystem**: Massive library of Spring projects
- **Java 21 support**: Latest JVM features

#### **Spring WebFlux**
**Purpose**: Reactive web framework
**Why**:
- **Non-blocking I/O**: Better scalability than servlet-based (Spring MVC)
- **Backpressure**: Automatic flow control for data streams
- **Integration**: Works with R2DBC, reactive clients
- **Performance**: Handle more concurrent requests with fewer threads

**Trade-offs**:
- ✅ Scalability and performance
- ✅ Async composition with operators
- ❌ Steeper learning curve than blocking code
- ❌ Not suitable for CPU-intensive tasks

#### **Spring Data R2DBC**
**Purpose**: Reactive database access
**Why**:
- **Non-blocking**: Database calls don't block threads
- **Spring Data**: Familiar repository pattern
- **Type-safe**: Compile-time query validation
- **Performance**: Better throughput than JDBC

**Example**:
```java
public interface PrincipalRepository extends R2dbcRepository<Principal, UUID> {

    Mono<Principal> findByEmail(String email);

    @Query("SELECT * FROM principal WHERE iam_user_id = :iamUserId")
    Mono<Principal> findByIamUserId(String iamUserId);
}
```

**Why R2DBC over JPA?**
- R2DBC is reactive (non-blocking)
- JPA is blocking (forces thread-per-request model)
- Can't mix blocking and reactive code effectively

#### **Spring Cloud Gateway**
**Purpose**: API Gateway with routing and security
**Why**:
- **Reactive**: Built on Spring WebFlux
- **Filters**: Modify requests/responses (add headers, rate limiting)
- **OAuth2 support**: Built-in JWT validation
- **Load balancing**: Can route to multiple instances

**Alternatives**:
- Netflix Zuul (deprecated, blocking)
- Kong (separate infrastructure, not Spring-native)
- AWS API Gateway (vendor lock-in)

### Security

#### **Spring Security OAuth2 Resource Server**
**Purpose**: JWT validation and authentication
**Why**:
- **Standard**: OAuth2 is industry-standard
- **Stateless**: No session storage required
- **Microservices-friendly**: JWT can be validated independently
- **Integration**: Works with Keycloak, Okta, Auth0

#### **Keycloak**
**Purpose**: Identity and Access Management (IAM)
**Why**:
- **Open source**: No licensing fees
- **OAuth2/OpenID Connect**: Standard protocols
- **User management**: Built-in admin console
- **Multi-tenancy**: Support multiple realms
- **SSO**: Single sign-on across applications

**Alternatives**:
- Auth0 (SaaS, paid)
- Okta (SaaS, enterprise focus)
- AWS Cognito (vendor lock-in)

### Database

#### **H2 Database**
**Purpose**: Embedded SQL database (development)
**Why**:
- **Zero setup**: No installation required
- **Fast**: In-memory or file-based
- **SQL compatible**: Easy migration to PostgreSQL
- **Web console**: Built-in database viewer

**Configuration**:
```properties
spring.r2dbc.url=r2dbc:h2:file:///./data/principaldb
spring.r2dbc.username=sa
spring.r2dbc.password=
```

**For production**: Use PostgreSQL or MySQL with connection pooling.

### Code Generation & Mapping

#### **Lombok**
**Purpose**: Reduce boilerplate code
**Why**:
- **@Builder**: Fluent builder pattern
- **@Data**: Getters, setters, equals, hashCode, toString
- **@Slf4j**: Logger field
- **Compile-time**: No runtime overhead

**Example**:
```java
@Data
@Builder
@Entity
public class Principal {
    private UUID id;
    private String alias;
    private String email;
}

// Usage
Principal principal = Principal.builder()
    .id(UUID.randomUUID())
    .alias("John Doe")
    .email("john@example.com")
    .build();
```

#### **MapStruct**
**Purpose**: Type-safe bean mapping
**Why**:
- **Compile-time**: Generates mapping code (no reflection)
- **Type-safe**: Catches mapping errors at compile time
- **Performance**: As fast as hand-written code
- **Maintainability**: Declarative mapping configuration

**Example**:
```java
@Mapper(componentModel = "spring")
public interface PrincipalMapper {

    @Mapping(source = "iamUserId", target = "keycloakId")
    @Mapping(source = "alias", target = "displayName")
    PrincipalDTO toDTO(Principal principal);
}
```

**Why MapStruct over manual mapping?**
- Less boilerplate code
- Catches field name mismatches at compile time
- Generates optimized bytecode

### Functional Programming

#### **Vavr (formerly Javaslang)**
**Purpose**: Functional programming utilities
**Why**:
- **Either<L, R>**: Type-safe error handling (no exceptions in return types)
- **Try**: Exception handling as values
- **Immutable collections**: Thread-safe by default

**Either Pattern**:
```java
// Traditional approach (exceptions)
public Principal createPrincipal(String email) throws ValidationException, DatabaseException {
    if (!isValid(email)) throw new ValidationException();
    return repository.save(new Principal(email));
}

// Either approach (functional)
public Either<CustomError, Principal> createPrincipal(String email) {
    if (!isValid(email)) {
        return Either.left(CustomError.builder()
            .code(400)
            .message("Invalid email")
            .build());
    }

    Principal principal = repository.save(new Principal(email));
    return Either.right(principal);
}

// Usage
Either<CustomError, Principal> result = createPrincipal("john@example.com");
if (result.isRight()) {
    Principal principal = result.get();
} else {
    CustomError error = result.getLeft();
}
```

**Why Either over exceptions?**
- **Explicit**: Return type shows operation can fail
- **Type-safe**: Compiler enforces error handling
- **Functional**: Compose with `.map()`, `.flatMap()`
- **No surprises**: No hidden exceptions

### External Integrations

#### **Plaid Java SDK**
**Purpose**: Financial data integration
**Why**:
- **Official SDK**: Maintained by Plaid
- **Type-safe**: Strongly-typed request/response models
- **Comprehensive**: Supports all Plaid products

**Products Used**:
- `accounts` - Account balances
- `transactions` - Transaction history
- `investments` - Holdings and securities
- `liabilities` - Loans and credit

**Configuration**:
```java
PlaidClient plaidClient = PlaidClient.newBuilder()
    .clientIdAndSecret(clientId, secret)
    .sandboxBaseUrl()  // or .developmentBaseUrl(), .productionBaseUrl()
    .build();
```

### Testing

#### **JUnit 5**
**Purpose**: Unit testing framework
**Why**:
- **Annotations**: `@Test`, `@BeforeEach`, `@ParameterizedTest`
- **Assertions**: Readable test assertions
- **Extensions**: Mockito integration

#### **Reactor Test**
**Purpose**: Testing reactive code
**Why**:
- **StepVerifier**: Verify Mono/Flux behavior
- **Virtual time**: Test delays without waiting

**Example**:
```java
@Test
void testFindPrincipalById() {
    UUID id = UUID.randomUUID();
    Principal principal = Principal.builder().id(id).build();

    when(repository.findById(id)).thenReturn(Mono.just(principal));

    StepVerifier.create(service.findById(id))
        .expectNext(principal)
        .verifyComplete();
}
```

---

## Database Configuration

### H2 Database (Development)

#### Per-Service Databases

| Service | Database File | JDBC URL |
|---------|---------------|----------|
| client-ms | `./data/principaldb` | `jdbc:h2:file:./data/principaldb` |
| integration-ms | `./data/principaldb` | `jdbc:h2:file:./data/principaldb` |
| document-ms | `./data/docdb` | `jdbc:h2:file:./data/docdb` |
| portfolio-ms | None (stateless) | N/A |

**Why shared database for client-ms and integration-ms?**
- Both services need user identity information
- Simplifies foreign key relationships
- For production, split into separate PostgreSQL databases

#### R2DBC Configuration

**application.properties** (client-ms):
```properties
# R2DBC Connection (reactive)
spring.r2dbc.url=r2dbc:h2:file:///./data/principaldb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.r2dbc.username=sa
spring.r2dbc.password=

# H2 Console (for debugging)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Schema Management
spring.jpa.hibernate.ddl-auto=update
```

**Connection URL Parameters**:
- `DB_CLOSE_DELAY=-1`: Keep database open until JVM exits
- `DB_CLOSE_ON_EXIT=FALSE`: Don't close database when last connection closes

#### H2 Console Access

**URL**: http://localhost:8081/h2-console

**Login**:
- **JDBC URL**: `jdbc:h2:file:./data/principaldb`
- **Username**: `sa`
- **Password**: *(leave blank)*

**Use cases**:
- Inspect database schema
- Run SQL queries for debugging
- Verify data persistence

**Security Note**: Disable H2 console in production!

#### Schema Management

**Auto-generation** (development):
```properties
spring.jpa.hibernate.ddl-auto=update
```

**Options**:
- `create`: Drop and recreate schema on startup (data loss!)
- `create-drop`: Drop schema on shutdown
- `update`: Update schema to match entities (safe, recommended for dev)
- `validate`: Validate schema matches entities (no changes)
- `none`: No schema management (production)

**For production**: Use migration tools like Flyway or Liquibase:
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Flyway migration** (example):
```sql
-- V1__Create_principal_table.sql
CREATE TABLE principal (
    id UUID PRIMARY KEY,
    alias VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    iam_user_id VARCHAR(255),
    activated BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP
);
```

---

## Development Patterns

### Reactive Programming Best Practices

#### Rule 1: Never Block

**❌ Bad**:
```java
@GetMapping("/profile")
public Mono<Principal> getProfile(String userId) {
    Principal principal = repository.findById(userId).block();  // BLOCKING!
    return Mono.just(principal);
}
```

**✅ Good**:
```java
@GetMapping("/profile")
public Mono<Principal> getProfile(String userId) {
    return repository.findById(userId);  // Non-blocking
}
```

#### Rule 2: Compose with Operators

**❌ Bad**:
```java
Mono<Principal> principal = repository.findById(userId);
Principal p = principal.block();

if (p != null) {
    p.setLastLogin(LocalDateTime.now());
    repository.save(p).block();
}
```

**✅ Good**:
```java
repository.findById(userId)
    .flatMap(principal -> {
        principal.setLastLogin(LocalDateTime.now());
        return repository.save(principal);
    })
    .subscribe();  // Let framework subscribe
```

#### Rule 3: Handle Empty Cases

**❌ Bad**:
```java
return repository.findById(userId)
    .map(ResponseEntity::ok);  // Returns empty Mono if not found!
```

**✅ Good**:
```java
return repository.findById(userId)
    .map(ResponseEntity::ok)
    .defaultIfEmpty(ResponseEntity.notFound().build());
```

#### Rule 4: Combine Multiple Reactive Streams

**Parallel Execution** (both requests start simultaneously):
```java
Mono<User> user = userService.getUser(userId);
Mono<Account> account = accountService.getAccount(accountId);

// Combine results
Mono.zip(user, account)
    .map(tuple -> {
        User u = tuple.getT1();
        Account a = tuple.getT2();
        return new UserAccountDTO(u, a);
    });
```

**Sequential Execution** (second depends on first):
```java
userService.getUser(userId)
    .flatMap(user -> accountService.getAccountsForUser(user.getId()))
    .collectList();
```

### CQRS Implementation

#### Command Design

**Principles**:
1. **Immutable**: Use `@Builder` and `final` fields
2. **Validation**: Validate in command constructor
3. **Explicit intent**: Command name describes action

**Example**:
```java
@Builder
public class UpdatePrincipalProfileCommand implements Command {
    private final UUID principalId;
    private final String alias;
    private final String phoneNumber;

    // Validation in constructor
    public UpdatePrincipalProfileCommand(UUID principalId, String alias, String phoneNumber) {
        this.principalId = Objects.requireNonNull(principalId, "Principal ID required");
        this.alias = Objects.requireNonNull(alias, "Alias required");
        this.phoneNumber = phoneNumber;  // Optional

        if (alias.isBlank()) {
            throw new IllegalArgumentException("Alias cannot be blank");
        }
    }
}
```

#### Handler Design

**Principles**:
1. **Single responsibility**: One handler per command
2. **Error handling**: Return `Either<CustomError, T>`
3. **Reactive**: Return `Mono<Either<CustomError, T>>`
4. **Testable**: Inject dependencies via constructor

**Template**:
```java
@Component
public class UpdatePrincipalProfileCommandHandler implements CommandHandler<UpdatePrincipalProfileCommand> {

    private final PrincipalRepository repository;

    public UpdatePrincipalProfileCommandHandler(PrincipalRepository repository) {
        this.repository = repository;
    }

    @Override
    public Mono<Either<CustomError, Principal>> handle(UpdatePrincipalProfileCommand cmd) {
        return repository.findById(cmd.getPrincipalId())
            .switchIfEmpty(Mono.just(Either.left(CustomError.builder()
                .code(404)
                .message("Principal not found")
                .build())))
            .flatMap(principal -> {
                principal.setAlias(cmd.getAlias());
                principal.setPhoneNumber(cmd.getPhoneNumber());
                return repository.save(principal);
            })
            .map(Either::<CustomError, Principal>right)
            .onErrorResume(ex -> Mono.just(Either.left(CustomError.builder()
                .code(500)
                .message("Failed to update profile")
                .details(ex.getMessage())
                .build())));
    }
}
```

### Error Handling with Either

#### Creating Either Results

**Success**:
```java
Principal principal = new Principal();
Either<CustomError, Principal> success = Either.right(principal);
```

**Error**:
```java
Either<CustomError, Principal> error = Either.left(CustomError.builder()
    .code(400)
    .message("Invalid email format")
    .build());
```

#### Checking Either Results

```java
if (result.isRight()) {
    Principal principal = result.get();
    // Handle success
} else {
    CustomError error = result.getLeft();
    // Handle error
}
```

#### Converting to ResponseEntity

```java
return commandManager.processCommand(command)
    .map(either -> either.isRight()
        ? ResponseEntity.ok(either.get())
        : ResponseEntity.status(either.getLeft().getCode())
            .body(either.getLeft()));
```

---

## Integration with AI Service

### Overview

The **document-ms** microservice integrates with **client-portal-data** (Python AI service) for intelligent document processing.

### Architecture

```
┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Frontend   │      │  document-ms │      │client-portal-│
│   (React)    │─────▶│   (Java)     │─────▶│  data (AI)   │
└──────────────┘      └──────────────┘      └──────────────┘
  Upload file          Store file            Extract data
  (multipart)          Trigger AI            Save JSON

                            │
                            │ Polling
                            ▼
                      ┌──────────────┐
                      │  JSON File   │
                      │  (Results)   │
                      └──────────────┘
```

### API Contract

#### Request: Trigger Extraction

**Endpoint**: `POST http://localhost:8000/process`
**Content-Type**: `application/json`

```json
{
  "file_path": "uploads/user-123/bank_statement.pdf"
}
```

**Response**: `202 Accepted`
```json
{
  "message": "Extraction started",
  "file_path": "uploads/user-123/bank_statement.pdf"
}
```

**Note**: Processing happens in background. Results saved as `bank_statement.json`.

#### Response: Extraction Results

**File**: `uploads/user-123/bank_statement.json`

```json
{
  "num_pages": 2,
  "category": "bank_statement",
  "occurrences": 2,
  "classification_results": [
    {
      "image_url": "https://huggingface.co/datasets/.../page_1.jpg",
      "extracted_data": {
        "category": "bank_statement"
      }
    }
  ],
  "extracted_data": [
    {
      "image_url": "https://huggingface.co/datasets/.../page_1.jpg",
      "extracted_data": {
        "account_holder": {
          "value": "John Doe",
          "bbox": [120.5, 80.3, 320.8, 95.7],
          "confidence": 0.98
        },
        "account_number": {
          "value": "1234567890",
          "bbox": [150.2, 120.5, 250.3, 135.8],
          "confidence": 0.95
        },
        "transactions": [
          {
            "date": "2025-01-15",
            "description": "Payment received",
            "debit": null,
            "credit": 5000.00,
            "balance": 10000.00
          }
        ]
      }
    }
  ]
}
```

### Implementation

#### 1. Trigger AI Extraction

```java
@Service
public class AIExtractionClient {

    private final WebClient webClient;

    @Value("${ai.service.url}")
    private String aiServiceUrl;  // http://localhost:8000

    public Mono<Void> processDocument(String filePath) {
        return webClient.post()
            .uri(aiServiceUrl + "/process")
            .bodyValue(Map.of("file_path", filePath))
            .retrieve()
            .bodyToMono(Void.class)
            .timeout(Duration.ofSeconds(10))
            .onErrorResume(TimeoutException.class, ex -> {
                log.warn("AI service timeout, will retry");
                return Mono.empty();
            });
    }
}
```

#### 2. Poll for Results

```java
@Service
public class DocumentExtractionService {

    public Mono<DocumentExtractionResult> waitForResults(String documentPath) {
        String jsonPath = documentPath.replace(".pdf", ".json");

        return Mono.defer(() -> {
            if (Files.exists(Paths.get(jsonPath))) {
                String json = Files.readString(Paths.get(jsonPath));
                return Mono.just(objectMapper.readValue(json, DocumentExtractionResult.class));
            }
            return Mono.empty();
        })
        .repeatWhenEmpty(repeat -> repeat
            .delayElements(Duration.ofSeconds(2))
            .take(60))  // Max 2 minutes
        .timeout(Duration.ofMinutes(2));
    }
}
```

#### 3. Update Document Status

```java
@Service
public class DocumentProcessingOrchestrator {

    private final AIExtractionClient aiClient;
    private final DocumentRepository repository;
    private final DocumentExtractionService extractionService;

    public Mono<Document> processDocument(Document document) {
        // 1. Update status to PROCESSING
        document.setStatus("PROCESSING");

        return repository.save(document)
            .flatMap(doc -> {
                // 2. Trigger AI extraction
                return aiClient.processDocument(doc.getFilePath())
                    .thenReturn(doc);
            })
            .flatMap(doc -> {
                // 3. Wait for results
                return extractionService.waitForResults(doc.getFilePath())
                    .flatMap(results -> {
                        // 4. Update document with results
                        doc.setStatus("COMPLETED");
                        doc.setCategory(results.getCategory());
                        doc.setExtractedDataPath(doc.getFilePath().replace(".pdf", ".json"));
                        return repository.save(doc);
                    });
            })
            .onErrorResume(ex -> {
                // 5. Mark as failed on error
                document.setStatus("FAILED");
                return repository.save(document);
            });
    }
}
```

### Configuration

**application.yml** (document-ms):
```yaml
ai:
  service:
    url: http://localhost:8000
    timeout: 120000  # 2 minutes

storage:
  base:
    path: ./uploads

spring:
  codec:
    max-in-memory-size: 10MB  # File upload limit
```

### Error Handling

**Scenarios**:
1. **AI service unavailable**: Retry with exponential backoff
2. **Timeout**: Mark document as PROCESSING, continue polling
3. **Invalid file format**: Return error immediately
4. **Extraction failed**: Mark document as FAILED, store error message

---

## API Gateway Security

### JWT Validation Flow

```
┌──────────────┐
│   Client     │
│  (Browser)   │
└──────┬───────┘
       │ 1. Login
       ▼
┌──────────────┐
│  client-ms   │
│  /login      │
└──────┬───────┘
       │ 2. Authenticate
       ▼
┌──────────────┐
│  Keycloak    │
│  (OAuth2)    │
└──────┬───────┘
       │ 3. Return JWT
       ▼
┌──────────────┐
│   Client     │
│  (Store JWT) │
└──────┬───────┘
       │ 4. API Request
       │ Authorization: Bearer <JWT>
       ▼
┌─────────────────────────────────┐
│         Gateway (9002)          │
│                                 │
│  ┌───────────────────────────┐ │
│  │   JWT Validation          │ │
│  │  • Verify signature       │ │
│  │  • Check expiration       │ │
│  │  • Validate issuer        │ │
│  └─────────────┬─────────────┘ │
│                │ 5. Valid?     │
│                ▼               │
│  ┌───────────────────────────┐ │
│  │  Extract User ID          │ │
│  │  • Get 'sub' claim        │ │
│  │  • Add X-User-ID header   │ │
│  └─────────────┬─────────────┘ │
└────────────────┼───────────────┘
                 │ 6. Forward
                 ▼
           ┌──────────────┐
           │ Microservice │
           │ (Headers:    │
           │  X-User-ID)  │
           └──────────────┘
```

### JWT Structure

**Encoded JWT** (sent in Authorization header):
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5OGY3YjM0Zi0xYzJlLTQ1YWEtOGE3Yi0zNGI1YzZkN2U4OTAiLCJpYXQiOjE3MDYwNDAwMDAsImV4cCI6MTcwNjA0MzYwMCwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3JlYWxtcy9jbGllbnQtcG9ydGFsIn0.signature
```

**Decoded JWT**:
```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "98f7b34f-1c2e-45aa-8a7b-34b5c6d7e890",  // User ID (Keycloak)
    "iat": 1706040000,                              // Issued at
    "exp": 1706043600,                              // Expiration (1 hour)
    "iss": "http://localhost:8080/realms/client-portal",
    "email": "john.doe@example.com",
    "name": "John Doe"
  },
  "signature": "..."  // RSA signature
}
```

### Public vs Protected Endpoints

**application.yml** (gateway):
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/client-portal
```

**SecurityConfig.java**:
```java
@Bean
public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(exchanges -> exchanges
            // Public endpoints (no JWT)
            .pathMatchers("/api/v1/principals/public/**").permitAll()
            .pathMatchers("/actuator/health").permitAll()

            // Protected endpoints (JWT required)
            .pathMatchers("/api/v1/**").authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2.jwt())
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
}
```

### User ID Extraction

**Purpose**: Microservices need to know which user is making the request.

**Implementation**:
```java
@Component
public class UserIdExtractorFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMap(auth -> {
                // Extract user ID from JWT
                String userId = auth.getToken().getClaimAsString("sub");

                // Add header for downstream services
                ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-User-ID", userId)
                    .build();

                return chain.filter(exchange.mutate().request(request).build());
            })
            .switchIfEmpty(chain.filter(exchange));  // No JWT (public endpoint)
    }
}
```

**Usage in Microservices**:
```java
@GetMapping("/profile")
public Mono<ResponseEntity<?>> getProfile(@RequestHeader("X-User-ID") String userId) {
    return principalRepository.findByIamUserId(userId)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
}
```

**Why this approach?**
- Microservices don't need JWT parsing logic
- Consistent user identification across all services
- Simplifies authorization (just check userId)

---

## Configuration

### Environment Variables

**Purpose**: Externalize configuration for different environments (dev, staging, prod).

**application.properties** (example with placeholders):
```properties
# Keycloak
keycloak.base.url=${KEYCLOAK_URL:http://localhost:8080}
keycloak.realm=${KEYCLOAK_REALM:client-portal}
keycloak.client.id=${KEYCLOAK_CLIENT_ID:admin-client}
keycloak.client.secret=${KEYCLOAK_CLIENT_SECRET:}

# Plaid
plaid.client.id=${PLAID_CLIENT_ID:YOUR_CLIENT_ID}
plaid.secret=${PLAID_SECRET:YOUR_SECRET}
plaid.env=${PLAID_ENV:sandbox}

# AI Service
ai.service.url=${AI_SERVICE_URL:http://localhost:8000}

# Database
spring.r2dbc.url=${R2DBC_URL:r2dbc:h2:file:///./data/principaldb}
spring.r2dbc.username=${R2DBC_USERNAME:sa}
spring.r2dbc.password=${R2DBC_PASSWORD:}

# Storage
storage.base.path=${STORAGE_PATH:./uploads}
```

**Setting Environment Variables**:
```bash
# Development (local)
export PLAID_CLIENT_ID=your_dev_client_id
export PLAID_SECRET=your_dev_secret

# Production (Docker)
docker run -e PLAID_CLIENT_ID=prod_id -e PLAID_SECRET=prod_secret ...
```

### Service-Specific Configuration

#### client-ms
```properties
server.port=8081

# Keycloak
keycloak.base.url=http://localhost:8080
keycloak.realm=client-portal
keycloak.client.id=admin-client
keycloak.client.secret=YOUR_CLIENT_SECRET

# Database
spring.r2dbc.url=r2dbc:h2:file:///./data/principaldb
```

#### portfolio-ms
```properties
server.port=8082

# Plaid
plaid.client.id=YOUR_PLAID_CLIENT_ID
plaid.secret=YOUR_PLAID_SECRET
plaid.env=sandbox
plaid.api.url=https://sandbox.plaid.com

# Integration Service
integration.service.url=http://localhost:8083
```

#### integration-ms
```properties
server.port=8083

# Plaid
plaid.client.id=YOUR_PLAID_CLIENT_ID
plaid.secret=YOUR_PLAID_SECRET
plaid.env=sandbox

# Database
spring.r2dbc.url=r2dbc:h2:file:///./data/principaldb
```

#### document-ms
```properties
server.port=8084

# AI Service
ai.service.url=http://localhost:8000

# Storage
storage.base.path=./uploads

# Database
spring.r2dbc.url=r2dbc:h2:file:///./data/docdb

# File Upload
spring.codec.max-in-memory-size=10MB
```

#### gateway
```properties
server.port=9002

# OAuth2
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/client-portal

# Routing
spring.cloud.gateway.routes[0].id=client-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/v1/principals/**

# CORS
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedOrigins=http://localhost:5173
spring.cloud.gateway.globalcors.cors-configurations.[/**].allowedMethods=GET,POST,PUT,DELETE,OPTIONS
```

### Production Recommendations

1. **Database**: Migrate to PostgreSQL
```properties
spring.r2dbc.url=r2dbc:postgresql://db-host:5432/invictus
spring.r2dbc.username=invictus_user
spring.r2dbc.password=${DB_PASSWORD}
```

2. **Secrets Management**: Use Vault or AWS Secrets Manager
```yaml
spring:
  cloud:
    vault:
      uri: https://vault.example.com
      authentication: TOKEN
      token: ${VAULT_TOKEN}
```

3. **Observability**: Add distributed tracing
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

4. **Logging**: Centralized logging with ELK
```properties
logging.level.com.asbitech=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n
```

---

## Conclusion

This backend architecture provides a robust, scalable foundation for the Invictus wealth management platform. Key strengths:

- **Domain-Driven Design**: Clear business logic separation
- **Reactive Programming**: High-performance, non-blocking operations
- **CQRS Pattern**: Explicit command handling with type safety
- **Microservices**: Independent deployment and scaling
- **OAuth2 Security**: Industry-standard authentication
- **External Integrations**: Keycloak (IAM), Plaid (financial data), AI service (document processing)

For setup instructions, see [QUICKSTART.md](QUICKSTART.md).

For questions or issues, refer to the troubleshooting section in QUICKSTART.md or review logs in terminal windows for specific error messages.
