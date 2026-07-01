# Bond Issuance System — QA Automation Assessment

## Context

You are joining a capital markets engineering team as a **QA Automation Engineer**. The Bond Issuance System (BIS) has been deployed and is used in production. Your job is to build a comprehensive automated test suite that gives the team confidence in the system's correctness.

The system manages bond issuances, investor subscriptions, allocations, coupon payments, and maturity events. It accepts bond data via SFTP file upload, exposes two API versions simultaneously, and provides a web interface for investors.

**Authentication is not in scope.** The application provides predefined mock users (INV-001 through INV-005) with a user switcher. API calls identify the user via the `X-User-Id` header.

`PRODUCT.md` is the source of truth for expected system behavior.

**Sample fixtures are examples only.** The CSV files under `fixtures/` illustrate the expected file format and are not guaranteed to be usable as-is. The business date starts at today's calendar date.

**API Documentation:** The backend provides interactive API documentation via Swagger UI at `http://localhost:8080/swagger-ui.html`. Use this to explore available endpoints, request/response schemas, and test API calls directly.

---

## Your Tasks

### 1. Build an Automated Test Suite

Create a test suite that validates the system behaves according to its product specification. The suite should demonstrate your ability to think critically about what could go wrong in a financial system and verify that the implementation matches the documented behavior.

Consider all layers of the system and the interactions between them.

### 2. Document Your Test Approach

Create `test-plan.md` explaining your testing strategy, what you chose to cover, how you structured the suite, and any trade-offs you made.

### 3. Document Findings

Create `defects.md` recording anything you discover where the system's actual behavior differs from the documented specification. Include sufficient detail for a developer to reproduce each finding.

### 4. Make It Runnable

Provide a single command from the repository root:

```bash
./run-tests.sh
```

The script assumes the application stack (`docker compose up -d`) is already running and in a clean state.

The script **must**:
1. Execute the full test suite
2. Generate a test report at the end

### 5. Maintain AI Usage Log

Append every AI interaction to `ai-prompt.log`:

```text
---
timestamp: 2026-05-05T10:30:00Z
model: <model name>
prompt: |
  <full prompt text>
---
```

---

## Constraints

- **Branch:** Submit on `candidate/<your-name>`
- **Deadline:** 7 days (168 hours) from access granted
- **Docker:** Do not add third-party Docker images beyond what is already in docker-compose
- **Expected effort:** 2–3 hours. Do not spend more than 6 hours.

---

## Deliverables

| File | Purpose |
|------|---------|
| `run-tests.sh` | Single entry point to run the full suite |
| `test-plan.md` | Test strategy and coverage documentation |
| `defects.md` | Findings with reproduction evidence |
| `README.md` | Setup steps and architecture decisions |
| `ai-prompt.log` | AI interaction log |

---

## Evaluation

Your submission will be evaluated on automation design, coverage breadth, test reliability, and the quality of your defect evidence.

AI usage is encouraged. Use whatever tools help you work effectively.
