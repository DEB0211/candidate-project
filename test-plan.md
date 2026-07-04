# Test Plan — Bond Issuance System

## 1. Objective

Provide the engineering team with an automated, repeatable safety net that
verifies the Bond Issuance System (BIS) behaves exactly as documented in
[`PRODUCT.md`](PRODUCT.md). The suite treats the system as a black box and
validates behaviour across every externally observable layer and the
interactions between them.

## 2. Scope

**In scope**

- Bond ingestion via SFTP (file format & field validation) — §5
- Subscription rules (window, uniqueness, quantity, auth header, capacity) — §6
- Allocation (full vs. proportional, floor rounding, REJECTED) — §7 & §9A
- Coupon payments (daily amount, business-day-only accrual, weekend skip, up to maturity) — §8 & §9A
- Maturity (principal return, weekend shift, MATURED transition) — §9 & §9A
- System control (business date get/advance/reset, lifecycle triggers) — §12
- API v1 ↔ v2 consistency — §11
- Concurrency / atomicity of available size — §6
- Web UI investor flows (subscribe, portfolio) and reconciliation with the API — §10/§11
- Money precision via an independent `BigDecimal` oracle — §13

**Out of scope**

- Authentication/authorization (explicitly out of scope per spec).
- Non-functional performance/load and security penetration testing (only light
  concurrency probing is included).
- Public-holiday calendars (spec defines only weekends as non-business days).

## 3. Test strategy

- **Layered, black-box.** Each layer (SFTP, API v1, API v2, UI) has dedicated
  tests; cross-layer tests reconcile state between layers (e.g. a UI subscription
  is verified through the API portfolio).
- **Spec-driven oracle.** Expected financial values are recomputed independently
  with exact decimal arithmetic (`FinancialCalculator`) and anchored to the
  worked example in §9A, so a wrong-but-consistent implementation is still caught.
- **Deterministic time control.** All lifecycle progression is driven explicitly
  through `POST /api/system/advance-date`. Fixtures compute their book/maturity
  dates relative to the live business date, making tests independent of the real
  calendar and of prior test runs.
- **Risk-based prioritisation** via Allure severities:
  - `BLOCKER` — ingestion works, subscription works, allocation math, coupon math, maturity.
  - `CRITICAL` — validation rejections, duplicate/timing rules, parity, concurrency.
  - `NORMAL` — secondary flows (multi-bond files, list parity, UI portfolio).
- **Reliability first.** The suite runs sequentially because the business date is
  global shared state; this trades speed for deterministic, non-flaky results.
  UI tests recreate their own data and can be excluded (`SKIP_UI=true`) in
  browser-less environments.

## 4. Coverage matrix

| Area | Spec §| Test class | Representative cases |
|------|-------|-----------|----------------------|
| SFTP ingestion (happy path) | 5 | `BondIngestionApiTest` | valid file creates bond; multi-bond file; duplicate filename not reprocessed |
| CSV validation | 5 | `BondValidationApiTest` | 21 data-driven negatives: ISIN length/empty, currency, faceValue sign/max/decimals/non-numeric, couponRate bounds/precision, totalSize sign/max/non-integer, date ordering, date format, missing header, malformed row |
| Subscription rules | 6 | `SubscriptionApiTest` | within window OK; before-open & after-close rejected; missing `X-User-Id`; non-positive quantity; duplicate per investor |
| Concurrency / atomicity | 6 | `ConcurrencyApiTest` | 5 simultaneous orders vs. small capacity → no oversell |
| Allocation | 7, 9A | `AllocationApiTest` | oversubscribed 40k/30k/50k → 33,333/25,000/41,666 (sum 99,999); undersubscribed full; zero-allocation → REJECTED |
| Coupon payments | 8, 9A | `CouponPaymentApiTest` | daily = face×rate×qty (50.00); weekend skip; total through maturity |
| Maturity | 9, 9A | `MaturityApiTest` | principal = face×qty; weekend → next business day; status → MATURED |
| System date | 12 | `SystemDateApiTest` | get returns valid date; advance +1 day exactly; reset → today |
| API parity | 11 | `ApiVersionParityTest` | v1/v2 bond detail values equal; both lists contain the bond |
| Web UI | 10, 11 | `UiSubscriptionTest`, `UiPortfolioTest` | list renders; subscribe via UI reflected in API; API subscription shown in UI portfolio |

## 5. Suite structure

- `clients/` — REST Assured clients per API version + system control.
- `sftp/` — JSch client and a fluent `BondCsvBuilder` for valid & malformed files.
- `support/` — base classes (`BaseApiTest`, `BondTestSupport`, `BaseUiTest`) and a
  shared `BondFixture` (single source for upload/poll/advance so API & UI reuse it).
- `util/` — `FinancialCalculator` (money oracle), `DateHelper` (business days),
  `IsinGenerator`, `JsonFields` (schema-tolerant reads).
- `pages/` — `AppPage` Page Object isolating UI locators.
- `resources/testng.xml` — sequential suite; UI grouped separately for opt-out.

## 6. Reporting

- **Allure** produces an HTML report with steps, severities, request/response
  logging on failures, and UI failure screenshots.
- **Surefire** XML feeds the pass/fail summary printed by `run-tests.sh`.

## 7. Trade-offs & assumptions

- **Endpoint/JSON shapes** are derived from `PRODUCT.md` conventions and REST
  norms; Swagger is the authoritative source. Response reading is deliberately
  tolerant (multiple candidate paths) and centralised so reconciling any
  difference touches only the client/helper layer.
- **Sequential over parallel** for correctness given the global business date.
- **UI depth is intentionally shallower** than the API layer: the richest
  behaviour lives in the API/back end, so UI tests focus on the critical journey
  and layer reconciliation rather than exhaustive UI permutations.
- **Concurrency is best-effort**: real threads fire simultaneously, but a single
  local backend limits how aggressively true races can be forced.
