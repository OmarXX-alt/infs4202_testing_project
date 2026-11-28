# Automated tests

One command per suite, from a fresh clone. Every test names the designed test case (`TC-...`) and the
requirement it implements in its `@DisplayName`; the test cases are in `docs/test-cases.md`.

**A failing test is a finding, not a broken test.** Expected results come from the Requirements and
`engine/openapi.yaml`; a test fails when the system violates them. Each failure is linked to a GitHub issue in
`docs/test-cases.md`.

**Known-defect tag (Portal).** Team Portal tests that fail because of a reported defect carry
`@Tag(KnownDefect.TAG)` (`known-defect`) and name their issue in the `@DisplayName`, e.g. "(defect #6)". `portal/pom.xml`
excludes this tag by default, so `./mvnw test` and the CI "Tests" workflow stay green and fail only on unexpected
breakage. Run the defect tests on their own with `-Dgroups=known-defect -DexcludedGroups=none`. When a defect is fixed
its test passes; then remove the tag.

Prerequisites: JDK 17 or later (`java -version`). The Maven wrapper downloads Maven and the dependencies on the
first run (internet access needed; see the root README for a proxy).

| Suite | Level | Folder | Command (macOS / Linux) | Needs |
| --- | --- | --- | --- | --- |
| Portal unit and integration (vendor + team) | Unit, service, integration (white-box) | `portal/src/test` | `cd portal && ./mvnw test -Dmaven.test.failure.ignore=true` | nothing running |
| Portal, Module 2 only (passing tests) | Service, integration (white-box, WireMock) | `portal/src/test/.../Module2*` | `cd portal && ./mvnw test -Dtest='Module2*'` | nothing running |
| Portal, known-defect tests (expected to fail) | Service, integration | `portal/src/test` | `cd portal && ./mvnw test -Dgroups=known-defect -DexcludedGroups=none -Dmaven.test.failure.ignore=true` | nothing running |
| Engine contract | API (black-box) | `tests/engine-contract` | `cd tests/engine-contract && ./mvnw test` | Engine running |

Windows: use `.\mvnw.cmd` instead of `./mvnw`, and quote properties: `.\mvnw.cmd test "-Dtest=Module2*"`,
`.\mvnw.cmd test "-Dgroups=known-defect" "-DexcludedGroups=none"`.

## Engine contract suite

1. Start the Engine: `java -jar engine/uniplace-engine.jar` (or `./start.sh`, which also starts the Portal).
   Check it: `curl http://127.0.0.1:8081/api/v1/health`.
2. `cd tests/engine-contract && ./mvnw test`
3. Results: `tests/engine-contract/target/surefire-reports/`.

Another Engine address: `./mvnw test -Dengine.url=http://127.0.0.1:9090/api/v1`.

| Class | Endpoint | Test cases |
| --- | --- | --- |
| `EligibilityContractTest` | `/eligibility` | TC-APP-001 to 009 |
| `MatchContractTest` | `/match` | TC-APP-010 to 012 |
| `RankContractTest` | `/rank` | TC-APP-013 to 016 |
| `ScheduleContractTest` | `/schedule/validate` | TC-INT-001 to 006 |
| `EnginePerformanceTest` | all (PERF-02, p95 of 20 runs) | TC-APP-017 |

`EnginePerformanceTest` takes about a minute because ranking 200 candidates is slow.

## Coverage

`cd portal && ./mvnw test -Dmaven.test.failure.ignore=true`, then open `portal/target/site/jacoco/index.html`.

## Results log

| Date | Suite | Tests | Failed | Notes |
| --- | --- | --- | --- | --- |
| 24/11/2026 | Engine contract | 81 | 29 | 12 distinct contract deviations (Module 2) |
| 24/11/2026 | Portal Module 2 | 27 | 16 | 10 distinct requirement violations |
| 28/11/2026 | Portal default (`./mvnw test`) | 158 | 0 | known-defect tests excluded; CI green |
| 28/11/2026 | Portal known-defect | 16 | 16 | issues #3 to #12 |
