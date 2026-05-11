# 📓 Candidate Service — Technical Blackbook

Insights into the architectural decisions and internal patterns of the Candidate Service.

## ⚙️ Design Decisions

### 1. Soft Delete Implementation
- **Problem**: Hard-deleting a candidate would break foreign key integrity in the **Voting Service** (we need to know who a vote was for, even if the candidate is removed).
- **Solution**: We use an `is_deleted` boolean flag.
- **Implementation**: The Repository layer filters out `is_deleted = true` in all standard queries. This preserves the audit trail while hiding the record from the UI.

### 2. UUID Strategy
- **Choice**: UUID for public identifiers.
- **Rationale**: 
  - Prevents "Scraping": If we used `1, 2, 3`, an attacker could easily script a crawl of all candidates.
  - Predictability: Makes the system appear more professional and enterprise-grade.

### 3. Unique Constraints
- **Constraint**: `(name, election_id)`
- **Rationale**: This prevents a candidate from accidentally being registered twice for the same election, which would cause data corruption during the vote tally.

## 🚀 Performance & Scalability

### 1. Indexing Strategy
To ensure the **Voting Service** stays fast, we've indexed:
- `election_id`: For fast candidate lists.
- `status`: For filtering active participants.
- `(election_id, status)`: A composite index for the most common query.

### 2. DTO Pattern vs. Entity
- **Choice**: Strict DTO separation.
- **Rationale**: The `Candidate` entity contains JPA-specific logic and internal fields (like `isDeleted`). By returning a `CandidateResponseDTO`, we ensure the frontend only sees what it needs, reducing payload size and increasing security.

## 🔮 Future Enhancements
- **Kafka Integration**: When a candidate is created or their status changes, a message will be sent to the `candidate-events` topic. The **Voting Service** will consume this to update its local cache, reducing the need for REST calls during peak voting hours.
- **Redis Caching**: Candidate lists for popular elections will be cached in Redis to handle high traffic.

---
> **Back to [Service Overview](SERVICE_OVERVIEW.md)**
