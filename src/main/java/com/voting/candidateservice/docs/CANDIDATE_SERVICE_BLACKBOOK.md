# 📖 Candidate Service — The Complete Black Book

> **Service:** `candidate-service`  
> **Version:** `0.0.1-SNAPSHOT`  
> **Author:** Vaibhav  
> **Tech Stack:** Java 17 · Spring Boot 3.3.6 · PostgreSQL · Spring Cloud 2023.0.3  
> **Port:** `8082`  
> **Database:** `candidateservice_db` (PostgreSQL)  
> **Last Updated:** April 21, 2026

---

## 📑 Table of Contents

1. [Service Overview](#1-service-overview)
2. [Architecture & Package Structure](#2-architecture--package-structure)
3. [Tech Stack & Dependencies](#3-tech-stack--dependencies)
4. [Data Model Layer](#4-data-model-layer)
5. [DTO Layer — Data Transfer Objects](#5-dto-layer--data-transfer-objects)
6. [Repository Layer — Data Access](#6-repository-layer--data-access)
7. [Service Layer — Business Logic](#7-service-layer--business-logic)
8. [Controller Layer — REST API](#8-controller-layer--rest-api)
9. [Exception Handling Strategy](#9-exception-handling-strategy)
10. [Mapper Layer — Entity ↔ DTO Conversion](#10-mapper-layer--entity--dto-conversion)
11. [Configuration & Infrastructure](#11-configuration--infrastructure)
12. [Complete API Reference](#12-complete-api-reference)
13. [Request/Response Examples](#13-requestresponse-examples)
14. [Testing Strategy](#14-testing-strategy)
15. [Design Decisions & Trade-offs](#15-design-decisions--trade-offs)
16. [Cross-Service Contracts](#16-cross-service-contracts)
17. [Data Flow Diagrams](#17-data-flow-diagrams)
18. [Future Roadmap](#18-future-roadmap)
19. [Quick Reference Cheat Sheet](#19-quick-reference-cheat-sheet)

---

## 1. Service Overview

### What It Does
The Candidate Service is the **candidate lifecycle management** microservice in the Blockchain-Inspired Online Voting System. It is responsible for:

- **Registering** candidates for elections
- **Querying** candidates by ID, election, status
- **Updating** candidate details and status (ACTIVE → WITHDRAWN → DISQUALIFIED)
- **Soft-deleting** candidates while preserving audit trails
- **Validating** candidates for the Voting Service (inter-service communication)
- **Bulk operations** for admin efficiency

### Where It Sits in the System

```
                    ┌──────────────┐
                    │  API Gateway │
                    │  (Port 8080) │
                    └──────┬───────┘
                           │
          ┌────────────────┼────────────────┐
          │                │                │
    ┌─────▼─────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │   User    │  │ ★CANDIDATE★ │  │   Voting   │
    │  Service  │  │   SERVICE   │  │   Service  │
    │  (8081)   │  │   (8082)    │  │   (8083)   │
    └───────────┘  └──────┬──────┘  └────────────┘
                          │
                   ┌──────▼──────┐
                   │ PostgreSQL  │
                   │ candidate   │
                   │ service_db  │
                   └─────────────┘
```

### Who Depends on Candidate Service?

| Consumer | What It Calls | Why |
|----------|--------------|-----|
| **Voting Service** | `GET /{id}/exists` | Check if candidate exists before accepting a vote |
| **Voting Service** | `GET /{id}/validate?electionId=X` | Validate candidate belongs to election and is ACTIVE |
| **Result Service** | `GET /election/{electionId}` | Fetch candidate names/parties for result display |
| **Result Service** | `GET /election/{electionId}/active` | Show only active candidates in results |
| **Admin UI** | All CRUD endpoints | Manage candidate registrations |

---

## 2. Architecture & Package Structure

### Layered Architecture Pattern

```
    HTTP Request
        │
        ▼
┌─────────────────────┐
│    Controller        │  ← REST endpoints, input validation, HTTP status codes
│    (Thin layer)      │  ← Delegates everything to Service
└─────────┬───────────┘
          │  CandidateRequestDTO / CandidateUpdateDTO
          ▼
┌─────────────────────┐
│    Service           │  ← Business logic, duplicate checks, edge cases
│    (Interface + Impl)│  ← Uses Mapper for DTO ↔ Entity conversion
└─────────┬───────────┘
          │  Candidate entity
          ▼
┌─────────────────────┐
│    Repository        │  ← Spring Data JPA, derived queries
│    (JPA Interface)   │  ← Talks to PostgreSQL
└─────────┬───────────┘
          │  SQL
          ▼
┌─────────────────────┐
│    PostgreSQL        │
│    candidates table  │
└─────────────────────┘
```

### Package Structure

```
src/main/java/com/voting/candidateservice/
├── CandidateServiceApplication.java         ← Spring Boot entry point
│
├── config/
│   └── DataSeeder.java                      ← Seeds sample data on first boot
│
├── controller/
│   └── CandidateController.java             ← 11 REST endpoints
│
├── dto/
│   ├── ApiResponse.java                     ← Generic response wrapper
│   ├── BulkCandidateRequestDTO.java         ← Bulk registration request
│   ├── CandidateRequestDTO.java             ← Create request
│   ├── CandidateResponseDTO.java            ← Response payload
│   ├── CandidateStatusUpdateDTO.java        ← Status change request
│   ├── CandidateUpdateDTO.java              ← Update request
│   └── CandidateValidationDTO.java          ← Feign validation response
│
├── exception/
│   ├── DuplicateResourceException.java      ← 409 Conflict
│   ├── GlobalExceptionHandler.java          ← @RestControllerAdvice
│   └── ResourceNotFoundException.java       ← 404 Not Found
│
├── mapper/
│   └── CandidateMapper.java                 ← Entity ↔ DTO conversion
│
├── model/
│   ├── Candidate.java                       ← JPA Entity
│   └── enums/
│       └── CandidateStatus.java             ← ACTIVE, DISQUALIFIED, WITHDRAWN
│
├── repository/
│   └── CandidateRepository.java             ← JPA Repository
│
└── service/
    ├── CandidateService.java                ← Interface (contract)
    └── CandidateServiceImpl.java            ← Implementation (business logic)
```

### File Count Summary

| Package | Files | Role |
|---------|-------|------|
| `config/` | 1 | Bootstrap configuration |
| `controller/` | 1 | REST API layer |
| `dto/` | 7 | Data transfer objects |
| `exception/` | 3 | Error handling |
| `mapper/` | 1 | Object conversion |
| `model/` | 2 | JPA entity + enum |
| `repository/` | 1 | Data access |
| `service/` | 2 | Business logic |
| **Total** | **19 source files** | |

---

## 3. Tech Stack & Dependencies

### Maven Dependencies (`pom.xml`)

| Dependency | GroupId | Purpose |
|-----------|---------|---------|
| `spring-boot-starter-web` | `org.springframework.boot` | REST controllers, embedded Tomcat |
| `spring-boot-starter-actuator` | `org.springframework.boot` | Health checks, metrics (`/actuator/health`) |
| `spring-boot-starter-validation` | `org.springframework.boot` | Bean Validation (`@Valid`, `@NotBlank`, `@NotNull`) |
| `spring-boot-starter-data-jpa` | `org.springframework.boot` | Hibernate ORM, Spring Data repositories |
| `postgresql` | `org.postgresql` | PostgreSQL JDBC driver (runtime scope) |
| `h2` | `com.h2database` | In-memory DB for tests (test scope) |
| `spring-cloud-starter-netflix-eureka-client` | `org.springframework.cloud` | Service discovery client (currently disabled) |
| `lombok` | `org.projectlombok` | Boilerplate elimination (`@Data`, `@Builder`, etc.) |
| `springdoc-openapi-starter-webmvc-ui` | `org.springdoc` | Swagger UI at `/swagger-ui.html` |
| `spring-boot-starter-test` | `org.springframework.boot` | JUnit 5, Mockito, MockMvc (test scope) |

### Version Matrix

| Component | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.3.6 |
| Spring Cloud | 2023.0.3 |
| SpringDoc OpenAPI | 2.5.0 |
| Lombok | Managed by Spring Boot BOM |

---

## 4. Data Model Layer

### Entity: `Candidate.java`

**Location:** `model/Candidate.java`

```java
@Entity
@Table(name = "candidates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // Internal PK — NEVER exposed in API

    @Column(name = "external_id", unique = true, nullable = false, updatable = false)
    private UUID externalId;            // Public ID — used in all API interactions

    @Column(nullable = false)
    private String name;

    @Column(name = "party")
    private String party;               // Optional field

    @Column(name = "election_id", nullable = false)
    private UUID electionId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private CandidateStatus status = CandidateStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void ensureExternalId() {
        if (externalId == null) {
            this.externalId = UUID.randomUUID();
        }
    }
}
```

### Public ID Pattern — Why Two IDs?

This is a **critical design decision** used at FAANG companies:

| Field | Type | Purpose | Exposed in API? |
|-------|------|---------|-----------------|
| `id` | `Long` (auto-increment) | **Internal** primary key | ❌ Never |
| `externalId` | `UUID` (random) | **Public** identifier | ✅ Always |

**Why?**
1. **Performance:** `Long` auto-increment is ~10x faster for JPA joins, indexes, and foreign keys than UUID
2. **Security:** Sequential `Long` IDs are guessable (`/candidates/1`, `/candidates/2`). UUIDs are unpredictable
3. **Scalability:** UUID generation is distributed — no central sequence needed across microservice instances
4. **The `@PrePersist` hook** ensures a UUID is always assigned before the first save

### Database Schema (Auto-generated by Hibernate)

```sql
CREATE TABLE candidates (
    id              BIGSERIAL PRIMARY KEY,              -- Internal auto-increment
    external_id     UUID UNIQUE NOT NULL,               -- Public UUID
    name            VARCHAR(255) NOT NULL,
    party           VARCHAR(255),
    election_id     UUID NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP NOT NULL
);
```

### Enum: `CandidateStatus`

**Location:** `model/enums/CandidateStatus.java`

```java
public enum CandidateStatus {
    ACTIVE,         // Candidate is eligible for votes
    DISQUALIFIED,   // Candidate removed by admin (votes may exist)
    WITHDRAWN       // Candidate withdrew voluntarily
}
```

**State Transitions:**
```
    ┌──────────┐
    │  ACTIVE  │ ← Default on creation
    └────┬─────┘
         │
    ┌────┴────────────┐
    │                 │
    ▼                 ▼
┌──────────┐   ┌───────────┐
│WITHDRAWN │   │DISQUALIFIED│
└──────────┘   └───────────┘
```

- `ACTIVE → WITHDRAWN`: Candidate voluntarily steps down
- `ACTIVE → DISQUALIFIED`: Admin removes candidate (election rules violation)
- **One-way transitions** — once withdrawn/disqualified, cannot go back to ACTIVE (enforced at business level in future)

---

## 5. DTO Layer — Data Transfer Objects

### Why DTOs?
> **Rule:** Never expose JPA entities directly in API responses. DTOs decouple your database schema from your API contract.

The service uses **7 DTOs** for different operations:

### 5.1 `CandidateRequestDTO` — Creating a Candidate

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CandidateRequestDTO {

    @NotBlank(message = "Candidate name cannot be blank")
    private String name;

    private String party;    // Optional — independent candidates exist

    @NotNull(message = "Election ID must be provided")
    private UUID electionId;
}
```

**Validation Rules:**
| Field | Rule | Error Message |
|-------|------|---------------|
| `name` | `@NotBlank` (not null, not empty, not whitespace) | "Candidate name cannot be blank" |
| `party` | None (optional) | — |
| `electionId` | `@NotNull` | "Election ID must be provided" |

### 5.2 `CandidateResponseDTO` — API Response Payload

```java
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CandidateResponseDTO {
    private UUID id;                    // ← This is the externalId, NOT the internal Long
    private String name;
    private String party;
    private UUID electionId;
    private CandidateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

> **Key insight:** The `id` field in the response maps to `Candidate.externalId`, NOT `Candidate.id`. The internal Long ID is never exposed.

### 5.3 `CandidateUpdateDTO` — Updating a Candidate

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class CandidateUpdateDTO {
    @NotBlank(message = "Candidate name cannot be blank")
    private String name;

    @NotBlank(message = "Party name cannot be blank")
    private String party;
}
```

> **Note:** `electionId` is NOT updatable — a candidate cannot switch elections. Only `name` and `party` can be changed.

### 5.4 `CandidateStatusUpdateDTO` — Changing Status

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class CandidateStatusUpdateDTO {
    @NotNull(message = "Candidate status cannot be null")
    private CandidateStatus status;
}
```

### 5.5 `CandidateValidationDTO` — Inter-Service Validation Response

Used by the **Voting Service** via OpenFeign to validate a candidate before accepting a vote:

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class CandidateValidationDTO {
    private UUID candidateId;
    private UUID electionId;
    private boolean isValid;              // Can votes be cast for this candidate?
    private CandidateStatus currentStatus;
    private String message;               // Human-readable reason if invalid
}
```

**Example responses:**
| Scenario | `isValid` | `message` |
|----------|-----------|-----------|
| Active candidate, correct election | `true` | "Valid" |
| Candidate not found | `false` | "Candidate not found" |
| Wrong election | `false` | "Candidate does not belong to this election" |
| Withdrawn candidate | `false` | "Candidate is WITHDRAWN" |
| Disqualified candidate | `false` | "Candidate is DISQUALIFIED" |

### 5.6 `BulkCandidateRequestDTO` — Bulk Registration

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class BulkCandidateRequestDTO {
    @NotEmpty(message = "Candidate list cannot be empty")
    @Valid    // ← Cascading validation — validates each CandidateRequestDTO inside
    private List<CandidateRequestDTO> candidates;
}
```

### 5.7 `ApiResponse<T>` — Generic Response Wrapper

**Every API endpoint** wraps its response in this structure:

```java
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static <T> ApiResponse<T> success(T data, String message) { ... }
    public static <T> ApiResponse<T> error(String message) { ... }
}
```

**Success response shape:**
```json
{
    "success": true,
    "message": "Candidate created successfully",
    "data": { ... },
    "timestamp": "2026-04-21T10:30:00"
}
```

**Error response shape:**
```json
{
    "success": false,
    "message": "Candidate not found with id: abc-123",
    "data": null,
    "timestamp": "2026-04-21T10:30:00"
}
```

---

## 6. Repository Layer — Data Access

### `CandidateRepository.java`

```java
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Find all candidates for a specific election (excluding soft-deleted)
    List<Candidate> findByElectionIdAndIsDeletedFalse(UUID electionId);

    // Find only active candidates (by election + status)
    List<Candidate> findByElectionIdAndStatusAndIsDeletedFalse(UUID electionId, CandidateStatus status);

    // Paginated list of all non-deleted candidates
    Page<Candidate> findAllByIsDeletedFalse(Pageable pageable);

    // Check existence by public UUID
    boolean existsByExternalIdAndIsDeletedFalse(UUID externalId);

    // Find by public UUID
    Optional<Candidate> findByExternalIdAndIsDeletedFalse(UUID externalId);

    // Duplicate check: same name in same election
    boolean existsByNameAndElectionIdAndIsDeletedFalse(String name, UUID electionId);
}
```

### Query Derivation — How Spring Data Generates SQL

| Method Name | Generated SQL (simplified) |
|-------------|---------------------------|
| `findByExternalIdAndIsDeletedFalse(UUID)` | `SELECT * FROM candidates WHERE external_id = ? AND is_deleted = false` |
| `findByElectionIdAndIsDeletedFalse(UUID)` | `SELECT * FROM candidates WHERE election_id = ? AND is_deleted = false` |
| `findByElectionIdAndStatusAndIsDeletedFalse(UUID, CandidateStatus)` | `SELECT * FROM candidates WHERE election_id = ? AND status = ? AND is_deleted = false` |
| `existsByNameAndElectionIdAndIsDeletedFalse(String, UUID)` | `SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM candidates WHERE name = ? AND election_id = ? AND is_deleted = false` |
| `findAllByIsDeletedFalse(Pageable)` | `SELECT * FROM candidates WHERE is_deleted = false LIMIT ? OFFSET ?` |

### Key Design Decisions

1. **Every query includes `IsDeletedFalse`** — Soft-deleted records are invisible by default
2. **Primary key is `Long`** — JpaRepository types to `Long`, not `UUID`. All public-facing lookups use `externalId`
3. **No `@Query` annotations** — All queries are derived from method naming conventions. This keeps things clean for the current complexity level

---

## 7. Service Layer — Business Logic

### Interface: `CandidateService.java`

Defines the **contract** that any implementation must fulfill:

```java
public interface CandidateService {
    CandidateResponseDTO createCandidate(CandidateRequestDTO requestDTO);
    CandidateResponseDTO getCandidateById(UUID id);
    Page<CandidateResponseDTO> getAllCandidates(Pageable pageable);
    CandidateResponseDTO updateCandidate(UUID id, CandidateUpdateDTO updateDTO);
    CandidateResponseDTO updateCandidateStatus(UUID id, CandidateStatusUpdateDTO statusUpdateDTO);
    List<CandidateResponseDTO> bulkRegisterCandidates(BulkCandidateRequestDTO bulkRequestDTO);
    List<CandidateResponseDTO> getCandidatesByElection(UUID electionId);
    List<CandidateResponseDTO> getActiveCandidatesByElection(UUID electionId);
    boolean candidateExists(UUID id);
    CandidateValidationDTO validateCandidateForElection(UUID candidateId, UUID electionId);
    void deleteCandidate(UUID id);
}
```

### Implementation: `CandidateServiceImpl.java`

#### Constructor Injection (via Lombok)

```java
@Service
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {
    private final CandidateRepository candidateRepository;
    private final CandidateMapper candidateMapper;
    ...
}
```

> `@RequiredArgsConstructor` generates a constructor with `final` fields — this is the **recommended** way to do dependency injection in Spring (over `@Autowired` field injection).

#### Method-by-Method Breakdown

##### 7.1 `createCandidate(CandidateRequestDTO)` — Register a Candidate

```
Flow:
1. Check: Does a candidate with this name already exist in this election?
   → YES: throw DuplicateResourceException (409 Conflict)
   → NO: continue
2. Convert DTO → Entity (via CandidateMapper)
3. Save to database
4. Convert Entity → ResponseDTO
5. Return
```

**Key behaviors:**
- `@Transactional` — rolls back on any exception
- Duplicate check uses `existsByNameAndElectionIdAndIsDeletedFalse()` — a candidate can use the same name in a *different* election
- The `@PrePersist` hook on the entity generates the `externalId` UUID

##### 7.2 `getCandidateById(UUID)` — Fetch Single Candidate

```
Flow:
1. Look up by externalId where is_deleted = false
   → NOT FOUND: throw ResourceNotFoundException (404)
   → FOUND: convert to ResponseDTO and return
```

##### 7.3 `getAllCandidates(Pageable)` — Paginated List

```
Flow:
1. Query all non-deleted candidates with pagination
2. Map each entity to ResponseDTO using .map()
3. Return Page<CandidateResponseDTO>
```

**Pagination defaults:** `page=0, size=20` (Spring Data defaults)

##### 7.4 `updateCandidate(UUID, CandidateUpdateDTO)` — Update Details

```
Flow:
1. Find candidate by externalId (or throw 404)
2. Set new name and party from DTO
3. Save (updatedAt auto-updates via @UpdateTimestamp)
4. Return updated ResponseDTO
```

> **Note:** `electionId` and `status` are NOT modifiable through this endpoint.

##### 7.5 `updateCandidateStatus(UUID, CandidateStatusUpdateDTO)` — Change Status

```
Flow:
1. Find candidate by externalId (or throw 404)
2. Set new status from DTO
3. Save
4. Return updated ResponseDTO
```

> **Future improvement:** Add state transition validation (e.g., cannot go from WITHDRAWN back to ACTIVE).

##### 7.6 `bulkRegisterCandidates(BulkCandidateRequestDTO)` — Bulk Create

```
Flow:
1. Stream over each CandidateRequestDTO in the list
2. Call createCandidate() for each one individually
3. Collect results into a list
4. Return all created candidates
```

**Important trade-off:** This processes candidates **sequentially**, not in batch. Each one gets its own duplicate check. If candidate #3 out of 5 is a duplicate, it throws an exception and the entire transaction rolls back (because of `@Transactional`).

##### 7.7 `deleteCandidate(UUID)` — Soft Delete

```
Flow:
1. Find candidate by externalId (or throw 404)
2. Set isDeleted = true
3. Save
```

> The record stays in the database. All queries with `IsDeletedFalse` will exclude it.

##### 7.8 `getCandidatesByElection(UUID)` — Election Scope

Returns all non-deleted candidates for a given election (regardless of status).

##### 7.9 `getActiveCandidatesByElection(UUID)` — Active Only

Returns only `ACTIVE` candidates for a given election. Used by the Result Service to display valid candidates.

##### 7.10 `candidateExists(UUID)` — Lightweight Check

Returns a simple `boolean`. Used by Voting Service via OpenFeign for fast existence verification.

##### 7.11 `validateCandidateForElection(UUID, UUID)` — Full Validation

The most complex method. Performs **3-layer validation**:

```
Layer 1: Does the candidate exist?
  → NO: return { isValid: false, message: "Candidate not found" }

Layer 2: Does the candidate belong to THIS election?
  → NO: return { isValid: false, message: "Candidate does not belong to this election" }

Layer 3: Is the candidate ACTIVE?
  → NO: return { isValid: false, message: "Candidate is WITHDRAWN/DISQUALIFIED" }
  → YES: return { isValid: true, message: "Valid" }
```

> **Design choice:** This method does NOT throw exceptions — it returns a structured validation result. This is deliberate because the Voting Service needs to handle invalid candidates gracefully (show user a clear error), not deal with opaque HTTP error codes.

#### Helper Method

```java
private Candidate findCandidateByExternalId(UUID externalId) {
    return candidateRepository.findByExternalIdAndIsDeletedFalse(externalId)
            .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + externalId));
}
```

This **DRY helper** is reused by `getCandidateById`, `updateCandidate`, `updateCandidateStatus`, and `deleteCandidate`.

---

## 8. Controller Layer — REST API

### `CandidateController.java`

```java
@RestController
@RequestMapping("/api/v1/candidates")
@RequiredArgsConstructor
public class CandidateController { ... }
```

### Design Principles Applied

| Principle | How It's Implemented |
|-----------|---------------------|
| **Thin controller** | Controller only delegates to service. Zero business logic. |
| **Consistent response wrapper** | Every endpoint returns `ApiResponse<T>` |
| **Proper HTTP status codes** | `201 CREATED`, `200 OK`, `400 BAD REQUEST`, `404 NOT FOUND`, `409 CONFLICT` |
| **Input validation** | `@Valid` on every `@RequestBody` |
| **RESTful URL design** | Plural nouns, no verbs, hierarchical resources |

### Endpoint Summary

| # | Method | Path | Description | Status Code |
|---|--------|------|-------------|-------------|
| 1 | `POST` | `/api/v1/candidates` | Register candidate | 201 |
| 2 | `POST` | `/api/v1/candidates/bulk` | Bulk register | 201 |
| 3 | `GET` | `/api/v1/candidates/{id}` | Get by ID | 200 |
| 4 | `GET` | `/api/v1/candidates` | List all (paginated) | 200 |
| 5 | `PUT` | `/api/v1/candidates/{id}` | Update details | 200 |
| 6 | `PATCH` | `/api/v1/candidates/{id}/status` | Update status | 200 |
| 7 | `DELETE` | `/api/v1/candidates/{id}` | Soft-delete | 200 |
| 8 | `GET` | `/api/v1/candidates/election/{electionId}` | By election | 200 |
| 9 | `GET` | `/api/v1/candidates/election/{electionId}/active` | Active by election | 200 |
| 10 | `GET` | `/api/v1/candidates/{id}/exists` | Existence check | 200 |
| 11 | `GET` | `/api/v1/candidates/{id}/validate?electionId=X` | Full validation | 200 |

---

## 9. Exception Handling Strategy

### `GlobalExceptionHandler.java` — Centralized Error Handling

```
┌──────────────────────────────────────────────────┐
│             GlobalExceptionHandler               │
│            (@RestControllerAdvice)                │
├──────────────────────────────────────────────────┤
│                                                  │
│  ResourceNotFoundException   → 404 NOT FOUND     │
│  DuplicateResourceException  → 409 CONFLICT      │
│  MethodArgumentNotValid      → 400 BAD REQUEST   │
│  Exception (catch-all)       → 500 INTERNAL ERR  │
│                                                  │
└──────────────────────────────────────────────────┘
```

### Exception Details

#### `ResourceNotFoundException` (404)
- **When thrown:** Candidate not found by `externalId`, or already soft-deleted
- **Response body:**
```json
{
    "success": false,
    "message": "Candidate not found with id: abc-123-def",
    "data": null,
    "timestamp": "2026-04-21T10:30:00"
}
```

#### `DuplicateResourceException` (409)
- **When thrown:** Attempting to register a candidate with a name that already exists in the same election
- **Response body:**
```json
{
    "success": false,
    "message": "Candidate with name Rahul Sharma already registered for this election",
    "data": null,
    "timestamp": "2026-04-21T10:30:00"
}
```

#### `MethodArgumentNotValidException` (400)
- **When thrown:** Jakarta Bean Validation fails (`@NotBlank`, `@NotNull`, etc.)
- **Response body:** Includes a map of field-level errors
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

#### General `Exception` (500) — Safety Net
- Catches anything unexpected (DB connection failures, NPEs, etc.)
- **Response body:**
```json
{
    "success": false,
    "message": "An unexpected error occurred: Connection refused",
    "data": null,
    "timestamp": "2026-04-21T10:30:00"
}
```

### Custom Exception Classes

Both custom exceptions:
- Extend `RuntimeException` (unchecked — no need to declare in method signatures)
- Annotated with `@ResponseStatus` as a fallback (though `GlobalExceptionHandler` takes priority)
- Accept a single `String message` constructor parameter

---

## 10. Mapper Layer — Entity ↔ DTO Conversion

### `CandidateMapper.java`

```java
@Component
public class CandidateMapper {

    public Candidate toEntity(CandidateRequestDTO requestDTO) {
        return Candidate.builder()
                .name(requestDTO.getName())
                .party(requestDTO.getParty())
                .electionId(requestDTO.getElectionId())
                .build();
        // Note: externalId, status, isDeleted, timestamps are
        // all set automatically by defaults and @PrePersist
    }

    public CandidateResponseDTO toResponseDTO(Candidate candidate) {
        return CandidateResponseDTO.builder()
                .id(candidate.getExternalId())       // ← Maps externalId → id
                .name(candidate.getName())
                .party(candidate.getParty())
                .electionId(candidate.getElectionId())
                .status(candidate.getStatus())
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .build();
    }
}
```

### Why Manual Mapper (Not MapStruct)?

| Approach | Pros | Cons |
|----------|------|------|
| **Manual (current)** | No annotation processor setup, full control, easy to debug | More boilerplate as DTOs grow |
| **MapStruct** | Zero boilerplate, compile-time safe | Complex setup, harder to debug, overkill for 2 methods |

**Decision:** Manual mapper for now. Switch to MapStruct if the DTO count exceeds 10+.

### Field Mapping Table

| Entity Field | DTO Field | Direction |
|-------------|-----------|-----------|
| `externalId` | `id` | Entity → Response |
| `name` | `name` | Both |
| `party` | `party` | Both |
| `electionId` | `electionId` | Both |
| `status` | `status` | Entity → Response |
| `createdAt` | `createdAt` | Entity → Response |
| `updatedAt` | `updatedAt` | Entity → Response |
| `id` (Long) | — | **Never mapped** |
| `isDeleted` | — | **Never mapped** |

---

## 11. Configuration & Infrastructure

### `application.yml`

```yaml
spring:
  application:
    name: candidate-service             # Registered name in Eureka

  datasource:
    url: ${VOTING_DB_URL}               # e.g., jdbc:postgresql://localhost:5432/candidateservice_db
    username: ${VOTING_DB_USER}         # e.g., postgres
    password: ${VOTING_DB_PASSWORD}     # e.g., your_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: update                  # Auto-create/modify tables on startup
    show-sql: true                      # Log all SQL to console
    properties:
      hibernate:
        format_sql: true                # Pretty-print SQL
        dialect: org.hibernate.dialect.PostgreSQLDialect

server:
  port: 8082                            # Fixed port for this service

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    enabled: false                      # Enable after Eureka Server is deployed
```

### Environment Variables Required

| Variable | Example Value | Required |
|----------|-------------|----------|
| `VOTING_DB_URL` | `jdbc:postgresql://localhost:5432/candidateservice_db` | ✅ Yes |
| `VOTING_DB_USER` | `postgres` | ✅ Yes |
| `VOTING_DB_PASSWORD` | `your_password` | ✅ Yes |

### `DataSeeder.java` — Sample Data Bootstrap

Runs on first startup **only if the database is empty** (`repository.count() == 0`).

Seeds **10 candidates** across **2 elections**:

| Election | ID Pattern | Candidates |
|----------|-----------|------------|
| Presidential | `11111111-1111-...` | 5 (3 ACTIVE, 1 WITHDRAWN, 1 DISQUALIFIED) |
| Local | `22222222-2222-...` | 5 (4 ACTIVE, 1 WITHDRAWN) |

> ⚠️ **Production note:** Remove or disable `DataSeeder` before deploying to production. Use Flyway or Liquibase for production data migrations.

---

## 12. Complete API Reference

### Base URL: `http://localhost:8082/api/v1/candidates`

---

### `POST /api/v1/candidates` — Register Candidate

**Auth:** ADMIN (future)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `name` | Body | String | ✅ | Candidate's full name |
| `party` | Body | String | ❌ | Political party (optional) |
| `electionId` | Body | UUID | ✅ | Election this candidate belongs to |

**Success:** `201 Created`  
**Errors:** `400` (validation), `409` (duplicate name in same election)

---

### `POST /api/v1/candidates/bulk` — Bulk Register

**Auth:** ADMIN (future)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `candidates` | Body | `List<CandidateRequestDTO>` | ✅ | Array of candidates |

**Success:** `201 Created`  
**Errors:** `400` (validation, empty list), `409` (any duplicate)

---

### `GET /api/v1/candidates/{id}` — Get by ID

**Auth:** PUBLIC

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |

**Success:** `200 OK`  
**Errors:** `404` (not found or deleted)

---

### `GET /api/v1/candidates` — List All (Paginated)

**Auth:** PUBLIC

| Parameter | Location | Type | Required | Default | Description |
|-----------|----------|------|----------|---------|-------------|
| `page` | Query | int | ❌ | `0` | Page number (0-indexed) |
| `size` | Query | int | ❌ | `20` | Page size |
| `sort` | Query | String | ❌ | — | Sort field (e.g., `name,asc`) |

**Success:** `200 OK`  
**Response includes:** `content`, `totalElements`, `totalPages`, `size`, `number`

---

### `PUT /api/v1/candidates/{id}` — Update Details

**Auth:** ADMIN (future)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |
| `name` | Body | String | ✅ | New name |
| `party` | Body | String | ✅ | New party |

**Success:** `200 OK`  
**Errors:** `400` (validation), `404` (not found)

---

### `PATCH /api/v1/candidates/{id}/status` — Update Status

**Auth:** ADMIN (future)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |
| `status` | Body | Enum | ✅ | `ACTIVE`, `WITHDRAWN`, or `DISQUALIFIED` |

**Success:** `200 OK`  
**Errors:** `400` (validation, invalid enum), `404` (not found)

---

### `DELETE /api/v1/candidates/{id}` — Soft Delete

**Auth:** ADMIN (future)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |

**Success:** `200 OK` (data will be `null`)  
**Errors:** `404` (not found)

---

### `GET /api/v1/candidates/election/{electionId}` — By Election

**Auth:** PUBLIC

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `electionId` | Path | UUID | ✅ | Election identifier |

**Success:** `200 OK` — Returns array of all candidates (any status)

---

### `GET /api/v1/candidates/election/{electionId}/active` — Active Only

**Auth:** PUBLIC

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `electionId` | Path | UUID | ✅ | Election identifier |

**Success:** `200 OK` — Returns array of ACTIVE candidates only

---

### `GET /api/v1/candidates/{id}/exists` — Existence Check

**Auth:** INTERNAL (Feign)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |

**Success:** `200 OK` — `data: true` or `data: false`

---

### `GET /api/v1/candidates/{id}/validate` — Full Validation

**Auth:** INTERNAL (Feign)

| Parameter | Location | Type | Required | Description |
|-----------|----------|------|----------|-------------|
| `id` | Path | UUID | ✅ | Candidate's public UUID |
| `electionId` | Query | UUID | ✅ | Election to validate against |

**Success:** `200 OK` — Returns `CandidateValidationDTO`

---

## 13. Request/Response Examples

### Create Candidate

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Rahul Sharma",
    "party": "Swaraj Party",
    "electionId": "11111111-1111-1111-1111-111111111111"
  }'
```

**Response (201):**
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

### Bulk Register

**Request:**
```bash
curl -X POST http://localhost:8082/api/v1/candidates/bulk \
  -H "Content-Type: application/json" \
  -d '{
    "candidates": [
        { "name": "Amit Verma", "party": "Green Party", "electionId": "11111111-1111-1111-1111-111111111111" },
        { "name": "Priya Patel", "party": "Independent", "electionId": "11111111-1111-1111-1111-111111111111" }
    ]
  }'
```

### Get Paginated List

**Request:**
```bash
curl "http://localhost:8082/api/v1/candidates?page=0&size=5&sort=name,asc"
```

**Response (200):**
```json
{
    "success": true,
    "message": "Candidates retrieved successfully",
    "data": {
        "content": [
            { "id": "...", "name": "Alice Smith", ... },
            { "id": "...", "name": "Amit Verma", ... }
        ],
        "totalElements": 10,
        "totalPages": 2,
        "size": 5,
        "number": 0
    },
    "timestamp": "2026-04-21T10:30:00"
}
```

### Update Status (Disqualify)

**Request:**
```bash
curl -X PATCH http://localhost:8082/api/v1/candidates/a3f2b1c4-5d6e-7f8a-9b0c-1d2e3f4a5b6c/status \
  -H "Content-Type: application/json" \
  -d '{ "status": "DISQUALIFIED" }'
```

### Validate for Voting

**Request:**
```bash
curl "http://localhost:8082/api/v1/candidates/a3f2b1c4.../validate?electionId=11111111-1111-1111-1111-111111111111"
```

**Response (200) — Valid:**
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

### Validation Error (400)

**Request (blank name):**
```bash
curl -X POST http://localhost:8082/api/v1/candidates \
  -H "Content-Type: application/json" \
  -d '{ "name": "", "electionId": null }'
```

**Response (400):**
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

---

## 14. Testing Strategy

### Test Pyramid

```
                    ┌───────┐
                    │  E2E  │  ← Manual Postman tests (Day 5)
                   ┌┴───────┴┐
                   │ Controller│ ← MockMvc (15 tests)
                  ┌┴──────────┴┐
                  │  Service    │ ← Mockito (13 tests)
                 ┌┴────────────┴┐
                 │  Repository   │ ← @DataJpaTest + H2 (4 tests)
                 └──────────────┘
```

### Test Files & Coverage

| Test Class | Location | Tests | Framework |
|-----------|----------|-------|-----------|
| `CandidateServiceImplTest` | `test/.../service/` | 13 | Mockito + JUnit 5 |
| `CandidateControllerTest` | `test/.../controller/` | 15 | MockMvc + Mockito |
| `CandidateRepositoryTest` | `test/.../repository/` | 4 | @DataJpaTest + H2 |
| **Total** | | **32 tests** | |

### Service Layer Tests (13 tests)

| Test | What It Verifies |
|------|-----------------|
| `createCandidate_Success` | Happy path creation + save called once |
| `createCandidate_DuplicateResourceThrowsException` | 409 thrown, save never called |
| `getCandidateById_Success` | Returns correct DTO by externalId |
| `getCandidateById_NotFoundThrowsException` | 404 thrown when not found |
| `getAllCandidates_ReturnsPage` | Pagination works, mapping correct |
| `updateCandidate_Success` | Name/party updated correctly |
| `updateCandidateStatus_Success` | Status changes from ACTIVE to WITHDRAWN |
| `deleteCandidate_Success` | `isDeleted` set to true, save called |
| `getCandidatesByElection_Success` | Filters by electionId correctly |
| `validateCandidateForElection_Valid` | Returns isValid=true for active candidate |
| `validateCandidateForElection_InvalidElection` | Returns isValid=false for wrong election |
| `validateCandidateForElection_WithdrawnCandidate` | Returns isValid=false for WITHDRAWN status |
| `bulkRegisterCandidates_Success` | Creates 2 candidates, returns both |
| `getActiveCandidatesByElection_FiltersStatus` | Only returns ACTIVE candidates |
| `candidateExists_ReturnsTrue` | Boolean check returns true |

### Controller Layer Tests (15 tests)

| Test | HTTP Method | Status Code | What It Verifies |
|------|------------|-------------|-----------------|
| `createCandidate_Success` | POST | 201 | JSON structure, success flag |
| `bulkRegister_Success` | POST /bulk | 201 | Array response |
| `getCandidateById_Success` | GET /{id} | 200 | Correct ID returned |
| `getAllCandidates_Success` | GET | 200 | Paginated content |
| `getCandidatesByElection_Success` | GET /election/{id} | 200 | Election filter works |
| `getActiveCandidatesByElection_Success` | GET /election/{id}/active | 200 | Active filter works |
| `updateCandidate_Success` | PUT /{id} | 200 | Updates accepted |
| `updateStatus_Success` | PATCH /{id}/status | 200 | Status changed |
| `deleteCandidate_Success` | DELETE /{id} | 200 | Correct message |
| `candidateExists_Success` | GET /{id}/exists | 200 | Boolean true |
| `validateCandidate_Success` | GET /{id}/validate | 200 | Validation result |
| `createCandidate_ValidationFailed_WhenNameBlank` | POST | 400 | Blank name rejected |
| `createCandidate_ValidationFailed_WhenElectionIdNull` | POST | 400 | Null electionId rejected |
| `getCandidateById_Returns404` | GET /{id} | 404 | ResourceNotFoundException handled |
| `createCandidate_Returns409` | POST | 409 | DuplicateResourceException handled |

**Key Testing Pattern Used:**
- **Standalone MockMvc** (not `@WebMvcTest`) for faster, isolated tests
- `GlobalExceptionHandler` explicitly registered in MockMvc setup via `.setControllerAdvice()`
- Jackson `PageMixIn` to handle Pageable serialization issue in tests
- `PageableHandlerMethodArgumentResolver` added for pagination support

### Repository Layer Tests (4 tests)

| Test | What It Verifies |
|------|-----------------|
| `findByExternalIdAndIsDeletedFalse_Success` | UUID-based lookup works |
| `findByElectionIdAndIsDeletedFalse_Success` | Election filter returns correct results |
| `existsByNameAndElectionIdAndIsDeletedFalse_ReturnsTrue` | Duplicate detection query works |
| `findAllByIsDeletedFalse_FiltersDeleted` | Soft-deleted records are excluded |

**Test infrastructure:**
- `@DataJpaTest` — loads only JPA slice (no web layer, no service beans)
- `H2` in-memory database (test scope dependency)
- `hibernate.dialect = H2Dialect` overridden in `@TestPropertySource`
- `ddl-auto = create-drop` — fresh schema per test class

---

## 15. Design Decisions & Trade-offs

| # | Decision | Choice | Why | Alternative Considered |
|---|----------|--------|-----|----------------------|
| 1 | **Delete strategy** | Soft-delete (`is_deleted` flag) | Audit trail preservation, data recovery possible | Hard delete — simpler but loses history |
| 2 | **ID strategy** | Public UUID + Internal Long | Performance (Long PK) + Security (UUID public) | UUID-only PK — simpler but 10x slower joins |
| 3 | **Response wrapper** | `ApiResponse<T>` everywhere | Consistent contract for all clients | Raw entities — simpler but inconsistent |
| 4 | **Mapper approach** | Manual `@Component` class | Simple, debuggable, no annotation processor | MapStruct — zero boilerplate but complex setup |
| 5 | **URL versioning** | `/api/v1/` prefix | Future-proof; enables breaking changes | Header versioning — harder to test with curl |
| 6 | **Pagination** | Spring `Pageable` defaults | Standard Spring Data contract, zero config | Custom pagination — reinventing the wheel |
| 7 | **Validation** | Jakarta Bean Validation annotations | Declarative, standard, tested by framework | Manual if-checks — fragile, verbose |
| 8 | **Exception handling** | `@RestControllerAdvice` | Centralized, consistent, no try/catch in controllers | Per-controller try/catch — scattered, inconsistent |
| 9 | **Eureka client** | Included but `enabled: false` | Ready for Milestone 2 infrastructure setup | Add later — extra Maven change later |
| 10 | **Test framework** | Standalone MockMvc | Faster than `@SpringBootTest`, no context loading overhead | `@WebMvcTest` — slower, loads more beans |
| 11 | **Bulk operation** | Sequential `createCandidate()` per item | Reuses duplicate-check logic | `saveAll()` batch — skips business rules |
| 12 | **Config format** | `application.yml` | Hierarchical, readable | `.properties` — flat, harder to read nested config |
| 13 | **Credentials** | Environment variables | 12-factor app compliance, no secrets in code | Hardcoded — security risk |

---

## 16. Cross-Service Contracts

### Feign Endpoints (Called by Other Services)

The following endpoints are designed specifically for **inter-service communication** via OpenFeign:

```
┌──────────────────────────────────────────────────────────────┐
│  What Voting Service Will Call:                              │
│                                                              │
│  GET /api/v1/candidates/{id}/exists                          │
│    → Response: ApiResponse<Boolean>                          │
│    → Purpose: Fast existence check before accepting a vote   │
│                                                              │
│  GET /api/v1/candidates/{id}/validate?electionId=X           │
│    → Response: ApiResponse<CandidateValidationDTO>           │
│    → Purpose: Full validation (exists + correct election +   │
│               active status) before recording a vote         │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  What Result Service Will Call:                              │
│                                                              │
│  GET /api/v1/candidates/election/{electionId}                │
│    → Response: ApiResponse<List<CandidateResponseDTO>>       │
│    → Purpose: Get candidate names/parties for result display │
│                                                              │
│  GET /api/v1/candidates/election/{electionId}/active         │
│    → Response: ApiResponse<List<CandidateResponseDTO>>       │
│    → Purpose: Only show active candidates in results         │
└──────────────────────────────────────────────────────────────┘
```

### Future Kafka Events (Not Yet Implemented)

| Topic | Trigger | Payload | Consumer |
|-------|---------|---------|----------|
| `candidate.created` | `POST /candidates` | `{ candidateId, electionId, name }` | Voting Service (cache) |
| `candidate.status-changed` | `PATCH /{id}/status` | `{ candidateId, oldStatus, newStatus }` | Voting Service (reject votes) |
| `candidate.deleted` | `DELETE /{id}` | `{ candidateId, electionId }` | Voting Service (invalidate cache) |

---

## 17. Data Flow Diagrams

### Flow 1: Creating a Candidate

```
Client                Controller              Service                  Repository            DB
  │                      │                       │                        │                   │
  │  POST /candidates    │                       │                        │                   │
  │  { name, party,      │                       │                        │                   │
  │    electionId }       │                       │                        │                   │
  │─────────────────────►│                       │                        │                   │
  │                      │  @Valid passes         │                        │                   │
  │                      │  createCandidate(dto)  │                        │                   │
  │                      │──────────────────────►│                        │                   │
  │                      │                       │  existsByName...()      │                   │
  │                      │                       │───────────────────────►│                   │
  │                      │                       │                        │  SELECT COUNT(*)  │
  │                      │                       │                        │─────────────────►│
  │                      │                       │                        │◄─────────────────│
  │                      │                       │◄───────────────────────│  false            │
  │                      │                       │                        │                   │
  │                      │                       │  mapper.toEntity(dto)  │                   │
  │                      │                       │  repository.save()     │                   │
  │                      │                       │───────────────────────►│                   │
  │                      │                       │                        │  INSERT INTO...   │
  │                      │                       │                        │─────────────────►│
  │                      │                       │                        │◄─────────────────│
  │                      │                       │◄───────────────────────│  Candidate entity │
  │                      │                       │                        │                   │
  │                      │                       │  mapper.toResponseDTO()│                   │
  │                      │◄──────────────────────│  CandidateResponseDTO  │                   │
  │                      │                       │                        │                   │
  │  201 Created         │                       │                        │                   │
  │  ApiResponse<DTO>    │                       │                        │                   │
  │◄─────────────────────│                       │                        │                   │
```

### Flow 2: Vote Validation (Inter-Service)

```
Voting Service          Candidate Service          Candidate DB
     │                        │                         │
     │  GET /{id}/validate    │                         │
     │  ?electionId=X         │                         │
     │  (via OpenFeign)       │                         │
     │───────────────────────►│                         │
     │                        │  findByExternalId()     │
     │                        │────────────────────────►│
     │                        │◄────────────────────────│
     │                        │                         │
     │                        │  Check: exists?         │
     │                        │  Check: same election?  │
     │                        │  Check: ACTIVE status?  │
     │                        │                         │
     │  200 OK                │                         │
     │  { isValid: true/false,│                         │
     │    message: "...",     │                         │
     │    currentStatus: ...} │                         │
     │◄───────────────────────│                         │
```

---

## 18. Future Roadmap

### Immediate Next Steps (Candidate Service)

| Step | Task | Priority | Milestone |
|------|------|----------|-----------|
| 12 | **Kafka Integration** — Publish events on create/update/delete | 🟡 Medium | Milestone 4 |
| 13 | **Redis Caching** — Cache `getById()`, `getByElection()`, `exists()` | 🟡 Medium | Milestone 6 |
| 14 | **Spring Security** — JWT validation, `@PreAuthorize` on endpoints | 🔴 High | Milestone 7 |

### Remaining Unchecked Items

- [ ] `0.6` — Remove `@CrossOrigin(origins = "*")` from controller (handle CORS in Gateway/Config)
- [ ] `10.4` — Verify Eureka registration
- [ ] `12.1-12.5` — Kafka producer setup and event publishing
- [ ] `13.1-13.5` — Redis caching layer
- [ ] `14.1-14.5` — Spring Security + JWT integration

### Eventual Enhancements

| Enhancement | Description |
|-------------|-------------|
| **Search API** | `GET /search?name=&party=&electionId=` — Full-text search with Specification pattern |
| **State Machine** | Enforce valid status transitions (ACTIVE → WITHDRAWN ✅, WITHDRAWN → ACTIVE ❌) |
| **Audit Trail** | `@CreatedBy` / `@LastModifiedBy` — who made changes |
| **Pagination Metadata** | HATEOAS links in response (first, last, next, prev) |
| **Cache Eviction via Kafka** | When Voting Service detects issues, publish back to invalidate Candidate caches |
| **Flyway Migration** | Replace `ddl-auto: update` with versioned SQL migrations |

---

## 19. Quick Reference Cheat Sheet

### Start the Service

```bash
# Set environment variables
export VOTING_DB_URL=jdbc:postgresql://localhost:5432/candidateservice_db
export VOTING_DB_USER=postgres
export VOTING_DB_PASSWORD=yourpassword

# Run
cd candidate-service
mvn spring-boot:run
```

### Run Tests

```bash
mvn test                       # Run all 32 tests
mvn test -pl candidate-service # If in parent directory
```

### Access Points

| Resource | URL |
|----------|-----|
| API Base | `http://localhost:8082/api/v1/candidates` |
| Swagger UI | `http://localhost:8082/swagger-ui.html` |
| Health Check | `http://localhost:8082/actuator/health` |
| OpenAPI JSON | `http://localhost:8082/v3/api-docs` |

### HTTP Status Code Reference

| Code | Meaning | When |
|------|---------|------|
| `200` | OK | Successful GET, PUT, PATCH, DELETE |
| `201` | Created | Successful POST |
| `400` | Bad Request | Validation failure |
| `404` | Not Found | Candidate doesn't exist or was soft-deleted |
| `409` | Conflict | Duplicate candidate name in same election |
| `500` | Internal Server Error | Unexpected failure |

### Key Classes Quick Reference

| Need to... | Look at... |
|-----------|------------|
| Add a new endpoint | `CandidateController.java` |
| Add business logic | `CandidateServiceImpl.java` |
| Add a new query | `CandidateRepository.java` |
| Add a new response field | `CandidateResponseDTO.java` + `CandidateMapper.java` |
| Add a new validation | `CandidateRequestDTO.java` (annotation) or `CandidateServiceImpl.java` (logic) |
| Handle a new exception | `GlobalExceptionHandler.java` |
| Change DB schema | `Candidate.java` (entity) |
| Add a new status | `CandidateStatus.java` (enum) |

---

> 📖 **This is a living document.** Update this black book as new features (Kafka, Redis, Security) are integrated.  
> **Last Updated:** April 21, 2026  
> **Author:** Vaibhav Jain
