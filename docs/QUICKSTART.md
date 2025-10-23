# Quick Start Guide

Get your wealth management backend microservices running in minutes!

## Prerequisites

Before you begin, ensure you have:

- **Java 21**: JDK 21 or higher
- **Maven 3.8+**: Build tool for Java projects
- **Docker**: For running Keycloak (IAM service)
- **Internet connection**: For downloading dependencies

## Step 1: Start Keycloak

Keycloak provides authentication and authorization (OAuth2/JWT) for all microservices.

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_ENABLED=true \
  -e KC_HOSTNAME_STRICT_HTTPS=false \
  quay.io/keycloak/keycloak:24.0.2 \
  start-dev --http-enabled=true --hostname-strict-https=false
```

**Wait for Keycloak to start** (30-60 seconds):
```bash
# Check if Keycloak is ready
curl -I http://localhost:8080
# Should return HTTP 302 redirect when ready
```

## Step 2: Configure Keycloak

### 2a. Disable SSL Requirement (Development Only)

Use the Keycloak Admin CLI to disable SSL requirement:

```bash
# Access the Keycloak container
docker exec -it keycloak bash

# Authenticate the CLI tool
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Disable SSL requirement for master realm
/opt/keycloak/bin/kcadm.sh update realms/master \
  -s sslRequired=NONE

# Exit the container
exit
```

### 2b. Create Realm via Web Console

1. **Access Admin Console**: Open http://localhost:8080 in your browser
2. **Login**: Username: `admin`, Password: `admin`
3. Click the dropdown in top-left (shows "master")
4. Click **"Create realm"**
5. **Realm name**: `client-portal`
6. Click **"Create"**

### 2c. Disable SSL for New Realm

```bash
# Access Keycloak container again
docker exec -it keycloak bash

# Authenticate CLI (if not already authenticated)
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Disable SSL for client-portal realm
/opt/keycloak/bin/kcadm.sh update realms/client-portal \
  -s sslRequired=NONE

# Exit container
exit
```

### 2d. Create OAuth2 Client

1. In Keycloak console, go to **"Clients"** (left menu)
2. Click **"Create client"**
3. **Client ID**: `admin-client`
4. **Client type**: `OpenID Connect`
5. Click **"Next"**
6. **Client authentication**: `ON` ✓
7. **Authorization**: `OFF`
8. **Standard flow**: `ON`
9. **Service accounts roles**: `ON` ✓
10. Click **"Save"**

### 2e. Assign Service Account Roles

1. In **admin-client** settings, go to **"Service account roles"** tab
2. Click **"Assign role"**
3. Click **"Filter by realm roles"** dropdown
4. Change to **"Filter by clients"**
5. Select **`realm-management`** from the client list
6. Find and check **`realm-admin`** role
7. Click **"Assign"**

### 2f. Copy Client Secret

1. Go to **"Credentials"** tab
2. **Copy the Client secret** value
3. You'll need this in Step 4

## Step 3: Build All Services

Navigate to the backend directory and build:

```bash
cd back-client-portal

# Build all modules (installs common module dependency)
mvn clean install
```

**Expected output**: `BUILD SUCCESS` for all modules.

This will take 2-5 minutes depending on your internet speed (downloading dependencies on first run).

## Step 4: Update Client Secret

Open the KeycloakServiceImpl file:

**File**: `client-ms/src/main/java/com/asbitech/client_ms/infra/external/keycloak/impl/KeycloakServiceImpl.java`

**Line 37**: Replace with your actual client secret from Step 2f:
```java
private final String clientSecret = "YOUR_ACTUAL_CLIENT_SECRET_FROM_KEYCLOAK";
```

**Line 34**: Verify Keycloak URL is HTTP (not HTTPS):
```java
private final String keycloakBaseUrl = "http://localhost:8080";
```

Save the file.

## Step 5: Start Microservices

Open 5 separate terminal windows/tabs and start each service:

### Terminal 1 - Gateway Service
```bash
cd back-client-portal/gateway
mvn spring-boot:run
```
**Wait for**: "Started GatewayApplication" message

### Terminal 2 - Client Service
```bash
cd back-client-portal/client-ms
mvn spring-boot:run
```
**Wait for**: "Started ClientMsApplication" message

### Terminal 3 - Portfolio Service
```bash
cd back-client-portal/portfolio-ms
mvn spring-boot:run
```
**Wait for**: "Started PortfolioMsApplication" message

### Terminal 4 - Integration Service
```bash
cd back-client-portal/integration-ms
mvn spring-boot:run
```
**Wait for**: "Started IntegrationMsApplication" message

### Terminal 5 - Document Service
```bash
cd back-client-portal/document-ms
mvn spring-boot:run
```
**Wait for**: "Started DocumentMsApplication" message

## Step 6: Verify Setup

Check that all services are running:

```bash
# Gateway
curl http://localhost:9002/actuator/health

# Client Service
curl http://localhost:8081/actuator/health

# Portfolio Service
curl http://localhost:8082/actuator/health

# Integration Service
curl http://localhost:8083/actuator/health

# Document Service
curl http://localhost:8084/actuator/health
```

**Expected output**: `{"status":"UP"}` for all services.

## Step 7: Test Your First API Call

Test user registration (public endpoint, no authentication required):

```bash
curl -X POST http://localhost:9002/api/v1/principals/public \
  -H "Content-Type: application/json" \
  -d '{
    "alias": "John Doe",
    "mainPlatformId": "user123",
    "mail": "john.doe@example.com"
  }'
```

**Expected response**: JSON with user details and activation token.

If you see this response (not an error about HTTPS), congratulations! Your backend is running successfully.

## Quick Reference

### Start Development Session

After initial setup, start services in this order:

```bash
# 1. Start Keycloak (if not running)
docker start keycloak

# 2. Start all microservices (5 separate terminals)
cd back-client-portal/gateway && mvn spring-boot:run
cd back-client-portal/client-ms && mvn spring-boot:run
cd back-client-portal/portfolio-ms && mvn spring-boot:run
cd back-client-portal/integration-ms && mvn spring-boot:run
cd back-client-portal/document-ms && mvn spring-boot:run
```

### Stop All Services

```bash
# Stop Spring Boot services
pkill -f "spring-boot:run"

# Stop Keycloak container
docker stop keycloak
```

### Rebuild After Code Changes

```bash
# Rebuild all modules
cd back-client-portal
mvn clean install

# Restart affected services
```

### Check Service Status

```bash
# Check all health endpoints at once
for port in 8081 8082 8083 8084 9002; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | grep -o '"status":"[^"]*"')"
done
```

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| **Keycloak** | 8080 | Authentication/IAM |
| **Gateway** | 9002 | API Gateway (OAuth2 security, routing) |
| **client-ms** | 8081 | User accounts, authentication, MFA |
| **portfolio-ms** | 8082 | Portfolio management, assets |
| **integration-ms** | 8083 | External integrations (Plaid) |
| **document-ms** | 8084 | Document upload, AI extraction |

## What's Configured?

### Architecture
- **Microservices**: Domain-Driven Design with CQRS pattern
- **Database**: H2 (file-based, embedded) - auto-created on first run
- **Security**: OAuth2/JWT via Keycloak
- **Communication**: Reactive programming (Spring WebFlux)

### External Integrations
- **Keycloak**: Identity and Access Management
- **Plaid**: Financial data integration (placeholder credentials configured)
- **AI Service**: Document processing via `client-portal-data` (Python service)

### API Gateway
- **Public endpoints**: `/api/v1/principals/public/*` (no authentication)
- **Protected endpoints**: All others require `Authorization: Bearer <JWT>`
- **CORS**: Configured for frontend at `http://localhost:5173`

## Next Steps

### Learn More
- **Architecture Guide**: [BACKEND_GUIDE.md](BACKEND_GUIDE.md) - Comprehensive technical documentation
- **Frontend Integration**: Point your frontend to `http://localhost:9002` (gateway)
- **Database Console**: Access H2 console at service URLs (e.g., `http://localhost:8081/h2-console`)

### Development
1. **Hot reload**: All services support hot reload with Maven
2. **Logging**: Check terminal windows for detailed logs
3. **Debugging**: Connect IDE debugger to service ports (default: 5005)
4. **Testing**: Run tests with `mvn test` in each service directory

### Configure Plaid Integration (Optional)
1. Sign up at [Plaid Dashboard](https://dashboard.plaid.com/signup)
2. Get your `client_id` and `secret` from dashboard
3. Update in `integration-ms/src/main/resources/application.properties`:
   ```properties
   plaid.client.id=YOUR_CLIENT_ID
   plaid.secret=YOUR_SECRET
   plaid.env=sandbox
   ```
4. Also update in `portfolio-ms/src/main/resources/application.properties`
5. Restart integration-ms and portfolio-ms

### Configure AI Document Processing (Optional)
The `document-ms` integrates with `client-portal-data` (Python AI service) for document extraction:

1. Follow setup guide: [client-portal-data/docs/QUICKSTART.md](../client-portal-data/docs/QUICKSTART.md)
2. Start AI service: `python -m extractors.api` (runs on port 8000)
3. Upload documents via document-ms API - they'll be processed automatically

## Troubleshooting

### Issue: Service won't start (port already in use)
```bash
# Find and kill processes using the port
lsof -ti:8081 | xargs kill -9  # Replace 8081 with your port
```

### Issue: "HTTPS required" error from Keycloak
**Solution**: Follow Step 2a and 2c to disable SSL via CLI

### Issue: "Client secret not found" or authentication failures
**Solution**:
1. Verify client secret is correctly copied in KeycloakServiceImpl.java (Step 4)
2. Ensure `admin-client` has `realm-admin` role assigned (Step 2e)

### Issue: Database connection errors
```bash
# Clean database files and restart
rm -rf back-client-portal/data
# Restart affected services - databases will be recreated
```

### Issue: Build fails with "Cannot resolve dependency: common"
```bash
# Rebuild from parent directory
cd back-client-portal
mvn clean install -DskipTests
```

## Support

Having issues not covered here?

1. **Check logs**: Review terminal output for specific error messages
2. **Verify configuration**: Ensure Keycloak realm is `client-portal`, client ID is `admin-client`
3. **Read detailed docs**: [BACKEND_GUIDE.md](BACKEND_GUIDE.md) has comprehensive troubleshooting
4. **Database inspection**: Use H2 console to check data

---

**Success!** You should now have a running wealth management microservices backend. Start building your frontend or explore the APIs!
