# 📖 Candidate Service — API Reference

Base Path: `/api/v1/candidates`

## 1. Core CRUD Operations

### Create Candidate
- **URL**: `POST /api/v1/candidates`
- **Auth**: ADMIN
- **Body**:
```json
{
  "name": "Rahul Sharma",
  "party": "Swaraj Party",
  "electionId": "11111111-1111-1111-1111-111111111111"
}
```

### Get Candidate by ID
- **URL**: `GET /api/v1/candidates/{id}`
- **Auth**: PUBLIC

### Update Candidate
- **URL**: `PUT /api/v1/candidates/{id}`
- **Auth**: ADMIN

### Soft Delete Candidate
- **URL**: `DELETE /api/v1/candidates/{id}`
- **Auth**: ADMIN

---

## 2. Election-Scoped APIs

### Get Candidates by Election
Returns all active candidates for a specific election.
- **URL**: `GET /api/v1/candidates/election/{electionId}`
- **Auth**: PUBLIC

### Validate Candidate (Internal)
Used by Voting Service to check if a candidate is active.
- **URL**: `GET /api/v1/candidates/{id}/validate?electionId={eid}`
- **Auth**: INTERNAL

---

## 3. Bulk & Status Operations

### Bulk Register
- **URL**: `POST /api/v1/candidates/bulk`
- **Body**: `{"candidates": [...]}`

### Update Status
Change status to `DISQUALIFIED` or `WITHDRAWN`.
- **URL**: `PATCH /api/v1/candidates/{id}/status`
- **Body**: `{"status": "WITHDRAWN"}`

---

## 🛑 Status Codes

| Code | Meaning |
|------|---------|
| `200` | Success. |
| `201` | Created successfully. |
| `404` | Candidate or Election not found. |
| `409` | Conflict (e.g., candidate already registered in this election). |

---
> **Back to [Service Overview](SERVICE_OVERVIEW.md)**
