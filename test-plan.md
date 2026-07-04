# Test Plan — Bond Issuance System

## What I'm trying to do

Give the team a repeatable check that BIS actually behaves the way `PRODUCT.md`
describes. I treat the system as a black box and test every layer you can reach
from outside — SFTP, both API versions, the web UI — plus the points where those
layers have to agree with each other.

Most of my attention goes to the money: allocation, coupons, and maturity. A
financial system that's wrong there is worse than one that's down, because the
errors are quiet.

## Scope

Covered:

- SFTP ingestion — file format and field validation (§5)
- Subscription rules — window, uniqueness, quantity, `X-User-Id`, capacity (§6)
- Allocation — full vs. proportional, floor rounding, REJECTED on zero (§7, §9A)
- Coupons — daily amount, business days only, weekend skip, through maturity (§8, §9A)
- Maturity — principal return, weekend shift, transition to MATURED (§9, §9A)
- System control — business date get/advance/reset and the lifecycle it triggers (§12)
- v1 vs v2 consistency (§11)
- Subscription atomicity under concurrency (§6)
- UI investor flows (subscribe, portfolio) reconciled against the API (§10, §11)
- Money precision via an independent `BigDecimal` check (§13)

Not covered, and why:

- Auth — the spec puts it out of scope.
- Performance/load and security testing — out of scope; the concurrency test is
  the only nod in that direction, and it's light.
- Public holidays — the spec only treats weekends as non-business days.

## How the suite is put together

- **Layered and black-box.** Each layer has its own tests. Where two layers
  describe the same thing, a test reconciles them — e.g. a subscription made in
  the browser is confirmed through the API portfolio, so a green UI test can't
  hide a broken backend.
- **The oracle is independent.** Expected amounts are recomputed with exact
  decimal arithmetic (`FinancialCalculator`) and tied to the §9A worked example.
- **Time is driven explicitly.** Nothing waits on the wall clock. Lifecycle
  progress happens through `POST /api/system/advance-date`, and fixtures compute
  their dates relative to the live business date.
- **Priorities via Allure severities.** BLOCKER = ingestion, subscribe, and the
  three money calculations. CRITICAL = validation, timing/duplicate rules,
  parity, concurrency. NORMAL = the secondary flows.
- **Reliability over speed.** Sequential run because of the shared business date.
  UI tests build their own data and can be dropped with `SKIP_UI=true`.

## Coverage matrix

| Area | Spec § | Test class | What it checks |
|------|--------|-----------|----------------|
| SFTP ingestion (happy path) | 5 | `BondIngestionApiTest` | valid file creates a bond; multi-bond file; duplicate filename not reprocessed; duplicate ISIN rejected; max-limit values accepted |
| CSV validation | 5 | `BondValidationApiTest` | 27 data-driven negatives: ISIN length/empty, issuer/bond name empty and over-length, currency length and unassigned code, faceValue sign/max/decimals/non-numeric, couponRate bounds/precision, totalSize sign/max/non-integer, date ordering, date format, missing header, malformed row |
| Subscription rules | 6 | `SubscriptionApiTest` | in-window OK; before-open and after-close rejected; missing `X-User-Id`; non-positive quantity; duplicate per investor |
| Concurrency | 6 | `ConcurrencyApiTest` | 5 simultaneous orders against small capacity → no oversell |
| Allocation | 7, 9A | `AllocationApiTest` | oversubscribed 40k/30k/50k → 33,333/25,000/41,666 (sum 99,999); undersubscribed full; zero allocation → REJECTED |
| Coupons | 8, 9A | `CouponPaymentApiTest` | daily = face×rate×qty (50.00); weekend skip; total through maturity |
| Maturity | 9, 9A | `MaturityApiTest` | principal = face×qty; weekend → next business day; status → MATURED |
| System date | 12 | `SystemDateApiTest` | get returns a valid date; advance moves exactly one day; reset → today |
| API parity | 11 | `ApiVersionParityTest` | v1/v2 bond detail values equal; both lists contain the bond |
| Web UI | 10, 11 | `UiSubscriptionTest`, `UiPortfolioTest` | list renders; UI subscribe shows up in the API; API subscribe shows up in the UI portfolio |

## Where things live

- `clients/` — one REST Assured client per API version, plus system control.
- `sftp/` — the JSch client and `BondCsvBuilder` for valid and broken files.
- `support/` — base classes (`BaseApiTest`, `BondTestSupport`, `BaseUiTest`) and
  `BondFixture`, the single place that does upload/poll/advance so API and UI
  tests share it.
- `util/` — `FinancialCalculator` (the oracle), `DateHelper` (business days),
  `IsinGenerator`, `JsonFields` (schema-tolerant reads).
- `pages/` — `AppPage`, which owns the UI locators.
- `resources/testng.xml` — sequential suite; UI split out so it can be skipped.

## Trade-offs and assumptions

- **Endpoints and JSON shapes** are my best guess from the `PRODUCT.md`
  conventions and normal REST layout — Swagger is the real authority. Reading is
  intentionally forgiving (a few candidate paths) and kept in the client/helper
  layer, so reconciling any difference is a one-file change. This is the part I'd
  verify first against a running stack.
- **Sequential, not parallel** — forced by the global business date.
- **The UI is tested shallower than the API** — the interesting logic is in the
  backend, so the UI tests cover the main journey and the cross-layer check
  rather than every permutation.
- **Concurrency is best effort** — real threads fire together, but one local
  backend only lets you push the race so hard.
