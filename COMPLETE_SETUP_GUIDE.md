# 🚀 Complete Setup Guide - Wealth Management Microservices Platform

This guide provides step-by-step instructions to set up the entire microservices platform from scratch, addressing all common configuration issues.

## 📋 Prerequisites

Before starting, ensure you have:

- **Java 21** (JDK) installed
- **Maven 3.8+** installed  
- **Docker** installed and running
- **Git** (for cloning repositories)

### Verify Prerequisites
```bash
java --version    # Should show Java 21
mvn --version     # Should show Maven 3.8+
docker --version  # Should show Docker version
```

---

## 🏗️ Architecture Overview

### Microservices
| Service | Port | Purpose | Database |
|---------|------|---------|----------|
| **gateway** | 9002 | API Gateway, OAuth2 security, routing | None |
| **client-ms** | 8081 | User accounts, authentication, MFA | H2 (principaldb) |
| **portfolio-ms** | 8082 | Portfolio management, assets | H2 (principaldb) |
| **integration-ms** | 8083 | External integrations (Plaid) | H2 (principaldb) |
| **document-ms** | 8084 | Document upload, AI extraction | H2 (docdb) |
| **keycloak** | 8080 | Identity & Access Management | Built-in |

### Request Flow
```
Client → Gateway (9002) → Microservice (808x) → Database
         ↓ JWT validation
         Keycloak (8080)
```

---

## 🔧 Step-by-Step Setup

### Step 1: Clean Up Previous Installations

If you've tried running this before, clean up first:

```bash
# Stop any running containers
docker stop keycloak 2>/dev/null && docker rm keycloak 2>/dev/null

# Stop any running Spring Boot processes
pkill -f "spring-boot:run" 2>/dev/null

# Clean up database files (optional - removes all data)
rm -rf /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/data 2>/dev/null
```

### Step 2: Start Keycloak with Proper Development Configuration

**CRITICAL**: Keycloak must be started with HTTP enabled for development:

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

**Wait for Keycloak to start** (about 30-60 seconds):
```bash
# Check if Keycloak is ready
curl -I http://localhost:8080
# Should return HTTP 302 redirect when ready
```

### Step 2.5: Disable SSL Requirement via CLI (CRITICAL for First-Time Setup)

**Important**: If you encounter "HTTPS required" error when accessing http://localhost:8080, the master realm's SSL setting needs to be disabled first. Use the Keycloak Admin CLI:

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

**What this does**: Disables the SSL requirement for the master realm, allowing HTTP access to the admin console in development.

**After running these commands**: Refresh your browser at http://localhost:8080 - the "HTTPS required" error should be gone, and you can proceed to Step 3.

### Step 3: Configure Keycloak Admin Console

1. **Access Admin Console**: Open http://localhost:8080 in your browser
2. **Login**: Username: `admin`, Password: `admin`

#### Create the Realm
1. Click the dropdown in top-left corner (currently shows "master")
2. Click **"Create realm"**
3. **Realm name**: `client-portal`
4. Click **"Create"**

#### Configure SSL Requirements (CRITICAL STEP)

**Note**: If you completed Step 2.5 (CLI method), the master realm SSL is already disabled. You still need to configure SSL for the client-portal realm after creating it.

1. In the `client-portal` realm, go to: **Realm Settings** → **General** tab
2. Find **"Require SSL"** setting
3. Change from **"External requests"** to **"None"** ✅
4. Click **"Save"**

**Alternatively, use CLI method** (if you prefer):
```bash
docker exec -it keycloak bash
/opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin
/opt/keycloak/bin/kcadm.sh update realms/client-portal -s sslRequired=NONE
exit
```

#### Create OAuth2 Client
1. Go to **"Clients"** in the left menu
2. Click **"Create client"**
3. **Client ID**: `admin-client`
4. **Client type**: `OpenID Connect`
5. Click **"Next"**
6. **Client authentication**: `ON` ✅
7. **Authorization**: `OFF`
8. **Standard flow**: `ON`
9. **Service accounts roles**: `ON` ✅
10. Click **"Save"**

#### Configure Service Account Roles
1. In the **admin-client** settings, go to **"Service account roles"** tab
2. Click **"Assign role"**
3. Click **"Filter by realm roles"** dropdown at the top-left
4. Change to **"Filter by clients"**
5. Select **`realm-management`** from the client list
6. Find and check the **`realm-admin`** role (gives full admin access to the realm)
7. Click **"Assign"**

**What this does**: The `realm-admin` role from the `realm-management` client allows the service account to manage users, clients, and realm settings via the Keycloak Admin API.

**Alternatively**, you can assign specific granular roles instead of `realm-admin`:
- `manage-users` - Create, update, delete users
- `view-users` - View user information
- `manage-clients` - Manage OAuth2 clients
- `query-users` - Search for users

#### Get Client Secret
1. Go to **"Credentials"** tab
2. **Copy the Client secret** value
3. **Important**: You'll need to update this in the code (see Step 5)

### Step 4: Build All Microservices

Navigate to the project directory and build all modules:

```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal

# Build all modules (installs common module dependency)
mvn clean install
```

**Expected output**: `BUILD SUCCESS` for all modules.

### Step 5: Update Configuration Files

#### 5.1 Update Client Secret in KeycloakServiceImpl

**File**: `client-ms/src/main/java/com/asbitech/client_ms/infra/external/keycloak/impl/KeycloakServiceImpl.java`

**Line 34**: Ensure Keycloak URL is HTTP:
```java
private final String keycloakBaseUrl = "http://localhost:8080";
```

**Line 37**: Update with your actual client secret from Step 3:
```java
private final String clientSecret = "YOUR_ACTUAL_CLIENT_SECRET_FROM_KEYCLOAK";
```

#### 5.2 Verify Gateway Configuration

**File**: `gateway/src/main/resources/application.yml`

Ensure the JWT issuer URI is correct:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/client-portal
```

### Step 6: Start All Microservices

Open 5 separate terminal windows/tabs and run each service:

#### Terminal 1 - Gateway Service
```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/gateway
mvn spring-boot:run
```
**Wait for**: "Started GatewayApplication"

#### Terminal 2 - Client Service  
```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/client-ms
mvn spring-boot:run
```
**Wait for**: "Started ClientMsApplication"

#### Terminal 3 - Portfolio Service
```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/portfolio-ms
mvn spring-boot:run
```

#### Terminal 4 - Integration Service
```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/integration-ms
mvn spring-boot:run
```

#### Terminal 5 - Document Service
```bash
cd /Users/raoof/Documents/work/space/invictus/invictus-client-portal/back-client-portal/document-ms
mvn spring-boot:run
```

### Step 7: Verify All Services Are Running

Check health endpoints:
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

All should return `{"status":"UP"}`.

---

## 🧪 Testing the Setup

### Test 1: Public API Endpoint (No Authentication)

Test user registration through the gateway:

```bash
curl -X POST http://localhost:9002/api/v1/principals/public \
  -H "Content-Type: application/json" \
  -d '{
    "alias": "test user",
    "mainPlatformId": "test123",
    "mail": "test@example.com"
  }'
```

**Expected response**: Success response with user details (not error about HTTPS/SSL).

### Test 2: Gateway Routing

Test that gateway routes correctly to each service:

```bash
# Test 1: client-ms routing (public endpoint - no auth required)
curl -s -o /dev/null -w "client-ms route: HTTP %{http_code}\n" http://localhost:9002/api/v1/principals/public

# Test 2: portfolio-ms routing (protected - requires JWT)
curl -s -o /dev/null -w "portfolio-ms route: HTTP %{http_code}\n" http://localhost:9002/api/v1/portfolios/test

# Test 3: integration-ms routing (protected - requires JWT)
curl -s -o /dev/null -w "integration-ms route: HTTP %{http_code}\n" http://localhost:9002/api/v1/integrations/test

# Test 4: document-ms routing (protected - requires JWT)
curl -s -o /dev/null -w "document-ms route: HTTP %{http_code}\n" http://localhost:9002/api/v1/documents/test
```

**Expected output:**
```
client-ms route: HTTP 405
portfolio-ms route: HTTP 401
integration-ms route: HTTP 401
document-ms route: HTTP 401
```

**What this validates:**
- **HTTP 405** from client-ms: Public endpoint exists, wrong HTTP method (needs POST) ✅
- **HTTP 401** from other services: Gateway security is blocking requests without JWT tokens ✅
- All routes are correctly configured and working!

**Understanding the results:**
- **401 Unauthorized = SUCCESS** - The gateway is correctly enforcing authentication
- **405 Method Not Allowed = SUCCESS** - Public endpoint is accessible without auth
- These responses prove the gateway is routing requests to the correct backend services

**If you see:**
- ❌ Connection timeouts: Backend service isn't running
- ❌ Connection refused: Gateway isn't running
- ✅ HTTP status codes (401, 405, 404): Routing is working correctly!

---

## 🐛 Troubleshooting

### Issue: "HTTPS required" Error

**Symptoms**:
- Error in Postman: `"message": "Keycloak API error: {\"error\":\"invalid_request\",\"error_description\":\"HTTPS required\"}"`
- Browser shows "HTTPS required" when accessing Keycloak
- Cannot access Keycloak Admin Console at http://localhost:8080

**Solution (Method 1 - CLI - Recommended)**:

Use the Keycloak Admin CLI to disable SSL requirement:

```bash
# Access the Keycloak container
docker exec -it keycloak bash

# Authenticate CLI
/opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

# Disable SSL for master realm
/opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

# Disable SSL for client-portal realm (if it exists)
/opt/keycloak/bin/kcadm.sh update realms/client-portal -s sslRequired=NONE

# Exit container
exit
```

Refresh browser and the error should be gone.

**Solution (Method 2 - Restart Container)**:

If CLI method doesn't work, restart Keycloak with proper development flags:
1. Stop and remove container: `docker stop keycloak && docker rm keycloak`
2. Restart with proper flags (see Step 2)
3. Follow Step 2.5 to disable SSL via CLI

**Solution (Method 3 - Web Console)**:
1. If you can access the admin console, configure realm SSL requirement to "None" (Step 3)
2. Ensure KeycloakServiceImpl uses `http://` not `https://` (Step 5.1)

### Issue: "NotSslRecordException" in Logs

**Symptoms**: 
```
io.netty.handler.ssl.NotSslRecordException: not an SSL/TLS record
```

**Solution**: 
- Change `keycloakBaseUrl` from `https://localhost:8080` to `http://localhost:8080` in KeycloakServiceImpl.java

### Issue: Services Won't Start

**Symptoms**: Port already in use errors

**Solution**:
```bash
# Find and kill processes using ports
lsof -ti:8080,8081,8082,8083,8084,9002 | xargs kill -9

# Clean restart all services
```

### Issue: "Client secret not found" or Authentication Failures

**Solution**:
1. Verify you copied the correct client secret from Keycloak Credentials tab
2. Ensure `admin-client` has proper service account roles assigned
3. Check realm name is exactly `client-portal`

### Issue: Database Connection Errors

**Solution**:
```bash
# Clean database files and restart
rm -rf back-client-portal/data
# Restart affected services - they will recreate databases
```

---

## 📊 Service URLs & Endpoints

| Service | Health Check | Admin Console | H2 Console |
|---------|--------------|---------------|------------|
| **Gateway** | http://localhost:9002/actuator/health | - | - |
| **Client** | http://localhost:8081/actuator/health | - | http://localhost:8081/h2-console |
| **Portfolio** | http://localhost:8082/actuator/health | - | http://localhost:8082/h2-console |
| **Integration** | http://localhost:8083/actuator/health | - | http://localhost:8083/h2-console |
| **Document** | http://localhost:8084/actuator/health | - | http://localhost:8084/h2-console |
| **Keycloak** | http://localhost:8080 | http://localhost:8080 | - |

### H2 Database Console Access
- **URL**: `jdbc:h2:file:./data/principaldb` (or `./data/docdb` for document-ms)
- **Username**: `sa`
- **Password**: *(leave blank)*

---

## 🔐 API Testing Examples

### Create User Account
```bash
curl -X POST http://localhost:9002/api/v1/principals/public \
  -H "Content-Type: application/json" \
  -d '{
    "alias": "John Doe",
    "mainPlatformId": "user123",
    "mail": "john.doe@example.com"
  }'
```

### Activate Account (after user creation)
```bash
curl -X POST http://localhost:9002/api/v1/principals/public/activate \
  -H "Content-Type: application/json" \
  -d '{
    "activationToken": "TOKEN_FROM_CREATION_RESPONSE",
    "tempPassword": "TEMP_PASSWORD_FROM_CREATION_RESPONSE"
  }'
```

---

## 🎯 Next Steps

Once everything is running:

1. **Frontend Integration**: The frontend should connect to `http://localhost:9002` (gateway)
2. **API Documentation**: Access Swagger UI at individual service endpoints
3. **Database Management**: Use H2 consoles to inspect data
4. **Development**: All services support hot reload with Maven

---

## 📝 Quick Reference Commands

```bash
# Stop all services
pkill -f "spring-boot:run"
docker stop keycloak

# Clean restart Keycloak
docker rm keycloak
docker run -d --name keycloak -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin \
  -e KC_HTTP_ENABLED=true -e KC_HOSTNAME_STRICT_HTTPS=false \
  quay.io/keycloak/keycloak:24.0.2 start-dev --http-enabled=true --hostname-strict-https=false

# Rebuild all services
cd back-client-portal && mvn clean install

# Check all health endpoints
for port in 8081 8082 8083 8084 9002; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq -r .status)"
done
```

---

**🎉 Congratulations!** If you've followed all steps, your wealth management microservices platform should now be running successfully.

For issues not covered here, check the logs in your terminal windows for specific error messages.
