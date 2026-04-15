# 🗳️ Candidate Service

> Part of the **Blockchain-Inspired Online Voting System** — a production-grade, scalable microservices platform.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📌 Overview

The **Candidate Service** manages the lifecycle of election candidates — registration, updates, status management, and validation. It is a core microservice consumed by the Voting Service (to validate candidates before accepting votes) and the Result Service (to display candidate information in results).

### Responsibilities
- ✅ Candidate CRUD operations (Standard REST)
- ✅ Standardized ApiResponse wrapper
- ✅ Global Exception Handling
- ✅ DTO-based data transfer
- ✅ Refactored to idiomatic Java conventions
- ✅ soft-delete implementation
- ✅ Bulk candidate registration
- ✅ Status management (ACTIVE / DISQUALIFIED / WITHDRAWN)
- 🔜 Election-scoped candidate queries
- 🔜 Candidate existence & validation checks (Feign)
- 🔜 Kafka event publishing

---

## 🏗️ System Architecture

```
                    ┌──────────────────┐
                    │   API Gateway    │
                    │ (Spring Cloud)   │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
    ┌─────────▼──┐  ┌───────▼────┐  ┌──────▼──────┐
    │  User      │  │ Candidate  │  │  Voting     │
    │  Service   │  │ Service    │  │  Service    │
    │  (Auth)    │  │ ◄──THIS──► │  │             │
    └────────────┘  └──────┬─────┘  └─────────────┘
                           │               │
                     ┌──────▼─────┐         │
                     │ PostgreSQL │         │
                     │ (candidate │    OpenFeign
                     │ service_db)│   (validates
                     │            │  candidates)
                     └────────────┘
```

### Cross-Service Communication
| Consumer | Protocol | Endpoints Called | Purpose |
|----------|----------|-----------------|---------|
| Voting Service | OpenFeign (sync) | `GET /{id}/exists`, `GET /{id}/validate` | Validate before accepting vote |
| Result Service | OpenFeign (sync) | `GET /election/{electionId}/active` | Display candidate info in results |
| Voting Service | Kafka (async) | Listens to `candidate.*` topics | Cache invalidation on status changes |

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Database | PostgreSQL (dedicated: `candidateservice_db`) |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| Service Discovery | Eureka Client |
| Messaging | Apache Kafka (event publishing) |
| Caching | Redis (planned) |
| Build Tool | Maven |
| Boilerplate | Lombok |

---

## 🔌 API Reference

### Base URL
```
http://localhost:8082/api/v1/candidates
```

### Endpoints

#### Phase 1 — Core CRUD

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/` | Register a new candidate | ✅ |
| `GET` | `/{id}` | Get candidate by ID | ✅ |
| `GET` | `/` | List all candidates (paginated) | ✅ |
| `PUT` | `/{id}` | Update candidate details | ✅ |
| `DELETE` | `/{id}` | Soft-delete a candidate | ✅ |

#### Phase 2 — Election-Scoped (Internal)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `GET` | `/election/{electionId}` | Get candidates for an election | 🔜 |
| `GET` | `/{id}/exists` | Check if candidate exists | 🔜 |
| `GET` | `/{id}/validate?electionId=X` | Validate candidate for election | 🔜 |

#### Phase 3 — Bulk & Search

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `POST` | `/bulk` | Register multiple candidates | 🔜 |
| `GET` | `/search?name=&party=&electionId=` | Search/filter candidates | 🔜 |

#### Phase 4 — Status Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `PATCH` | `/{id}/status` | Change candidate status | ✅ |
| `GET` | `/election/{electionId}/active` | Get active candidates only | 🔜 |

### Request/Response Examples

<details>
<summary><b>POST /api/v1/candidates</b> — Create Candidate</summary>

**Request:**
```json
{
  "name": "Jane Doe",
  "party": "Independent",
  "electionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Candidate registered successfully",
  "data": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "name": "Jane Doe",
    "party": "Independent",
    "electionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "ACTIVE",
    "createdAt": "2026-04-14T11:00:00Z",
    "updatedAt": "2026-04-14T11:00:00Z"
  },
  "timestamp": "2026-04-14T11:00:00Z"
}
```
</details>

<details>
<summary><b>GET /api/v1/candidates/{id}/validate?electionId=X</b> — Validate for Election</summary>

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "exists": true,
    "active": true,
    "electionId": "550e8400-e29b-41d4-a716-446655440000"
  }
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
  "message": "Candidate status updated to DISQUALIFIED",
  "data": {
    "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "name": "Jane Doe",
    "party": "Independent",
    "electionId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "DISQUALIFIED",
    "createdAt": "2026-04-14T11:00:00Z",
    "updatedAt": "2026-04-14T12:30:00Z"
  }
}
```
</details>

<details>
<summary><b>GET /api/v1/candidates?page=0&size=20&sort=name,asc</b> — Paginated List</summary>

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "...",
        "name": "Jane Doe",
        "party": "Independent",
        "electionId": "...",
        "status": "ACTIVE",
        "createdAt": "...",
        "updatedAt": "..."
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 45,
    "totalPages": 3
  }
}
```
</details>

---

## 🗄️ Database Schema

**Database:** `candidate_service_db` (isolated — microservices own their data)

```sql
CREATE TABLE candidates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    party           VARCHAR(255),
    election_id     UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    
    CONSTRAINT uq_candidate_election UNIQUE (name, election_id)
);

CREATE INDEX idx_candidates_election_id ON candidates(election_id);
CREATE INDEX idx_candidates_status ON candidates(status);
CREATE INDEX idx_candidates_election_status ON candidates(election_id, status);
```

**Status Values:** `ACTIVE` | `DISQUALIFIED` | `WITHDRAWN`

---

## 📦 Project Structure

```
candidate-service/
├── src/
│   ├── main/
│   │   ├── java/com/voting/candidateservice/
│   │   │   ├── CandidateServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── CandidateController.java
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── BulkCandidateRequestDTO.java
│   │   │   │   ├── CandidateRequestDTO.java
│   │   │   │   ├── CandidateResponseDTO.java
│   │   │   │   ├── CandidateStatusUpdateDTO.java
│   │   │   │   ├── CandidateUpdateDTO.java
│   │   │   │   └── CandidateValidationDTO.java
│   │   │   ├── exception/
│   │   │   │   ├── DuplicateResourceException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── mapper/
│   │   │   │   └── CandidateMapper.java
│   │   │   ├── model/
│   │   │   │   ├── Candidate.java
│   │   │   │   └── enums/
│   │   │   │       └── CandidateStatus.java
│   │   │   ├── repository/
│   │   │   │   └── CandidateRepository.java
│   │   │   └── service/
│   │   │       ├── CandidateService.java (Interface)
│   │   │       └── CandidateServiceImpl.java (Implementation)
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/voting/candidate_service/
│           ├── controller/
│           ├── service/
│           └── repository/
├── IMPLEMENTATION_PLAN.md
├── README.md
├── pom.xml
└── .gitignore
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+
- Eureka Server running (for service discovery)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/candidate-service.git
   cd candidate-service
   ```

2. **Create PostgreSQL database**
   ```sql
   CREATE DATABASE candidate_service_db;
   ```

3. **Configure database connection**  
   Update `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/candidate_service_db
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

4. **Build the project**
   ```bash
   ./mvnw clean install
   ```

5. **Run the service**
   ```bash
   ./mvnw spring-boot:run
   ```

6. **Verify**
   ```bash
   curl http://localhost:8082/actuator/health
   ```

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CandidateServiceTest

# Run integration tests
mvn verify
```

---

## 📡 Kafka Events Published

| Topic | Trigger | Payload |
|-------|---------|---------|
| `candidate.created` | New candidate registered | `{candidateId, name, party, electionId}` |
| `candidate.status-changed` | Status updated | `{candidateId, oldStatus, newStatus, electionId}` |
| `candidate.deleted` | Candidate soft-deleted | `{candidateId, electionId}` |

---

## 🔗 Related Services

| Service | Repository | Description |
|---------|-----------|-------------|
| User Service | [user-service](https://github.com/<your-username>/user-service) | Authentication, JWT, roles |
| Voting Service | [voting-service](https://github.com/<your-username>/voting-service) | Vote casting, hash chaining |
| Result Service | [result-service](https://github.com/<your-username>/result-service) | Dynamic vote aggregation |
| API Gateway | [api-gateway](https://github.com/<your-username>/api-gateway) | Routing, rate limiting |
| Eureka Server | [eureka-server](https://github.com/<your-username>/eureka-server) | Service discovery |

---

## 📋 Implementation Progress

> Track detailed progress in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md)

| Phase | Description | Status |
|-------|-------------|--------|
| Step 0 | Fix existing code issues | ✅ Done |
| Step 1 | Entity & schema enhancement | ✅ Done |
| Step 2 | DTO Expansion | 🔜 In Progress |
| Step 3 | Exception handling (Global) | 🔜 In Progress |
| Step 4 | Mapper utility | 🔜 Planned |
| Step 5 | Repository queries (Pagination) | ✅ Done |
| Step 6 | Core CRUD APIs | ✅ Done |
| Step 7 | Election-scoped APIs | 🔜 Planned |
| Step 8 | Bulk + status APIs | 🔜 Planned |
| Step 9 | Controller wiring | ✅ Done |
| Step 10 | Configuration | 🔜 Planned |
| Step 11 | Testing & Build Verification | ✅ Done |
| Step 12 | Kafka integration | 🔜 Planned |
| Step 13 | Redis caching | 🔜 Planned |
| Step 14 | Security (JWT) | 🔜 Planned |

---

## 📝 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

> **Maintainer:** Vaibhav  
> **Last Updated:** April 15, 2026
