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

## 🚀 Advanced Discussion Topics
- **Eventual Consistency**: How Kafka helps keep the Voting Service's candidate cache in sync.
- **Bulk Operations**: Optimizing `batch inserts` for importing large candidate lists.
- **Data Governance**: Ensuring candidates' personal data (if any) is handled according to privacy laws.

---
> **Back to [Service Overview](SERVICE_OVERVIEW.md)**
