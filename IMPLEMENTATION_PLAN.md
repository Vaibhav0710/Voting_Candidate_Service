# 📋 Candidate Service — Implementation Plan

> **Project:** Blockchain-Inspired Online Voting System  
> **Service:** `candidate-service`  
> **Tech:** Java 17 · Spring Boot 4.x · PostgreSQL · Spring Cloud · Maven  
> **Started:** April 14, 2026  
> **Status:** 🟢 Step 0 & 1 Complete

---

## 📌 Current State (Baseline)

| Layer | File | Status |
|-------|------|--------|
| Entity | `Candidate.java` | ✅ Done (basic fields: id, name, party, electionId) |
| Repository | `ICandidateRepository.java` | ✅ Done (extends JpaRepository + `findByElectionId`) |
| Service Interface | `ICandidateService.java` | ⚠️ 3 methods (create, getById, list) |
| Service Impl | `CandidateService.java` | ⚠️ Working but returns raw entities |
| Request DTO | `CandidateRequestDTO.java` | ✅ Done (with @NotBlank, @NotNull validation) |
| Response DTO | `CandidateResponseDTO.java` | ✅ Done (basic fields) |
| Controller | `CandidateController.java` | ⚠️ 2 endpoints, not using DTOs |
| Config | `application.properties` | ⚠️ Only has app name |

### ⚠️ Known Issues to Fix
- Controller accepts raw `Candidate` entity → should use `CandidateRequestDTO`
- Controller returns raw `Candidate` entity → should return `CandidateResponseDTO`
- No `@Valid` annotation on request body
- URL paths use PascalCase (`/List`, `/CreateCandidate`) → should be REST-standard
- `getCandidateById()` returns `null` on not-found instead of throwing exception
- No global exception handling (`@RestControllerAdvice`)
- No `application.yml` with database / Eureka config

---

## 🏗️ Complete API Contract

### Base Path: `/api/v1/candidates`

### Phase 1 — Core CRUD APIs
| # | Method | Endpoint | Description | Auth |
|---|--------|----------|-------------|------|
| 1 | `POST` | `/api/v1/candidates` | Register a new candidate | ADMIN |
| 2 | `GET` | `/api/v1/candidates/{id}` | Get candidate by ID | PUBLIC |
| 3 | `GET` | `/api/v1/candidates` | List all candidates (paginated) | PUBLIC |
| 4 | `PUT` | `/api/v1/candidates/{id}` | Update candidate details | ADMIN |
| 5 | `DELETE` | `/api/v1/candidates/{id}` | Soft-delete a candidate | ADMIN |

### Phase 2 — Election-Scoped APIs (Voting Service depends on these)
| # | Method | Endpoint | Description | Auth |
|---|--------|----------|-------------|------|
| 6 | `GET` | `/api/v1/candidates/election/{electionId}` | Get all candidates for an election | PUBLIC |
| 7 | `GET` | `/api/v1/candidates/{id}/exists` | Check if candidate exists (lightweight) | INTERNAL |
| 8 | `GET` | `/api/v1/candidates/{id}/validate` | Validate candidate belongs to election | INTERNAL |

### Phase 3 — Bulk & Search APIs
| # | Method | Endpoint | Description | Auth |
|---|--------|----------|-------------|------|
| 9 | `POST` | `/api/v1/candidates/bulk` | Register multiple candidates at once | ADMIN |
| 10 | `GET` | `/api/v1/candidates/search?name=&party=&electionId=` | Search/filter candidates | PUBLIC |

### Phase 4 — Status Management APIs
| # | Method | Endpoint | Description | Auth |
|---|--------|----------|-------------|------|
| 11 | `PATCH` | `/api/v1/candidates/{id}/status` | Change candidate status | ADMIN |
| 12 | `GET` | `/api/v1/candidates/election/{electionId}/active` | Get only active candidates | PUBLIC |

---

## 🗂️ Target Package Structure

```
src/main/java/com/voting/candidate_service/
├── CandidateServiceApplication.java
├── config/
│   └── AppConfig.java                     ← CORS, beans, etc.
├── controller/
│   └── CandidateController.java           ← REST endpoints
├── dto/
│   ├── ApiResponse.java                   ← Generic response wrapper
│   ├── BulkCandidateRequestDTO.java       ← Bulk create request
│   ├── CandidateRequestDTO.java           ← Create request (exists)
│   ├── CandidateResponseDTO.java          ← Response payload (exists)
│   ├── CandidateStatusUpdateDTO.java      ← Status change request
│   ├── CandidateUpdateDTO.java            ← Update request
│   └── CandidateValidationDTO.java        ← Feign validation response
├── exception/
│   ├── DuplicateResourceException.java    ← 409 Conflict
│   ├── GlobalExceptionHandler.java        ← @RestControllerAdvice
│   └── ResourceNotFoundException.java     ← 404 Not Found
├── mapper/
│   └── CandidateMapper.java              ← Entity ↔ DTO conversion
├── model/
│   ├── Candidate.java                     ← JPA Entity (exists)
│   └── enums/
│       └── CandidateStatus.java           ← ACTIVE, DISQUALIFIED, WITHDRAWN
├── repository/
│   └── ICandidateRepository.java          ← JPA Repository (exists)
└── service/
    ├── ICandidateService.java             ← Interface (exists)
    └── CandidateService.java              ← Implementation (exists)
```

---

## 🔧 Step-by-Step Implementation Checklist

### Step 0: Fix Existing Issues
> Priority: 🔴 HIGH — These are architecture violations

- [x] 0.1 — Rename controller base path: `@RequestMapping("/candidate")` → `@RequestMapping("/api/v1/candidates")`
- [x] 0.2 — Change `@PostMapping("/CreateCandidate")` → `@PostMapping`
- [x] 0.3 — Change `@GetMapping("/List")` → `@GetMapping`
- [x] 0.4 — Controller: Accept `@Valid @RequestBody CandidateRequestDTO` instead of `Candidate`
- [x] 0.5 — Controller: Return `CandidateResponseDTO` (or `ApiResponse<CandidateResponseDTO>`) instead of `Candidate`
- [ ] 0.6 — Remove `@CrossOrigin(origins = "*")` from controller (handle CORS in Gateway/Config)

---

### Step 1: Entity & Schema Enhancement
> Files: `Candidate.java`, `CandidateStatus.java`

- [x] 1.1 — Create `model/enums/CandidateStatus.java` enum
- [x] 1.2 — Add `status` field to `Candidate` entity (`@Enumerated(EnumType.STRING)`, default `ACTIVE`)
- [x] 1.3 — Add `createdAt` field (`@CreationTimestamp`)
- [x] 1.4 — Add `updatedAt` field (`@UpdateTimestamp`)
- [x] 1.5 — Add `isDeleted` boolean (default `false`) for soft-delete
- [x] 1.6 — Add unique constraint on `(name, election_id)` to `@Table` annotation
- [x] 1.7 — Verify entity compiles and JPA auto-DDL creates correct schema

---

### Step 2: DTOs (Data Transfer Objects)
> Files: 5 new DTOs + 1 update

- [ ] 2.1 — Update `CandidateResponseDTO` → add `status`, `createdAt`, `updatedAt` fields
- [ ] 2.2 — Create `CandidateUpdateDTO` (optional `name`, optional `party`)
- [ ] 2.3 — Create `CandidateStatusUpdateDTO` (required `status` validated against enum)
- [ ] 2.4 — Create `CandidateValidationDTO` (`exists`, `active`, `electionId`) — used by Voting Service Feign
- [ ] 2.5 — Create `BulkCandidateRequestDTO` (`List<CandidateRequestDTO> candidates`)
- [ ] 2.6 — Create `ApiResponse<T>` generic wrapper (`success`, `message`, `data`, `timestamp`, `errors`)

---

### Step 3: Exception Handling
> Files: 3 new files

- [ ] 3.1 — Create `ResourceNotFoundException` (extends `RuntimeException`)
- [ ] 3.2 — Create `DuplicateResourceException` (extends `RuntimeException`)
- [ ] 3.3 — Create `GlobalExceptionHandler` (`@RestControllerAdvice`)
  - Handler for `ResourceNotFoundException` → HTTP 404
  - Handler for `DuplicateResourceException` → HTTP 409
  - Handler for `MethodArgumentNotValidException` → HTTP 400 (validation)
  - Handler for `ConstraintViolationException` → HTTP 400
  - Catch-all for `Exception` → HTTP 500

---

### Step 4: Mapper Utility
> Files: 1 new file

- [ ] 4.1 — Create `CandidateMapper` utility class
  - `toEntity(CandidateRequestDTO dto)` → `Candidate`
  - `toResponseDTO(Candidate entity)` → `CandidateResponseDTO`
  - `toResponseDTOList(List<Candidate> entities)` → `List<CandidateResponseDTO>`
  - `toResponseDTOPage(Page<Candidate> page)` → `Page<CandidateResponseDTO>`

---

### Step 5: Repository Enhancement
> File: `ICandidateRepository.java`

- [ ] 5.1 — Add `Optional<Candidate> findByIdAndIsDeletedFalse(UUID id)`
- [ ] 5.2 — Add `List<Candidate> findByElectionIdAndIsDeletedFalse(UUID electionId)`
- [ ] 5.3 — Add `List<Candidate> findByElectionIdAndStatusAndIsDeletedFalse(UUID electionId, CandidateStatus status)`
- [ ] 5.4 — Add `boolean existsByIdAndIsDeletedFalse(UUID id)`
- [ ] 5.5 — Add `boolean existsByNameAndElectionIdAndIsDeletedFalse(String name, UUID electionId)` (duplicate check)
- [ ] 5.6 — Add `Page<Candidate> findAllByIsDeletedFalse(Pageable pageable)` (paginated list)

---

### Step 6: Service Layer — Phase 1 (Core CRUD)
> Files: `ICandidateService.java`, `CandidateService.java`

- [ ] 6.1 — Rewrite `ICandidateService` with full method signatures using DTOs
- [ ] 6.2 — Implement `createCandidate(CandidateRequestDTO)` with duplicate name check
- [ ] 6.3 — Implement `getCandidateById(UUID)` with `ResourceNotFoundException`
- [ ] 6.4 — Implement `getAllCandidates(Pageable)` with pagination
- [ ] 6.5 — Implement `updateCandidate(UUID, CandidateUpdateDTO)` with partial update logic
- [ ] 6.6 — Implement `deleteCandidate(UUID)` as soft-delete (set `isDeleted = true`)

---

### Step 7: Service Layer — Phase 2 (Election-Scoped)
- [ ] 7.1 — Implement `getCandidatesByElection(UUID electionId)`
- [ ] 7.2 — Implement `candidateExists(UUID id)` → returns boolean
- [ ] 7.3 — Implement `validateCandidateForElection(UUID candidateId, UUID electionId)` → returns `CandidateValidationDTO`

---

### Step 8: Service Layer — Phase 3 & 4 (Bulk + Status)
- [ ] 8.1 — Implement `bulkCreateCandidates(BulkCandidateRequestDTO)` with `@Transactional`
- [ ] 8.2 — Implement `updateCandidateStatus(UUID, CandidateStatusUpdateDTO)`
- [ ] 8.3 — Implement `getActiveCandidatesByElection(UUID electionId)`

---

### Step 9: Controller — Wire All Endpoints
> File: `CandidateController.java`

- [ ] 9.1 — Phase 1: `POST /`, `GET /{id}`, `GET /`, `PUT /{id}`, `DELETE /{id}`
- [ ] 9.2 — Phase 2: `GET /election/{electionId}`, `GET /{id}/exists`, `GET /{id}/validate`
- [ ] 9.3 — Phase 3: `POST /bulk`, `GET /search` (with `@RequestParam` filters)
- [ ] 9.4 — Phase 4: `PATCH /{id}/status`, `GET /election/{electionId}/active`
- [ ] 9.5 — Wrap all responses in `ApiResponse<T>`
- [ ] 9.6 — Add proper HTTP status codes (`@ResponseStatus` or `ResponseEntity`)

---

### Step 10: Configuration
> Files: `application.yml`, `AppConfig.java`

- [ ] 10.1 — Create `application.yml` replacing `application.properties`
  - PostgreSQL datasource (candidate_service_db)
  - JPA: `ddl-auto: update`, `show-sql: true`
  - Eureka client config
  - Server port (e.g., `8082`)
  - Logging levels
- [ ] 10.2 — Create `AppConfig.java` for CORS config (if needed before Gateway)
- [ ] 10.3 — Verify service starts and connects to PostgreSQL
- [ ] 10.4 — Verify Eureka registration

---

### Step 11: Testing
- [ ] 11.1 — Unit tests for `CandidateService` (mock repository)
- [ ] 11.2 — Integration tests for `CandidateController` (`@WebMvcTest`)
- [ ] 11.3 — Repository tests (`@DataJpaTest`)
- [ ] 11.4 — Test all endpoints via Postman / cURL
- [ ] 11.5 — Test validation errors (blank name, null electionId)
- [ ] 11.6 — Test 404 / 409 exception scenarios

---

### Step 12: Kafka Integration (Future — After Voting Service)
- [ ] 12.1 — Add `spring-kafka` dependency to `pom.xml`
- [ ] 12.2 — Create Kafka producer config
- [ ] 12.3 — Publish `candidate.created` event after successful creation
- [ ] 12.4 — Publish `candidate.status-changed` event after status update
- [ ] 12.5 — Publish `candidate.deleted` event after soft-delete

---

### Step 13: Redis Caching (Future — Performance Optimization)
- [ ] 13.1 — Add `spring-boot-starter-data-redis` dependency
- [ ] 13.2 — Cache `getCandidateById()` results
- [ ] 13.3 — Cache `getCandidatesByElection()` results
- [ ] 13.4 — Cache `candidateExists()` results (used heavily by Voting Service)
- [ ] 13.5 — Implement cache eviction on create/update/delete

---

### Step 14: Security (Future — After User Service Integration)
- [ ] 14.1 — Add `spring-boot-starter-security` dependency
- [ ] 14.2 — Configure JWT token validation (validate tokens from User Service)
- [ ] 14.3 — Secure ADMIN-only endpoints (POST, PUT, DELETE, PATCH)
- [ ] 14.4 — Allow PUBLIC endpoints without auth (GET operations)
- [ ] 14.5 — Add `@PreAuthorize` annotations on controller methods

---

## 🔗 Cross-Service Dependencies

### What Voting Service Will Call (OpenFeign)
```
GET  /api/v1/candidates/{id}/exists           → boolean
GET  /api/v1/candidates/{id}/validate?electionId=X → CandidateValidationDTO
```

### What Result Service Will Call (OpenFeign)
```
GET  /api/v1/candidates/election/{electionId}         → List<CandidateResponseDTO>
GET  /api/v1/candidates/election/{electionId}/active   → List<CandidateResponseDTO>
```

### Kafka Events Published
| Topic | Trigger | Consumer |
|-------|---------|----------|
| `candidate.created` | POST create | Voting Service |
| `candidate.status-changed` | PATCH status | Voting Service |
| `candidate.deleted` | DELETE | Voting Service |

---

## 📊 Database Schema (Target)

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

---

## 📝 Notes & Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Delete strategy | Soft-delete (`is_deleted` flag) | Preserve audit trail, allow recovery |
| URL versioning | `/api/v1/` prefix | Enable future breaking changes |
| DTO pattern | Manual mapper class | Avoid MapStruct complexity for now |
| Pagination | Spring `Pageable` (default: 20) | Standard Spring Data approach |
| Response wrapper | `ApiResponse<T>` | Consistent structure for all consumers |

---

> **Last Updated:** April 14, 2026  
> **Next Step:** Step 0 — Fix existing code issues
