# Bond Issuance System — QA Automation

Automated black-box tests for the Bond Issuance System (BIS). Everything here
checks the running system against what `PRODUCT.md` says it should do — the SFTP
ingestion path, both API versions, the subscription → allocation → coupon →
maturity lifecycle, v1/v2 consistency, subscription atomicity, and the investor
web UI.

The tests treat the app as a black box. Nothing pokes at the database or the
backend internals; if a behaviour isn't observable through SFTP, the API, or the
browser, it isn't tested.

## Stack

- **Java 21 + Maven** — matches the project's existing layout and the role.
- **TestNG** — groups and data providers, plus explicit control over ordering,
  which I need because the business date is global (see below).
- **REST Assured** — for the v1 (snake_case) and v2 (camelCase) APIs.
- **JSch** (`com.github.mwiede` fork) — uploads bond CSVs over SFTP. The fork is
  there because the stock JSch can't negotiate ciphers with a current `atmoz/sftp`.
- **Selenium 4** with a page object — Selenium Manager sorts out the driver.
- **BigDecimal** — used as a hand-rolled money oracle so the coupon/allocation/
  maturity amounts are checked against numbers the test computes itself, not the
  ones the app hands back.
- **Allure + Surefire** — HTML report with steps and, on UI failures, screenshots.

## Before you run

- Docker Desktop running.
- Access to the private registry the images live in:

  ```bash
  docker login interviewmarketnode.azurecr.io
  ```

  Those registry credentials come with the assessment. They are *not* the SFTP
  `bonduser`/`bondpass` login — that only gets you into the SFTP container.

- JDK 21+ and Maven 3.9+ on the path.
- A Chrome/Chromium install for the UI tests (skip them if you don't have one).

## Running

1. Start the stack from the repo root and wait for the backend to answer:

   ```bash
   docker compose up -d
   curl http://localhost:8080/api/system/date
   ```

2. Run the suite:

   ```bash
   ./run-tests.sh
   ```

   No browser handy? Skip the UI:

   ```bash
   SKIP_UI=true ./run-tests.sh
   API_BASE_URI=http://localhost:8080 ./run-tests.sh
   ```

3. Look at the report:

   ```bash
   mvn -f tests/pom.xml allure:serve
   # or open tests/target/site/allure-maven-plugin/index.html
   ```

`run-tests.sh` health-checks the backend first, resets the business date so the
run starts clean, runs the tests, and prints a pass/fail count with the report
path.

## Layout

```
candidate-project/
├── run-tests.sh                 # one entry point
├── test-plan.md                 # what I tested and why
├── defects.md                   # findings (spec vs. actual)
├── ai-prompt.log                # AI usage log
├── docker-compose.yml           # the app stack (provided)
├── fixtures/                    # sample CSVs (provided)
└── tests/
    ├── pom.xml
    └── src/test/
        ├── java/com/bis/qa/
        │   ├── api/             # API + SFTP tests
        │   ├── ui/              # Selenium tests
        │   ├── clients/         # REST Assured clients (v1, v2, system)
        │   ├── sftp/            # SFTP client + CSV builder
        │   ├── pages/           # AppPage (Selenium locators)
        │   ├── support/         # base classes + shared BondFixture
        │   ├── model/           # BondRecord
        │   ├── util/            # calculator, dates, ISINs, JSON reads
        │   └── config/          # TestConfig
        └── resources/           # test.properties, allure.properties, testng.xml
```

## Config

Everything configurable lives in
[`tests/src/test/resources/test.properties`](tests/src/test/resources/test.properties).
Any value can be overridden with a `-Dkey=value` system property or an env var
of the same name (system property beats env var beats the file). The ones you'll
touch most: `api.baseUri`, `ui.baseUrl`, the `sftp.*` block, `ui.headless`,
`ingestion.pollSeconds`.

## A few decisions worth calling out

- **The suite runs sequentially, on purpose.** The business date is one global
  value in the backend (`PRODUCT.md` §12). If two tests advanced it at once
  they'd trample each other, so I traded parallel speed for a deterministic run
  (`testng.xml`). `ConcurrencyApiTest` is the one exception — it deliberately
  fires real threads to check subscription atomicity.
- **Fixtures are date-relative.** Each test reads the live business date and
  builds its book/maturity dates off that, so it doesn't matter how far an
  earlier test pushed the clock or what today's real date is.
- **The money maths is checked independently.** `FinancialCalculator` recomputes
  the expected coupon/allocation/maturity with `BigDecimal` and is anchored to
  the worked example in §9A. A wrong-but-internally-consistent implementation
  still gets caught.
- **Response reading is deliberately loose.** The exact JSON casing/shape is only
  pinned down by Swagger, so `JsonFields` and the portfolio helpers try a few
  candidate paths. If the live schema differs from what I assumed, those helpers
  are the only thing to touch — the tests don't change.
- **UI locators live in one place.** `AppPage` prefers `data-testid` and falls
  back to visible text, so markup changes hit one file.

See [`test-plan.md`](test-plan.md) for the full strategy and coverage matrix.
