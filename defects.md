# Defects & Findings — Bond Issuance System

Where the running system does something other than what `PRODUCT.md` says, it
gets written up here with enough detail to reproduce it without me in the room.

**Status.** The entries come from running `./run-tests.sh` against a live stack
(`docker compose up -d`). Every failing test maps to a finding below, filled in
from the Allure report — request and response payloads, and screenshots for the
UI ones, are attached under `tests/target/site/allure-maven-plugin`. Section 3
is my watchlist: the behaviours I most expect to break, each with a test aimed at
it. After a run I confirm each one and promote any failure into a numbered entry.

## 1. Severity

| Severity | What it means |
|----------|---------------|
| BLOCKER  | Core money movement or lifecycle is broken. Not shippable. |
| CRITICAL | A rule is violated with money or data-integrity impact. |
| MAJOR    | Wrong, but contained. |
| MINOR    | Cosmetic or a contract inconsistency with little impact. |

## 2. Findings

One block per confirmed deviation. Template:

### DEF-001 — <short title>

- **Severity:** <BLOCKER | CRITICAL | MAJOR | MINOR>
- **Spec:** `PRODUCT.md` §<n>
- **Failing test:** `com.bis.qa.<package>.<Class>#<method>`
- **Expected:** <what the spec says should happen>
- **Actual:** <what the system did>
- **Repro:**
  1. <request / SFTP upload / date advance…>
  2. …
- **Evidence:** <request/response snippet, Allure link, screenshot>
- **Impact:** <why it matters>

<!-- Copy the block for DEF-002, DEF-003, … as failures are confirmed. -->

Nothing recorded yet — the backend images sit behind a private registry I don't
currently have credentials for, so I haven't been able to bring the stack up and
run the suite end to end. Once `docker login interviewmarketnode.azurecr.io`
succeeds and `curl http://localhost:8080/api/system/date` returns a date, one
run of `./run-tests.sh` populates this section from the failures.

## 3. Watchlist — the behaviours I'm watching

These are where I'd bet the bugs are. Each has a dedicated assertion; confirm the
outcome against the live system and promote any failure above.

| # | Behaviour | Spec | Test |
|---|-----------|------|------|
| 1 | Proportional allocation floors and totals 99,999, not 100,000 | §7, §9A | `AllocationApiTest#oversubscribed…` |
| 2 | Zero-allocation subscribers are REJECTED, not ALLOCATED with 0 | §7 | `AllocationApiTest#zeroAllocation…` |
| 3 | Coupon uses the daily rate: face×rate×qty = 50.00, not annualised | §8 | `CouponPaymentApiTest#dailyCoupon…` |
| 4 | No coupon on Sat/Sun | §8 | `CouponPaymentApiTest#coupon_isNotPaidOnWeekends` |
| 5 | Coupon paid up to and including maturity | §8, §9A | `CouponPaymentApiTest#coupon_accruesEveryBusinessDayUntilMaturity` |
| 6 | Maturity on a weekend pays the next business day | §9 | `MaturityApiTest#maturityOnWeekend…` |
| 7 | Amounts are exact decimal, no float drift | §13 | coupon/maturity/allocation tests |
| 8 | Subscription rejected before open and after close | §6 | `SubscriptionApiTest#subscribeBefore/AfterBook…` |
| 9 | One subscription per investor per bond | §6 | `SubscriptionApiTest#duplicateSubscription…` |
| 10 | `X-User-Id` required to subscribe | §6 | `SubscriptionApiTest#subscribeMissingUserHeader…` |
| 11 | Concurrent subscriptions can't oversell | §6 | `ConcurrencyApiTest#concurrentSubscriptions_doNotOversell` |
| 12 | Invalid CSVs (27 variants) are rejected, not silently skipped | §5 | `BondValidationApiTest` |
| 13 | Duplicate ISIN is not reprocessed or overwritten | §5 | `BondIngestionApiTest#duplicateIsin…` |
| 14 | Duplicate file names are not reprocessed | §5 | `BondIngestionApiTest#duplicateFileName…` |
| 15 | Values sitting exactly on the max limits are accepted | §5 | `BondIngestionApiTest#boundaryValues…` |
| 16 | `advance-date` moves exactly one day and fires lifecycle events | §12 | `SystemDateApiTest`, lifecycle tests |
| 17 | v1 and v2 report the same bond values | §11 | `ApiVersionParityTest` |

## 4. Environment notes

- The business date is one global value. The suite runs sequentially and resets
  it at the start of a run (`POST /api/system/reset`).
- The sample `fixtures/` CSVs use dates in the past relative to the starting
  business date, so I use them to show the format only — tests generate their own
  date-relative files at runtime.
