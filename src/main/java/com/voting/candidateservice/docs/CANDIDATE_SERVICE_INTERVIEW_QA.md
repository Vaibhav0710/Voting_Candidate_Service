# 🎤 Candidate Service — Technical Interview Q&A

This document contains a comprehensive list of technical interview questions based on the architecture, design patterns, and decisions made while building the **Candidate Service**. 

---

## 1. Architecture & Design Patterns

### Q1: Can you explain the layered architecture used in the Candidate Service?
**A:** The service uses a standard Spring Boot 3-tier architecture:
1. **Controller Layer (REST API):** A thin layer responsible only for HTTP routing, status codes, and delegating to the service.
2. **Service Layer (Business Logic):** Contains interfaces and implementations (e.g., `CandidateServiceImpl`) where all business rules, duplication checks, and transaction boundaries live.
3. **Repository Layer (Data Access):** Uses Spring Data JPA interfaces to interact directly with the PostgreSQL database.
This separation of concerns ensures that the application is easy to test, maintain, and scale.

### Q2: Why did you implement the "Public ID Pattern" (using an internal `Long` and an external `UUID`)?
**A:** This is a standard practice at FAANG companies for several key reasons:
- **Security:** Exposing an auto-incrementing `Long` ID in the API (`/api/v1/candidates/5`) allows attackers to guess other records and scrape the database. A `UUID` is unpredictable (`/api/v1/candidates/123e4567-e89b-12d3...`).
- **Performance:** `Long` auto-increment IDs are roughly 10x faster for database operations (indexing, joins, foreign keys) than UUIDs. 
- **Decoupling:** If we later migrate databases or change the primary key generation strategy, the external API contract containing the `UUID` doesn't change.

### Q3: Why did you use Data Transfer Objects (DTOs) instead of exposing your JPA Entities directly?
**A:** Exposing JPA entities directly is considered an anti-pattern. Using DTOs:
- **Prevents Over-Posting Attacks:** Users cannot modify fields like `isDeleted` or the internal database `id` by maliciously injecting them into the request JSON.
- **Enforces API Contracts:** Our API payload isn't strictly tied to our database schema. We can freely rename database columns without breaking client integrations.
- **Limits Data Exposure:** DTOs allow us to carefully curate exactly what data leaves the service, ensuring sensitive information never accidentally leaks.

### Q4: Can you explain your cross-service communication strategy, particularly the validation endpoint?
**A:** The `Candidate Service` provides validation endpoints (e.g., `validateCandidateForElection`) for the `Voting Service`. Instead of throwing exceptions (which would translate to HTTP 404 or 409), the Candidate Service returns a structured `CandidateValidationDTO` with a boolean `isValid` flag and a human-readable `message` (e.g., "Candidate is DISQUALIFIED"). This architectural decision means the Voting Service does not have to parse HTTP errors and can degrade gracefully or show meaningful feedback to the user.

---

## 2. Java & Spring Framework Core

### Q5: How do you handle dependency injection in your Spring Boot services, and why?
**A:** I use constructor injection via Lombok's `@RequiredArgsConstructor` along with `final` fields, rather than `@Autowired` field injection. 
- It ensures dependencies are strictly required (the class cannot be instantiated without them).
- It makes the classes easier to unit test without needing a Spring context or reflection since dependencies can be passed manually to the constructor.
- It prevents circular dependencies at startup.

### Q6: How do you handle exceptions globally in the Candidate Service?
**A:** I used Spring's `@RestControllerAdvice` combined with `@ExceptionHandler` methods (`GlobalExceptionHandler.java`). 
Instead of polluting controllers with `try-catch` blocks, exceptions (like `ResourceNotFoundException` or `DuplicateResourceException`) are thrown naturally in the Service layer. The Global Exception Handler catches them and maps them to consistent `ApiResponse<T>` wrappers with appropriate HTTP status codes (e.g., 404 Not Found, 409 Conflict).

### Q7: What is the significance of the `@Transactional` annotation on your service methods?
**A:** `@Transactional` ensures ACID properties for operations involving multiple database interactions. For example, during candidate creation, if we save a candidate but a subsequent operation fails in the same method block, the transaction will automatically roll back. This prevents orphaned or partially saved data, maintaining database consistency.

### Q8: How are validation rules enforced on incoming requests?
**A:** I used Jakarta Bean Validation annotations (e.g., `@NotBlank`, `@NotNull`) directly on the DTO properties. In the Controller, I added the `@Valid` annotation to the `@RequestBody`. If validation fails, Spring intercepts the request, throws a `MethodArgumentNotValidException`, and our `GlobalExceptionHandler` surfaces it cleanly as a `400 Bad Request` with exact field-level errors pointing to what's wrong.

### Q9: How is the `/candidates` GET endpoint returning lists of candidates?
**A:** The endpoint uses Spring Data JPA's built-in `Pageable` mechanism. Instead of returning the entire database (which would cause memory/OOM issues), it accepts `page` and `size` parameters to return a subset `Page<CandidateResponseDTO>`.

---

## 3. Database & Entity Management

### Q10: Why did you implement "Soft Deletes" instead of hard-deleting records?
**A:** In mission-critical systems like an online voting platform, **auditability and data lineage are non-negotiable**. If a candidate is disqualified or withdrawn, we cannot permanently `DELETE` their record from the database because we might have cast votes or audit logs pointing to their ID. Soft deletion (an `isDeleted = true` boolean flag) allows us to exclude them from standard queries (`findBy...AndIsDeletedFalse`) while safely retaining historical data.

### Q11: How do you generate the UUID for a new candidate securely?
**A:** I used a JPA lifecycle hook `@PrePersist` directly on the `Candidate` entity. Right before Hibernate attempts to run the `INSERT` query, this method checks if the `externalId` is null, and if so, safely generates a `UUID.randomUUID()`. This guarantees that it's impossible to create a database record missing this identifier, even if someone misses setting it at the service layer.

### Q12: How do you manage the states a Candidate can be in?
**A:** We use an `Enum` named `CandidateStatus` (ACTIVE, DISQUALIFIED, WITHDRAWN). State transitions flow in one direction (e.g., ACTIVE -> WITHDRAWN). Once a candidate withdraws, they cannot revert back to ACTIVE. The database maps this enum as a string column for human readability (`@Enumerated(EnumType.STRING)`), preventing issues if the enum array ordinal positions change.

### Q13: What happens during `bulkRegisterCandidates` if one candidate out of ten fails validation?
**A:** Because the method is annotated with `@Transactional`, if one candidate within the list fails validation or throws a `DuplicateResourceException`, the entire transaction rolls back. None of the candidates in that batch will be inserted into the database.

---

## 4. Testing & Reliability

### Q14: How did you test the REST endpoints without booting up a heavy web server?
**A:** I used `@WebMvcTest` in conjunction with `MockMvc`. This slices the Application Context to only load the web layer (Controllers, Exception Handlers) while we use `@MockBean` to mock out the underlying Service layer dependencies. This ensures that our HTTP routing, status codes, and input validations are heavily tested in isolation without spinning up an embedded Tomcat, resulting in incredibly fast test runs.

### Q15: How do you handle testing scenarios where a resource isn't found?
**A:** In the `MockMvc` tests, I use Mockito to inject a behavior where the service throws a `ResourceNotFoundException`. The test then performs an HTTP `GET`, and I specifically assert that the `GlobalExceptionHandler` intercepts it and correctly returns an HTTP status of `404 Not Found`, alongside validating the standard Error JSON object response shape.
