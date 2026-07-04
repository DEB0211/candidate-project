# Bond Issuance System — QA Automation Suite

Black-box automated test suite validating the Bond Issuance System (BIS) against
its product specification ([`PRODUCT.md`](PRODUCT.md)). The suite covers the SFTP
ingestion pipeline, both REST API versions, the full bond lifecycle (subscription
→ allocation → coupon → maturity), API v1/v2 parity, concurrency/atomicity, and
the investor web UI.

---

## Tech stack

| Concern | Choice | Why |
|---------|--------|-----|
| Language / build | Java 21, Maven | Matches the JD's Java-first automation expectation and the existing project structure. |
| Test runner | TestNG | Groups, data providers, and fine-grained sequencing control. |
| API testing | REST Assured | Fluent, readable HTTP assertions for v1 (snake_case) and v2 (camelCase). |
| SFTP | JSch (`com.github.mwiede` fork) | Uploads bond CSVs to the ingestion server; modern fork negotiates with `atmoz/sftp`. |
| UI testing | Selenium 4 (Page Object Model) | Selenium Manager auto-resolves the driver; POM keeps locators isolated. |
| Money math | `java.math.BigDecimal` | Independent exact-decimal oracle to verify coupon/allocation/maturity amounts. |
| Reporting | Allure + Surefire | Rich HTML report with steps, severities, and failure screenshots. |

---

## Prerequisites

- **Docker Desktop** running.
- Access to the private image registry used by `docker-compose.yml`:

  ```bash
  docker login interviewmarketnode.azurecr.io
  ```

- **JDK 21+** and **Maven 3.9+** on the `PATH` (`java -version`, `mvn -version`).
- For the UI tests: a **Chrome/Chromium** browser installed (Selenium Manager
  downloads the matching driver automatically).

---

## Running

1. Start the application stack (from the repo root):

   ```bash
   docker compose up -d
   ```

   Wait until the backend answers:

   ```bash
   curl http://localhost:8080/api/system/date
   ```

2. Run the full suite and generate the report:

   ```bash
   ./run-tests.sh
   ```

   Options:

   ```bash
   SKIP_UI=true ./run-tests.sh                 # API + SFTP only (no browser)
   API_BASE_URI=http://localhost:8080 ./run-tests.sh
   ```

3. View the report:

   ```bash
   mvn -f tests/pom.xml allure:serve           # interactive
   # or open tests/target/site/allure-maven-plugin/index.html
   ```

`run-tests.sh` performs a preflight health check, resets the business date for a
clean run, executes the suite, and prints a pass/fail summary plus the report path.

---

## Project structure

```
candidate-project/
├── run-tests.sh                 # single entry point
├── test-plan.md                 # strategy & coverage
├── defects.md                   # findings (actual vs. spec)
├── ai-prompt.log                # AI interaction log
├── docker-compose.yml           # app stack (provided)
├── fixtures/                    # sample CSVs (provided)
└── tests/
    ├── pom.xml
    └── src/test/
        ├── java/com/bis/qa/
        │   ├── api/             # API + SFTP test classes
        │   ├── ui/              # Selenium UI tests
        │   ├── clients/         # REST Assured API clients (v1, v2, system)
        │   ├── sftp/            # SFTP client + CSV builder
        │   ├── pages/           # Selenium page object (AppPage)
        │   ├── support/         # base classes + shared BondFixture
        │   ├── model/           # BondRecord
        │   ├── util/            # FinancialCalculator, DateHelper, IsinGenerator, JsonFields
        │   └── config/          # TestConfig
        └── resources/           # test.properties, allure.properties, testng.xml
```

---

## Configuration

All settings live in [`tests/src/test/resources/test.properties`](tests/src/test/resources/test.properties)
and can be overridden by a JVM system property (`-Dkey=value`) or an environment
variable of the same name (system property > env var > file).

Key settings: `api.baseUri`, `ui.baseUrl`, `sftp.*`, `ui.headless`, `ui.browser`,
`ingestion.pollSeconds`, `business.timezone`.

---

## Architecture decisions & trade-offs

- **Sequential execution.** The business date is a single global singleton in the
  backend (`PRODUCT.md` §12). Advancing it in one test would corrupt state seen by
  a concurrent test, so the suite runs sequentially for deterministic results
  (see [`testng.xml`](tests/src/test/resources/testng.xml)). The dedicated
  `ConcurrencyApiTest` still spins up real threads to probe subscription atomicity.
- **Date-relative fixtures.** Each test reads the live business date and builds
  book/maturity dates relative to it, so tests are independent of how far earlier
  tests advanced the clock and of the real calendar date.
- **Independent money oracle.** `FinancialCalculator` recomputes expected coupon,
  allocation, and maturity amounts with `BigDecimal`, matching the worked example
  in `PRODUCT.md` §9A, rather than trusting the system's own output.
- **Resilient response reading.** Because the exact JSON schema/casing is only
  documented via Swagger, `JsonFields` and the portfolio helpers try multiple
  candidate paths (snake_case/camelCase, flat/nested). If the live schema differs,
  only these helpers need adjusting — not the tests.
- **Isolated UI locators.** `AppPage` centralises Selenium locators (preferring
  `data-testid`, falling back to accessible text) so UI markup changes touch one file.

See [`test-plan.md`](test-plan.md) for the full strategy and coverage matrix.
