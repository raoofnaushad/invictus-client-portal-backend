# 💼 Wealth Management SaaS – Microservices Platform

A **Spring Boot–based microservices architecture** for managing wealth portfolios, clients, transactions, and real-time reporting.  
The platform follows **Domain-Driven Design (DDD)**, **Reactive Programming (WebFlux + R2DBC)**, and **Event-Driven Architecture (Kafka/RabbitMQ)** principles.

---

## 🧩 Microservices Overview

| Service | Description |
|----------|--------------|
| **Client Service** | Handles client accounts, authentication, and family relationships. |
| **Document Service** | Manages document upload, classification, and extraction (OCR/AI). |
| **Integration Service** | Handles external integration - Plaid |
| **Portfolio Service** | Tracks assets, liabilities, portfolios, and allocations. |
| **Reporting Service** | Provides real-time dashboards, analytics, and exportable reports. |
| **Gateway Service** | API Gateway for routing, load balancing, and centralized authentication. |

---

## ⚙️ Tech Stack

**Backend**
- Java 21 / Spring Boot 3.x
- Keycloack - IAM
- Spring WebFlux (Reactive)
- Spring Data R2DBC / PostgreSQL
- OpenAPI / Swagger

  ## 🔐 Keycloak Integration

### 1️⃣ Start Keycloak via Docker

```bash
docker run -d \
  --name keycloak \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:24.0.2 \
  start-dev

## ⚙️ Run Ms

- cd client-ms
- mvn spring-boot:run



