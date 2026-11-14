# 00 — Inventory and scope

## Purpose

This document set reverse-engineers the delivered UniPlace system from the outside in: the Spring Boot Portal source code is read, the prebuilt Engine is treated as a black box, and behaviour is confirmed against the running system. The output is baseline knowledge for test design, not a defect report and not a remediation plan.

**Scope rules applied**

| Rule | How it was applied |
| --- | --- |
| Read-only on `portal/` | No file under `portal/` was edited. All writes are under `docs/reverse-engineering/` plus one row in `docs/ai-usage-log.md`. |
| Engine black box | The Engine JAR was never opened, decompiled, or inspected. Evidence comes from `engine/openapi.yaml`, the Portal's calls, and HTTP probes. |
| No secrets | No credentials, API keys, SMTP passwords, or session identifiers are reproduced. The seeded demonstration password is quoted only because the vendor README already publishes it. |
| Evidence | Claims carry a `path/File.java:lineStart-lineEnd` or a `Class#method` reference. Inferences are marked **[INFERRED]**; unknowns are marked **[UNKNOWN]**. |
| Defects | Suspicious behaviour is recorded as a hypothesis in `07-hypotheses.md`, not as a filed defect. |

## System inventory

| Path | Type | What it contains | Evidence |
| --- | --- | --- | --- |
| `portal/` | Source tree | Java 17 / Spring Boot 3.3.5 Portal, 89 main Java files, 25 test Java files, 26 Thymeleaf templates, 3 JavaScript files, Maven wrapper. | `portal/pom.xml`; file counts from the tree |
| `portal/src/main/java/` | Source | `config`, `domain`, `repository`, `service`, `engine`, `ai`, `security`, `web` packages. | package tree |
| `portal/src/main/resources/templates/` | Views | Server-rendered Thymeleaf pages for login, dashboards, postings, applications, coordinator, student, recruiter and account functions. | `portal/src/main/resources/templates/**` |
| `portal/src/main/resources/static/` | Assets | `app.js`, `letter.js`, `settings.js`, `app.css`. | `portal/src/main/resources/static/**` |
| `portal/src/test/java/` | Tests | 24 test classes plus `IntegrationTest` base; 147 executed tests. | Surefire reports |
| `bin/uniplace-portal.jar` | Binary | Prebuilt Portal; not tracked in Git. | `README.md:13` |
| `engine/uniplace-engine.jar` | Binary | Matching and Compliance Engine; treated as black box. | `README.md:13` |
| `engine/openapi.yaml` | Contract | Six endpoints under `/api/v1` and request/response schemas. | `engine/openapi.yaml:10-247` |
| `start.cmd`, `start.sh` | Scripts | Start Engine on `127.0.0.1:8081` and Portal on `127.0.0.1:8080`; `--reset`, `--jar`. | `start.cmd:1-85`; `README.md:34-63` |
| `ai-eval/` | Data | Fictional motivation letters and reference score bands for AI evaluation. | `README.md:16` |
| `.github/workflows/tests.yml` | CI | Runs tests with `-Dmaven.test.failure.ignore=true`, uploads reports, then fails on XML `<failure>`/`<error>`. | `.github/workflows/tests.yml:1-32` |
| `docs/` | Documentation | Team starting points: strategy, test cases, AI usage log. | `docs/*.md` |

## Technology baseline

| Area | Choice | Evidence |
| --- | --- | --- |
| Language / runtime target | Java 17 | `portal/pom.xml` (`java.version`) |
| Framework | Spring Boot 3.3.5 | `portal/pom.xml` |
| Web | Spring MVC + Thymeleaf | `portal/pom.xml`; `templates/` |
| Persistence | Spring Data JPA + Hibernate 6.5.3 + H2 file database | `portal/src/main/resources/application.properties:6-11` |
| Security | Spring Security form login, BCrypt | `portal/src/main/java/com/halcyon/uniplace/config/SecurityConfig.java:17-57` |
| Mail | Spring Mail, optional SMTP | `portal/src/main/java/com/halcyon/uniplace/service/NotificationService.java:42-68` |
| Tests | JUnit 5, Spring Boot Test, Spring Security Test, WireMock 3.9.1, JaCoCo 0.8.12 | `portal/pom.xml` |
| Build | Maven wrapper | `portal/mvnw`, `portal/mvnw.cmd` |

## Functional surface

The Portal has three primary modules plus cross-cutting administration:

| Module | Main concerns | Primary classes |
| --- | --- | --- |
| Student | Profile, documents, posting search, applications, offers, interviews, notifications | `StudentController`, `ApplicationController`, `PostingController`, `DocumentService`, `ApplicationService`, `OfferService` |
| Recruiter | Company registration, postings, candidates, shortlisting, interviews, offers | `CompanyController`, `PostingController`, `ApplicationController`, `PostingService`, `RankingService`, `InterviewService`, `OfferService` |
| Coordinator | Students, companies, users, all applications, reports, settings, audit, outbox, test data | `CoordinatorController`, `SettingsController`, `CompanyService`, `UserService`, `ReportService`, `TestDataService` |
| AI assistant | Letter feedback and interview questions | `AiAssistantService`, `AiClient`, `PromptBuilder`, `AiResponseParser`, `FeedbackCache`, `SimulatedProvider` |
| Engine integration | Eligibility, match score, ranking, interview slot validation, allowance calculation | `EngineClient`, `EngineRequestMapper`, `MatchScoreService`, `RankingService`, `InterviewService`, `OfferService` |

## Requirement identifiers found in source

The source comments use requirement IDs as traceability anchors. They were not supplied with a separate Requirements document in this checkout, so the identifiers below are the only requirement-level evidence available.

| Prefix | Domain | Example references |
| --- | --- | --- |
| APP | Application lifecycle | `ApplicationService.java:87-92` (APP-02), `:134-181` (APP-01–APP-05), `:219-230` (APP-08), `:248-259` (APP-06) |
| POS | Postings | `PostingService.java:25-26` (POS-07), `:50-55` (POS-03/POS-04), `:68-82` (POS-06), `:174-180` (POS-03) |
| OFF | Offers and allowance | `OfferService.java:80-83` (OFF-02), `:131-164` (OFF-03/OFF-04), `:183-191` (OFF-05/OFF-07), `:237-256` (OFF-02) |
| INT | Interviews | `InterviewService.java:23`, `:111-129` (INT-04) |
| STU | Student records and documents | `StudentService.java:57-67` (STU-02), `:69-82` (STU-02), `DocumentService.java:28` (STU-04/SEC-06) |
| COM | Companies | `CompanyService.java` class, `ApplicationService.java:261-266` (COM-02) |
| USR | Accounts and access | `AccessGuard.java:12` (USR-05), `UserService.java:30-33` (USR-03) |
| ADM | Administration | `SettingsController.java:24` (ADM-01/ADM-03), `ReportService.java:21` (ADM-02), `DashboardService.java:17` (ADM-04), `TestDataService.java:31-34` (ADM-05) |
| AI | Assistant | `PostingController.java:96` (AI-01/AI-05), `ApplicationController.java:217` (AI-02) |
| NOT | Notifications | `CoordinatorController.java:230` (NOT-03), `OfferService.java:258` (NOT-04) |
| REL | Release/data | `DataSeeder.java:62` (REL-04) |
| SEC | Security | `DocumentService.java:28` (SEC-06) |

## Reverse-engineering method

1. Static read of build files, configuration, all main Java sources, all templates, and all test sources.
2. Test execution with failure-ignore so JaCoCo always writes a report: `.\mvnw.cmd -B test "-Dmaven.test.failure.ignore=true"` in `portal/`.
3. Coverage extraction from `portal/target/site/jacoco/jacoco.csv`.
4. Fresh runtime baseline with `start.cmd --jar --reset`; Portal and Engine start-up, seed time and data counts captured.
5. Authenticated HTTP probes as coordinator, student, recruiter, pending recruiter, and recruiter without a company.
6. Direct Engine probes for all six endpoints, including valid, boundary and invalid requests.
7. Restoration of the original H2 database and shutdown of both processes.

## Coverage of requested areas

| Area | Covered in | Status |
| --- | --- | --- |
| Inventory and stack | this file | Complete |
| Architecture and runtime topology | `01-architecture.md` | Complete |
| Domain model and persistence | `02-domain-model.md` | Complete |
| Business rules and state machine | `03-business-rules.md` | Complete |
| HTTP surface and Engine contract | `04-http-and-engine-api.md` | Complete |
| Authentication, authorisation, security | `05-security.md` | Complete |
| Configuration, data, build, CI | `06-configuration-and-data.md` | Complete |
| Suspicious behaviour as hypotheses | `07-hypotheses.md` | Complete |
| Test/coverage and runtime baseline | `08-runtime-baseline.md` | Complete |
| Executive summary | `09-summary.md` | Complete |

**Deliberately not done:** no decompilation of `uniplace-engine.jar`; no source changes; no penetration testing beyond authenticated role/ownership probes; no load or soak testing; no inspection of real personal data (the seeded data is fictional).

## Deliverables

| File | Purpose |
| --- | --- |
| `00-inventory.md` | Scope, inventory, method, coverage |
| `01-architecture.md` | Components, request flows, runtime topology |
| `02-domain-model.md` | Entities, relationships, schema, lifecycle |
| `03-business-rules.md` | Rules, constants, transitions, validation |
| `04-http-and-engine-api.md` | Portal routes, Engine contract, observed deviations |
| `05-security.md` | Authentication, authorisation, access-control analysis |
| `06-configuration-and-data.md` | Properties, defaults, seeding, storage, CI |
| `07-hypotheses.md` | Suspicious behaviours and confirmation methods |
| `08-runtime-baseline.md` | Tests, coverage, start-up, probe results |
| `09-summary.md` | Nine-hundred-word maximum summary |

## Open questions

- The Requirements document referenced by `README.md:5` is not in the repository; requirement wording beyond source comments is **[UNKNOWN]**.
- `bin/uniplace-portal.jar` is present locally but Git-ignored; whether it matches the current source commit is **[UNKNOWN]**.
- No production deployment topology, backup policy, or operating-system hardening guidance is supplied.
- The intended distinction between "active" application states and the states counted by `ApplicationService.countActive` is not documented outside the code.
- The expected behaviour when the Engine is unavailable is stated in code comments but not in a formal requirement; whether fail-open eligibility is accepted by the customer is **[UNKNOWN]**.
