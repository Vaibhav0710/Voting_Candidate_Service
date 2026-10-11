# 🗳️ Candidate Service

> Part of the **Blockchain-Inspired Online Voting System** — a production-grade, scalable microservices platform.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-blue.svg)](https://spring.io/projects/spring-cloud)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Tests](https://img.shields.io/badge/Tests-58%20Passing-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📌 Overview

The **Candidate Service** manages the complete lifecycle of election candidates — registration, updates, status management, and cross-service validation. It is a core microservice consumed by the Voting Service (to validate candidates before accepting votes) and the Result Service (to display candidate information in results).

### Feature Status
- ✅ Full CRUD operations (Create, Read, Update, Soft-Delete)
- ✅ Standardized `ApiResponse<T>` wrapper on all endpoints
- ✅ Global Exception Handling (`@RestControllerAdvice`)
- ✅ DTO-based request/response separation (7 DTOs)
- ✅ Public ID Pattern (Internal `Long` PK + External `UUID`)
- ✅ Soft-delete with `is_deleted` flag
- ✅ Bulk candidate registration
- ✅ Status management (ACTIVE / DISQUALIFIED / WITHDRAWN)
- ✅ Election-scoped candidate queries
- ✅ Candidate existence & validation endpoints (for Feign)
- ✅ Swagger/OpenAPI documentation
- ✅ Unit + Integration tests (58 tests passing)
- ✅ Sample data seeding (`DataSeeder`)
- ✅ Kafka event publishing (keyed by candidate id, sent only after the DB commit)
- ✅ Spring Security + JWT (writes need ADMIN; JSON 401/403; 500s never leak details)
- ✅ Redis caching of candidate reads (10 min TTL, evicted on every write, falls back to the DB if Redis fails)
- ✅ Input limits (name/party ≤ 100 chars, bulk ≤ 500 candidates)

---

## 🏗️ Architecture

```
                    ┌──────────────────┐
                    │   API Gateway    │
                    │ (Spring Cloud)   │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──┐  ┌───────▼────┐  ┌──────▼──────┐
    │  User      │  │★CANDIDATE★ │  │  Voting     │
    │  Service   │  │  SERVICE   │  │  Service    │
    │  (Auth)    │  │  (8082)    │  │  (8083)     │
    └────────────┘  └──────┬─────┘  └──────┬──────┘
                           │               │
                     ┌─────▼──────┐        │ OpenFeign
                     │ PostgreSQL │        │ (validates
                     │ candidate  │◄───────┘  candidates)
                     │ service_db │
                     └────────────┘
```

### Cross-Service Communication

| Consumer | Protocol | Endpoints Called | Purpose |
|----------|----------|--------------------|---------|
| **Voting Service** | OpenFeign (sync) | `GET /{id}/exists` | Fast existence check before vote |
| **Voting Service** | OpenFeign (sync) | `GET /{id}/validate?electionId=X` | Full validation (exists + election + active) |
| **Result Service** | OpenFeign (sync) | `GET /election/{electionId}` | Candidate names/parties for results |
| **Result Service** | OpenFeign (sync) | `GET /election/{electionId}/active` | Active candidates only |
| **Voting Service** | Kafka (async) | Listens to `candidate.*` topics | Keeps its candidate status cache current (used if this service is down) |

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.6 |
| Cloud | Spring Cloud | 2023.0.3 |
| Database | PostgreSQL (dedicated) | 16 |
| ORM | Spring Data JPA / Hibernate | — |
| API Docs | Swagger UI / OpenAPI 3.0 | springdoc 2.5.0 |
| Validation | Jakarta Bean Validation | — |
| Service Discovery | Eureka Client | disabled until server ready |
| Build Tool | Maven | 3.8+ |
| Boilerplate | Lombok | — |
| Test DB | H2 (in-memory) | test scope |

---

## 🔌 API Reference

### Base URL
```
http://localhost:8082/api/v1/candidates
```

### Swagger UI
```
http://localhost:8082/swagger-ui.html
```

### All Endpoints (11 total)

#### Phase 1 — Core CRUD

| Method | Endpoint | Description | Status Code | Auth |
|--------|----------|-------------|-------------|------|
| `POST` | `/api/v1/candidates` | Register a new candidate | 201 | ADMIN |
| `GET` | `/api/v1/candidates/{id}` | Get candidate by public UUID | 200 | PUBLIC |
| `GET` | `/api/v1/candidates` | List all candidates (paginated) | 200 | PUBLIC |
| `PUT` | `/api/v1/candidates/{id}` | Update candidate details | 200 | ADMIN |
| `DELETE` | `/api/v1/candidates/{id}` | Soft-delete a candidate | 200 | ADMIN |

#### Phase 2 — Election-Scoped (Inter-Service)

| Method | Endpoint | Description | Status Code | Auth |
|--------|----------|-------------|-------------|------|
| `GET` | `/api/v1/candidates/election/{electionId}` | All candidates for an election | 200 | PUBLIC |
| `GET` | `/api/v1/candidates/{id}/exists` | Lightweight existence check | 200 | INTERNAL |
| `GET` | `/api/v1/candidates/{id}/validate?electionId=X` | Full validation for voting | 200 | INTERNAL |

#### Phase 3 — Bulk Operations

| Method | Endpoint | Description | Status Code | Auth |
|--------|----------|-------------|-------------|------|
| `POST` | `/api/v1/candidates/bulk` | Register multiple candidates | 201 | ADMIN |

#### Phase 4 — Status Management

| Method | Endpoint | Description | Status Code | Auth |
|--------|----------|-------------|-------------|------|
| `PATCH` | `/api/v1/candidates/{id}/status` | Change candidate status | 200 | ADMIN |
| `GET` | `/api/v1/candidates/election/{electionId}/active` | Active candidates only | 200 | PUBLIC |

### Error Codes

| Code | Meaning | When |
|------|---------|------|
| `400` | Bad Request | Validation failure (`@NotBlank`, `@NotNull`) |
| `404` | Not Found | Candidate doesn't exist or was soft-deleted |
| `409` | Conflict | Duplicate candidate name in same election |
| `500` | Internal Error | Unexpected server failure |

---

## 📝 Request / Response Examples

<details>
<summary><b>POST /api/v1/candidates</b> — Register Candidate</summary>

**Request:**
```json
{
  "name": "Rahul Sharma",
  "party": "Swaraj Party",
  "electionId": "11111111-1111-1111-1111-111111111111"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Candidate created successfully",
  "data": {
    "id": "a3f2b1c4-5d6e-7f8a-9b0c-1d2e3f4a5b6c",
    "name": "Rahul Sharma",
    "party": "Swaraj Party",
    "electionId": "11111111-1111-1111-1111-111111111111",
    "status": "ACTIVE",
    "createdAt": "2026-04-21T10:30:00",
    "updatedAt": "2026-04-21T10:30:00"
  },
  "timestamp": "2026-04-21T10:30:00"
}
```
</details>

<details>
<summary><b>POST /api/v1/candidates/bulk</b> — Bulk Register</summary>

**Request:**
```json
{
  "candidates": [
    { "name": "Amit Verma", "party": "Green Party", "electionId": "11111111-1111-1111-1111-111111111111" },
    { "name": "Priya Patel", "party": "Independent", "electionId": "11111111-1111-1111-1111-111111111111" }
  ]
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Bulk registration successful",
  "data": [
    { "id": "...", "name": "Amit Verma", "status": "ACTIVE", ... },
    { "id": "...", "name": "Priya Patel", "status": "ACTIVE", ... }
  ],
  "timestamp": "2026-04-21T10:30:00"
}
```
</details>

<details>
<summary><b>GET /api/v1/candidates?page=0&size=5&sort=name,asc</b> — Paginated List</summary>

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Candidates retrieved successfully",
  "data": {
    "content": [
      {
        "id": "a3f2b1c4-...",
        "name": "Alice Smith",
        "party": "Liberty Party",
        "electionId": "22222222-2222-2222-2222-222222222222",
        "status": "ACTIVE",
        "createdAt": "2026-04-21T10:00:00",
        "updatedAt": "2026-04-21T10:00:00"
      }
    ],
    "totalElements": 10,
    "totalPages": 2,
    "size": 5,
    "number": 0
  },
  "timestamp": "2026-04-21T10:30:00"
}
```
</details>

<details>
<summary><b>PATCH /api/v1/candidates/{id}/status</b> — Update Status</summary>

**Request:**
```json
{
  "status": "DISQUALIFIED"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Candidate status updated successfully",
  "data": {
    "id": "a3f2b1c4-...",
    "name": "Rahul Sharma",
    "party": "Swaraj Party",
    "electionId": "11111111-1111-1111-1111-111111111111",
    "status": "DISQUALIFIED",
    "createdAt": "2026-04-21T10:30:00",
    "updatedAt": "2026-04-21T12:45:00"
  },
  "timestamp": "2026-04-21T12:45:00"
}
```
</details>

<details>
<summary><b>GET /api/v1/candidates/{id}/validate?electionId=X</b> — Validate for Voting</summary>

**Response (200 OK — Valid):**
```json
{
  "success": true,
  "message": "Validation completed",
  "data": {
    "candidateId": "a3f2b1c4-...",
    "electionId": "11111111-...",
    "valid": true,
    "currentStatus": "ACTIVE",
    "message": "Valid"
  },
  "timestamp": "2026-04-21T10:30:00"
}
```

**Response (200 OK — Invalid, wrong election):**
```json
{
  "success": true,
  "message": "Validation completed",
  "data": {
    "candidateId": "a3f2b1c4-...",
    "electionId": "99999999-...",
    "valid": false,
    "currentStatus": "ACTIVE",
    "message": "Candidate does not belong to this election"
  },
  "timestamp": "2026-04-21T10:30:00"
}
```
</details>

<details>
<summary><b>Validation Error (400)</b> — Blank Name + Null Election</summary>

**Request:**
```json
{
  "name": "",
  "electionId": null
}
```

**Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "name": "Candidate name cannot be blank",
    "electionId": "Election ID must be provided"
  },
  "timestamp": "2026-04-21T10:30:00"
}
```
</details>

---

## 🗄️ Database Schema

**Database:** `candidateservice_db` (isolated — each microservice owns its data)

### Public ID Pattern

The service uses a **dual-ID strategy** used at FAANG companies:

| Column | Type | Purpose | Exposed in API? |
|--------|------|---------|-----------------|
| `id` | `BIGSERIAL` | Internal PK (fast joins/indexes) | ❌ Never |
| `external_id` | `UUID` | Public identifier (secure, unpredictable) | ✅ Always |

```sql
CREATE TABLE candidates (
    id              BIGSERIAL PRIMARY KEY,
    external_id     UUID UNIQUE NOT NULL,
    name            VARCHAR(255) NOT NULL,
    party           VARCHAR(255),
    election_id     UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL,
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Recommended indexes
CREATE INDEX idx_candidates_external_id ON candidates(external_id);
CREATE INDEX idx_candidates_election_id ON candidates(election_id);
CREATE INDEX idx_candidates_election_status ON candidates(election_id, status);
```

### Status Values

| Status | Description |
|--------|-------------|
| `ACTIVE` | Eligible for votes (default on creation) |
| `DISQUALIFIED` | Removed by admin for rules violation |
| `WITHDRAWN` | Candidate voluntarily stepped down |

---

## 📦 Project Structure

```
candidate-service/
├── src/
│   ├── main/
│   │   ├── java/com/voting/candidateservice/
│   │   │   ├── CandidateServiceApplication.java    ← Entry point
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java                ← Redis read caches (typed JSON, 10 min TTL)
│   │   │   │   ├── DataSeeder.java                 ← Seeds 10 sample candidates
│   │   │   │   └── KafkaConfig.java                ← candidate.* topics
│   │   │   ├── controller/
│   │   │   │   └── CandidateController.java        ← 11 REST endpoints
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java                ← Generic response wrapper
│   │   │   │   ├── BulkCandidateRequestDTO.java    ← Bulk create request
│   │   │   │   ├── CandidateRequestDTO.java        ← Create request (validated)
│   │   │   │   ├── CandidateResponseDTO.java       ← Response payload
│   │   │   │   ├── CandidateStatusUpdateDTO.java   ← Status change request
│   │   │   │   ├── CandidateUpdateDTO.java         ← Update request (validated)
│   │   │   │   └── CandidateValidationDTO.java     ← Feign validation response
│   │   │   ├── exception/
│   │   │   │   ├── DuplicateResourceException.java ← 409 Conflict
│   │   │   │   ├── GlobalExceptionHandler.java     ← @RestControllerAdvice
│   │   │   │   └── ResourceNotFoundException.java  ← 404 Not Found
│   │   │   ├── mapper/
│   │   │   │   └── CandidateMapper.java            ← Entity ↔ DTO conversion
│   │   │   ├── model/
│   │   │   │   ├── Candidate.java                  ← JPA Entity
│   │   │   │   └── enums/
│   │   │   │       └── CandidateStatus.java        ← ACTIVE, DISQUALIFIED, WITHDRAWN
│   │   │   ├── repository/
│   │   │   │   └── CandidateRepository.java        ← Spring Data JPA
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java    ← Reads the bearer token
│   │   │   │   ├── JwtPrincipal.java               ← userId, username, role
│   │   │   │   ├── JwtService.java                 ← HS256 verification
│   │   │   │   └── SecurityConfig.java             ← Public reads, ADMIN writes, JSON 401/403
│   │   │   └── service/
│   │   │       ├── CandidateEventProducer.java     ← After-commit Kafka events
│   │   │       ├── CandidateService.java           ← Interface (contract)
│   │   │       └── CandidateServiceImpl.java       ← Implementation (business logic, caching)
│   │   └── resources/
│   │       └── application.yml                     ← Database, Kafka, Redis, Eureka config
│   └── test/
│       └── java/com/voting/candidateservice/
│           ├── CandidateServiceApplicationTests.java   ← 1 test (context loads)
│           ├── config/CacheConfigTest.java              ← 3 tests (Redis serializers)
│           ├── controller/
│           │   ├── CandidateControllerTest.java         ← 16 tests (MockMvc)
│           │   └── CandidateSecurityIntegrationTest.java← 9 tests (roles, 401/403, input limits)
│           ├── repository/CandidateRepositoryTest.java  ← 4 tests (@DataJpaTest + H2)
│           └── service/
│               ├── CandidateCachingTest.java            ← 5 tests (cache hits and evictions)
│               ├── CandidateEventProducerTest.java      ← 5 tests (after-commit, failures)
│               └── CandidateServiceImplTest.java        ← 15 tests (Mockito)
├── README.md                                       ← This file
├── pom.xml
└── .gitignore
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/candidate-service.git
   cd candidate-service
   ```

2. **Create PostgreSQL database**
   ```sql
   CREATE DATABASE candidateservice_db;
   ```

3. **Set environment variables**
   ```bash
   # Linux / macOS
   export VOTING_DB_URL=jdbc:postgresql://localhost:5432   # server base URL; /candidateservice_db is appended
   export VOTING_DB_USER=postgres
   export VOTING_DB_PASSWORD=yourpassword

   # Windows (PowerShell)
   $env:VOTING_DB_URL="jdbc:postgresql://localhost:5432"
   $env:VOTING_DB_USER="postgres"
   $env:VOTING_DB_PASSWORD="yourpassword"
   ```

4. **Build the project**
   ```bash
   mvn clean install
   ```

5. **Run the service**
   ```bash
   mvn spring-boot:run
   ```

6. **Verify**
   ```bash
   # Health check
   curl http://localhost:8082/actuator/health

   # Swagger UI (open in browser)
   http://localhost:8082/swagger-ui.html

   # Test API
   curl http://localhost:8082/api/v1/candidates
   ```

> **Note:** On first startup, `DataSeeder` will automatically create 10 sample candidates across 2 elections if the database is empty.

---

## 🧪 Testing

### Test Suite Summary

| Test Class | Tests | Type | Framework |
|-----------|-------|------|-----------|
| `CandidateServiceImplTest` | 13 | Unit (business logic) | Mockito + JUnit 5 |
| `CandidateControllerTest` | 15 | Integration (HTTP layer) | MockMvc + Mockito |
| `CandidateRepositoryTest` | 4 | Integration (database) | @DataJpaTest + H2 |
| **Total** | **32** | | |

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CandidateServiceImplTest
mvn test -Dtest=CandidateControllerTest
mvn test -Dtest=CandidateRepositoryTest
```

### What's Tested

- ✅ Happy path for all 11 endpoints
- ✅ Validation errors (blank name, null electionId) → 400
- ✅ Resource not found → 404
- ✅ Duplicate candidate in same election → 409
- ✅ Soft-delete sets `isDeleted = true`
- ✅ Pagination response structure
- ✅ Candidate validation for voting (valid, wrong election, withdrawn)
- ✅ Bulk registration with multiple candidates
- ✅ Repository query filtering (deleted records excluded)

---

## ⚙️ Configuration

### `application.yml`

```yaml
spring:
  application:
    name: candidate-service
  datasource:
    url: ${VOTING_DB_URL}/candidateservice_db   # VOTING_DB_URL = server base URL
    username: ${VOTING_DB_USER}
    password: ${VOTING_DB_PASSWORD}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
  data:
    redis:
      host: ${REDIS_HOST:localhost}
  cache:
    type: redis                                 # candidate read caches, 10 min TTL

server:
  port: 8082

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://localhost:8761/eureka/}
```
(abridged; see `src/main/resources/application.yml`)

### Environment Variables

| Variable | Example | Required |
|----------|---------|----------|
| `VOTING_DB_URL` | `jdbc:postgresql://localhost:5432` | ✅ |
| `VOTING_DB_USER` | `postgres` | ✅ |
| `VOTING_DB_PASSWORD` | `yourpassword` | ✅ |
| `JWT_SECRET` | same Base64 secret as user-service | ✅ |
| `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `REDIS_PORT`, `EUREKA_URL` | localhost defaults | optional |

---

## 📡 Kafka Events

Published only after the database transaction commits, keyed by candidate id, as plain JSON (no Java type headers).
If the broker is down the change is still saved and the failure is logged.

| Topic | Trigger | Payload | Consumer |
|-------|---------|---------|----------|
| `candidate.created` | `POST /candidates`, `POST /candidates/bulk` | candidate response (`id, name, party, electionId, status, createdAt, updatedAt`) | Voting Service (status cache) |
| `candidate.status-changed` | `PATCH /{id}/status` | same | Voting Service |
| `candidate.deleted` | `DELETE /{id}` | same | Voting Service |

---

## 📋 Implementation Progress

| Step | Description | Status |
|------|-------------|--------|
| 0 | Fix existing code (REST conventions, DTO pattern) | ✅ Done |
| 1 | Entity & schema (Public ID pattern, audit fields) | ✅ Done |
| 2 | DTOs (7 total — request, response, update, validation, bulk) | ✅ Done |
| 3 | Exception handling (`@RestControllerAdvice`) | ✅ Done |
| 4 | Mapper utility (Entity ↔ DTO) | ✅ Done |
| 5 | Repository queries (6 derived query methods) | ✅ Done |
| 6 | Service layer — Phase 1 (Core CRUD) | ✅ Done |
| 7 | Service layer — Phase 2 (Election-scoped) | ✅ Done |
| 8 | Service layer — Phase 3 & 4 (Bulk + Status) | ✅ Done |
| 9 | Controller — all 11 endpoints wired | ✅ Done |
| 10 | Configuration (`application.yml`, env vars) | ✅ Done |
| 11 | Testing (58 tests — service, controller, repository, security, caching, events) | ✅ Done |
| 12 | Kafka integration (after-commit, keyed events) | ✅ Done |
| 13 | Redis caching | ✅ Done |
| 14 | Security (JWT + `@PreAuthorize`) | ✅ Done |

---

## 🔗 Related Services

| Service | Port | Description | Status |
|---------|------|-------------|--------|
| User Service | 8081 | Authentication, JWT, roles | ✅ Complete |
| **Candidate Service** | **8082** | **Candidate lifecycle management** | **✅ Complete** |
| Voting Service | 8083 | Vote casting, hash chaining | ✅ Complete |
| Result Service | 8084 | Real-time vote aggregation | ✅ Complete |
| API Gateway | 8080 | Routing, rate limiting | ✅ Complete |
| Eureka Server | 8761 | Service discovery | ✅ Complete |

---

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

> **Maintainer:** Vaibhav Jain  
> **Last Updated:** May 29, 2026
