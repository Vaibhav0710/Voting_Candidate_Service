# 👤 Candidate Service — Service Overview

The **Candidate Service** is the registry for all participants in the Online Voting System. It manages the lifecycle of candidates, their affiliation with specific elections, and their eligibility status.

## 🎯 Purpose
In any election, there must be a "Single Source of Truth" for who the valid candidates are. This service:
1.  **Maintains the Registry**: Stores candidate names, political parties, and the elections they are contesting.
2.  **Enforces Rules**: Ensures that a candidate can only be registered once per election.
3.  **Manages Status**: Handles candidate withdrawals or disqualifications, ensuring the Voting Service can reject invalid votes in real-time.

## 🏗️ Core Responsibilities
- **Candidate Lifecycle**: CRUD operations (Create, Read, Update, Delete) for candidate records.
- **Soft Deletion**: Uses an `is_deleted` flag to preserve historical audit trails.
- **Election Scoping**: Groups candidates by `electionId` for easy retrieval by the frontend.
- **Validation Provider**: Exposes lightweight endpoints for the **Voting Service** to check if a candidate exists and is active before accepting a vote.

## 🛠️ Tech Stack
- **Framework**: Spring Boot 3.3.x
- **Persistence**: PostgreSQL (Spring Data JPA)
- **Mapping**: Manual Mapper pattern for clean Entity/DTO separation.
- **Validation**: Jakarta Bean Validation for strict input checking.
- **Audit**: Hibernate Audit annotations (`@CreationTimestamp`, `@UpdateTimestamp`).

## 📐 Architecture
The service is designed for high read-availability:
- **Stateless REST**: Standardized JSON responses via `ApiResponse<T>`.
- **Soft Delete Pattern**: All queries automatically filter out deleted records.
- **UUID Strategy**: Uses UUIDs for public-facing IDs to prevent database enumeration.

---
> **Related Documents:**
> - [API Reference](API_REFERENCE.md)
> - [Technical Blackbook](TECHNICAL_BLACKBOOK.md)
> - [Interview Guide](INTERVIEW_GUIDE.md)
