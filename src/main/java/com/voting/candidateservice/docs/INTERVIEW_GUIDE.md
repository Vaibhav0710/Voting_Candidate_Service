# 🎯 Candidate Service — Interview Guide

Prep questions for technical interviews regarding the Candidate microservice.

## 🧠 Technical Questions & Answers

### Q1: Why did you use Soft Delete instead of hard deleting records?
> **Answer**: In a microservices ecosystem, especially for a voting system, data integrity and auditability are critical. If we hard-delete a candidate, any existing votes in the **Voting Service** that reference that candidate's ID would become "orphaned" or point to non-existent data. Soft deletion preserves the record for auditing while logically removing it from active election lists.

### Q2: How do you handle a "Candidate Withdrawal" scenario?
> **Answer**: We use a `status` enum (`ACTIVE`, `WITHDRAWN`, `DISQUALIFIED`). If a candidate withdraws, we update their status to `WITHDRAWN`. The **Voting Service** checks this status before accepting any new votes. This allows us to keep the candidate on the list (for historical transparency) but stop accepting votes for them immediately.

### Q3: What happens if two admins try to register the same candidate simultaneously?
> **Answer**: We have a unique constraint at the database level on the combination of `name` and `electionId`. If a race condition occurs, the second request will trigger a `DataIntegrityViolationException`, which our `GlobalExceptionHandler` converts into a `409 Conflict` response for the user.

### Q4: How would you scale this service if election day traffic peaks?
> **Answer**: Since the candidate list is relatively static during the voting period, this service is highly cacheable. We could implement **Redis caching** for the `getCandidatesByElection` endpoint. Additionally, we can use **Read-Replicas** for the PostgreSQL database to handle massive read traffic without impacting the primary DB.

### Q5: Why is the `electionId` passed as a UUID?
> **Answer**: It ensures that we don't leak information about the total number of elections in the system and prevents attackers from "walking" the database to find private or upcoming election data. It also allows for easier integration with external systems that might use non-sequential IDs.

### Q6: Why is it beneficial to decouple this from the User Service?
> **Answer**: Decoupling ensures that candidate management doesn't depend on the availability of the User Service (and vice versa). It allows for independent scaling, better fault isolation, and specialized data modeling for election-specific needs.

### Q7: Explain the CAP Theorem in the context of this voting system.
> **Answer**: In a distributed system, we must choose between **Consistency** and **Availability** during a network partition. For voting, we typically prioritize **Strict Consistency** (ensure every vote is valid and not duplicated) over Availability (blocking votes if consistency cannot be guaranteed).

### Q8: How do you handle database schema changes?
> **Answer**: We use tools like **Flyway** or **Liquibase** for database migrations. This ensures that schema changes are versioned, automated, and consistent across all environments (dev, staging, production).

### Q9: How do you distinguish between Unit and Integration tests here?
> **Answer**: 
> - **Unit Tests**: Focus on business logic (e.g., candidate validation) in isolation using Mockito to mock dependencies.
> - **Integration Tests**: Focus on the interaction with the database or other services, typically using `@DataJpaTest` or `Testcontainers` to run a real PostgreSQL instance.

### Q10: How does this service participate in Service Discovery?
> **Answer**: It is a **Eureka Client**. At startup, it registers its IP and port with the Eureka Server. The API Gateway then uses this registry to route requests to `/api/v1/candidates/**` to an available instance of this service.

---
## 🚀 Advanced Discussion Topics
- **Eventual Consistency**: How Kafka helps keep the Voting Service's candidate cache in sync.
- **Bulk Operations**: Optimizing `batch inserts` for importing large candidate lists.
- **Audit Logging**: Tracking every change made to a candidate's profile for transparency.

### Q11: How do you handle Distributed Data Consistency across services?
> **Answer**: We follow the **Database-per-Service** pattern. For operations that span multiple services (like updating a candidate and notifying the voting service), we prefer **Eventual Consistency** via events (Kafka). We also use **Idempotency keys** to ensure that retries don't create duplicate data.

### Q12: How do you monitor the health and performance of this service?
> **Answer**: We use **Spring Boot Actuator** to expose metrics. These metrics (CPU, Memory, DB connection pool status) are collected by **Prometheus** and visualized in **Grafana**. We also use **Sleuth/Zipkin** for distributed tracing to find performance bottlenecks in cross-service calls.

---
> **Back to [Service Overview](SERVICE_OVERVIEW.md)**
