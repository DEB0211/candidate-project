# Defects & Findings — Bond Issuance System

This document records any behaviour where the running system deviates from
[`PRODUCT.md`](PRODUCT.md). Each entry carries enough detail for a developer to
reproduce it independently.

> **Status:** The findings below are produced by running `./run-tests.sh`
> against the live stack (`docker compose up -d`). Any test that fails maps to an
> entry here, populated from the Allure report (request/response payloads and, for
> UI defects, screenshots are attached in `tests/target/site/allure-maven-plugin`).
> The **Watchlist** in section 3 lists the highest-risk behaviours the suite
> targets — confirm/annotate each after the run.

---

## 1. Severity legend

| Severity | Meaning |
|----------|---------|
| BLOCKER  | Core money movement or lifecycle broken; unusable in production. |
| CRITICAL | Rule violation with financial or data-integrity impact. |
| MAJOR    | Incorrect but contained behaviour. |
| MINOR    | Cosmetic / contract inconsistency with low impact. |

---

## 2. Findings

> Populate one block per confirmed deviation. Template:

### DEF-001 — <short title>

- **Severity:** <BLOCKER | CRITICAL | MAJOR | MINOR>
- **Spec reference:** `PRODUCT.md` §<n>
- **Failing test:** `com.bis.qa.<package>.<Class>#<method>`
- **Expected (per spec):** <what should happen>
- **Actual (observed):** <what the system did>
- **Steps to reproduce:**
  1. <request / SFTP upload / date advance…>
  2. …
- **Evidence:** <request/response snippet, Allure test link, screenshot>
- **Notes / impact:** <analysis>

<!-- Duplicate the block above for DEF-002, DEF-003, … as failures are confirmed. -->

_No defects recorded yet — run `./run-tests.sh` against the live stack to populate._

---

## 3. Watchlist — high-risk behaviours the suite verifies

These are the areas most likely to reveal spec deviations. Each has a dedicated
assertion; confirm the outcome against the live system and promote any failure to
a numbered finding above.

| # | Behaviour under test | Spec | Test |
|---|----------------------|------|------|
| 1 | Proportional allocation floors correctly and total allocated = 99,999 (not rounded up to 100,000) | §7, §9A | `AllocationApiTest#oversubscribed…` |
| 2 | Zero-allocation subscribers are marked `REJECTED` (not `ALLOCATED` with 0) | §7 | `AllocationApiTest#zeroAllocation…` |
| 3 | Coupon uses a **daily** rate: face×rate×qty = 50.00 (not annualised) | §8 | `CouponPaymentApiTest#dailyCoupon…` |
| 4 | No coupon accrues on Saturday/Sunday | §8 | `CouponPaymentApiTest#coupon_isNotPaidOnWeekends` |
| 5 | Coupon is paid up to and including maturity date | §8, §9A | `CouponPaymentApiTest#coupon_accruesEveryBusinessDayUntilMaturity` |
| 6 | Maturity on a weekend pays on the next business day | §9 | `MaturityApiTest#maturityOnWeekend…` |
| 7 | Amounts are exact decimal (no floating-point drift) | §13 | coupon/maturity/allocation tests |
| 8 | Subscription rejected before book open and after book close | §6 | `SubscriptionApiTest#subscribeBefore/AfterBook…` |
| 9 | One subscription per investor per bond | §6 | `SubscriptionApiTest#duplicateSubscription…` |
| 10 | `X-User-Id` required for subscription | §6 | `SubscriptionApiTest#subscribeMissingUserHeader…` |
| 11 | Concurrent subscriptions cannot oversell capacity (atomicity) | §6 | `ConcurrencyApiTest#concurrentSubscriptions_doNotOversell` |
| 12 | Invalid CSVs (21 variants) are rejected, not silently skipped | §5 | `BondValidationApiTest` |
| 13 | Duplicate file names are not reprocessed | §5 | `BondIngestionApiTest#duplicateFileName…` |
| 14 | `advance-date` moves the date by exactly one day and triggers lifecycle events | §12 | `SystemDateApiTest`, lifecycle tests |
| 15 | API v1 and v2 report the same bond values consistently | §11 | `ApiVersionParityTest` |

---

## 4. Environment notes

- Business date is a global singleton; the suite runs sequentially and resets the
  date at the start of each run (`POST /api/system/reset`).
- Sample fixtures under `fixtures/` use dates in the past relative to the initial
  business date (today's calendar date), so they are used only to illustrate
  format; tests generate their own date-relative fixtures at runtime.
