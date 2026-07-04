#!/usr/bin/env bash
#
# Single entry point for the Bond Issuance System QA automation suite.
#
# Assumes the application stack is already running and in a clean state:
#     docker compose up -d
#
# What it does:
#   1. Preflight: verify the backend is reachable (fails fast with guidance).
#   2. Reset the business date so the run starts from a known state.
#   3. Execute the full test suite (API + SFTP + UI) via Maven / TestNG.
#   4. Generate an Allure HTML report and print a summary.
#
# Usage:
#   ./run-tests.sh                 # full suite (API + SFTP + UI)
#   SKIP_UI=true ./run-tests.sh    # API + SFTP only (no browser required)
#   API_BASE_URI=http://host:8080 ./run-tests.sh
#
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TESTS_DIR="${SCRIPT_DIR}/tests"

API_BASE_URI="${API_BASE_URI:-http://localhost:8080}"
UI_BASE_URL="${UI_BASE_URL:-http://localhost:5173}"
SKIP_UI="${SKIP_UI:-false}"
HEALTH_PATH="/api/system/date"

bold() { printf "\033[1m%s\033[0m\n" "$1"; }
info() { printf "  %s\n" "$1"; }
fail() { printf "\033[31mERROR:\033[0m %s\n" "$1" >&2; }

bold "==> Bond Issuance System - QA Automation Suite"
info "API base URI : ${API_BASE_URI}"
info "UI base URL  : ${UI_BASE_URL}"
info "Skip UI      : ${SKIP_UI}"

# ---------------------------------------------------------------------------
# 1. Preflight health check
# ---------------------------------------------------------------------------
bold "==> Preflight: checking that the stack is up"
if ! curl -fsS -m 10 "${API_BASE_URI}${HEALTH_PATH}" >/dev/null 2>&1; then
    fail "Backend not reachable at ${API_BASE_URI}${HEALTH_PATH}"
    fail "Start the stack first:  docker compose up -d"
    fail "(Private images require: docker login interviewmarketnode.azurecr.io)"
    exit 1
fi
info "Backend is reachable."

# ---------------------------------------------------------------------------
# 2. Reset to a known-clean business date
# ---------------------------------------------------------------------------
bold "==> Resetting system business date"
curl -fsS -m 10 -X POST "${API_BASE_URI}/api/system/reset" >/dev/null 2>&1 \
    && info "System reset to today's date." \
    || info "Reset endpoint not available or failed (continuing)."

# ---------------------------------------------------------------------------
# 3. Run the suite
# ---------------------------------------------------------------------------
bold "==> Running test suite"
MVN_ARGS=(
    -f "${TESTS_DIR}/pom.xml"
    -Dapi.baseUri="${API_BASE_URI}"
    -Dui.baseUrl="${UI_BASE_URL}"
)
if [ "${SKIP_UI}" = "true" ]; then
    # Run only the API + SFTP groups (no browser needed).
    MVN_ARGS+=(-Dgroups=api,sftp,smoke,allocation,parity,concurrency)
fi

# testFailureIgnore is set in the POM, so Maven returns 0 even when tests fail;
# defect-revealing failures still appear in the report.
mvn "${MVN_ARGS[@]}" test
MVN_EXIT=$?

# ---------------------------------------------------------------------------
# 4. Generate the Allure report
# ---------------------------------------------------------------------------
bold "==> Generating Allure report"
if mvn -f "${TESTS_DIR}/pom.xml" allure:report >/dev/null 2>&1; then
    REPORT="${TESTS_DIR}/target/site/allure-maven-plugin/index.html"
    info "Allure report: ${REPORT}"
else
    info "Allure report generation skipped/failed."
fi

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
bold "==> Summary"
SUREFIRE_DIR="${TESTS_DIR}/target/surefire-reports"
if [ -d "${SUREFIRE_DIR}" ]; then
    # Aggregate TestNG/surefire XML result counts.
    TOTAL=$(grep -rhoE 'tests="[0-9]+"' "${SUREFIRE_DIR}"/*.xml 2>/dev/null | grep -oE '[0-9]+' | awk '{s+=$1} END {print s+0}')
    FAIL=$(grep -rhoE 'failures="[0-9]+"' "${SUREFIRE_DIR}"/*.xml 2>/dev/null | grep -oE '[0-9]+' | awk '{s+=$1} END {print s+0}')
    ERR=$(grep -rhoE 'errors="[0-9]+"' "${SUREFIRE_DIR}"/*.xml 2>/dev/null | grep -oE '[0-9]+' | awk '{s+=$1} END {print s+0}')
    SKIP=$(grep -rhoE 'skipped="[0-9]+"' "${SUREFIRE_DIR}"/*.xml 2>/dev/null | grep -oE '[0-9]+' | awk '{s+=$1} END {print s+0}')
    info "Tests run : ${TOTAL}"
    info "Failures  : ${FAIL}"
    info "Errors    : ${ERR}"
    info "Skipped   : ${SKIP}"
    info "Surefire reports: ${SUREFIRE_DIR}"
else
    info "No surefire reports found."
fi

info "To view the Allure report locally:  mvn -f tests/pom.xml allure:serve"
exit 0
