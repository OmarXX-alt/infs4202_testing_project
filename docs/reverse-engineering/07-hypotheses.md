# 07 — Hypotheses and suspicious behaviour

## How to read this file

This is a **hypothesis register**, not a defect report. It records behaviour that looks inconsistent with the visible code, the Engine contract, or the stated business rules, and gives a way to confirm each item at runtime. Nothing here was filed as a bug.

| Status | Meaning |
| --- | --- |
| **Confirmed** | Reproduced against the running Portal or Engine during this baseline. |
| **Source-evident** | The code path is unambiguous; no runtime exploit was attempted. |
| **Config-evident** | A configuration value permits the behaviour; no incident was induced. |
| **Contract-tested** | Direct request to the black-box Engine showed a deviation from `openapi.yaml`. |
| **Test-evident** | Found while reading or running the test suite. |

Impact is a rough triage aid: **High** = data exposure, financial error, or workflow blocked; **Medium** = wrong result or bypass under realistic conditions; **Low** = hygiene, usability, or narrow edge case.

## Register summary

| ID | Area | Hypothesis | Status | Impact |
| --- | --- | --- | --- | --- |
| H-01 | Access | Any recruiter can download any student document | Confirmed | High |
| H-02 | Access | Any authenticated user can view any application | Source-evident | High |
| H-03 | Access | Any recruiter can edit any company's posting | Source-evident | High |
| H-04 | Access | Settings endpoints accept cross-site POSTs | Confirmed | Medium |
| H-05 | Access | Custom logout leaves the server session valid | Source-evident | Medium |
| H-06 | Access | Company name is a stored-XSS sink | Source-evident | Medium |
| H-07 | Access | Secret setting values are written to the log | Source-evident | Medium |
| H-08 | Access | Error pages disclose stack traces and messages | Config-evident | Medium |
| H-09 | Access | Student PII and raw letters go to an external AI provider | Source-evident | Medium |
| H-10 | Access | Change-password policy is weaker than account creation | Source-evident | Low |
| H-11 | Applying | Withdrawn applications still count against the active limit | Source-evident | Medium |
| H-12 | Applying | Applications are refused on the deadline day itself | Source-evident | Medium |
| H-13 | Offers | Response deadline is truncated to the start of a day | Source-evident | Medium |
| H-14 | Offers | Seat check and increment are not atomic | Source-evident | High |
| H-15 | Students | Academic-record edits do not recompute standing | Source-evident | Medium |
| H-16 | Notifications | A mail failure rolls back the business action | Source-evident | Medium |
| H-17 | Audit | Same user/action within one second is silently dropped | Source-evident | Low |
| H-18 | Shortlisting | Score equal to the threshold needs an override | Source-evident | Low |
| H-19 | Ranking | More than 500 candidates blocks the whole ranking view | Source-evident | Medium |
| H-20 | Engine call | Eligibility uses the global default minimum GPA | Source-evident | High |
| H-21 | Engine call | `priorInternships` is never sent to the eligibility check | Source-evident | Medium |
| H-22 | Engine call | Eligibility fails open when the Engine is down | Source-evident | High |
| H-23 | Engine call | Match score fails open as zero when the Engine is down | Source-evident | Medium |
| H-24 | Engine call | Match-score cache key ignores student skills | Source-evident | High |
| H-25 | Engine call | Eligibility document cache is never invalidated | Source-evident | Medium |
| H-26 | Engine call | Every Engine failure becomes "unavailable" | Source-evident | Medium |
| H-27 | Engine call | `EngineRejectedException` is never thrown | Source-evident | Low |
| H-28 | Engine contract | Missing required fields return 500, not 400 | Contract-tested | High |
| H-29 | Engine contract | GPA range and standing enum are not validated | Contract-tested | High |
| H-30 | Engine contract | Undocumented bounds exist for internships and seats | Contract-tested | Low |
| H-31 | Engine contract | Transcript validity window is 179 days, not 180 | Contract-tested | Medium |
| H-32 | Engine contract | Match normalisation differs from the contract | Contract-tested | Medium |
| H-33 | Engine contract | Missing `niceToHaveSkills` awards free points | Contract-tested | Medium |
| H-34 | Engine contract | Rank ignores the documented GPA tie-break | Contract-tested | Medium |
| H-35 | Engine contract | Schedule boundaries differ from the contract | Contract-tested | Medium |
| H-36 | Engine contract | Allowance boundaries differ from the contract | Contract-tested | Medium |
| H-37 | AI | Feedback cache key ignores the letter text | Source-evident | Medium |
| H-38 | AI | Parser does not enforce item counts | Source-evident | Low |
| H-39 | AI | Letter counter script throws on some browsers | Source-evident | Low |
| H-40 | AI | Prompt hard-codes pay and duration | Source-evident | Low |
| H-41 | Storage | Uploads go to a new temp folder on every start | Source-evident | High |
| H-42 | Settings | Three settings are cached forever in static fields | Source-evident | Medium |
| H-43 | Reporting | Average-score report performs N+1 queries | Source-evident | Medium |
| H-44 | Reporting | Days-to-offer includes pending and declined offers | Source-evident | Low |
| H-45 | Reporting | CSV cells are not protected against formula injection | Source-evident | Low |
| H-46 | Tests | `DataSeeder` has 0% line coverage | Test-evident | Medium |
| H-47 | Tests | Web controllers have very low coverage | Test-evident | Medium |
| H-48 | Tests | Engine validation-error test asserts the wrong behaviour | Test-evident | Low |
| H-49 | Tests | Match-score test only repeats one skill set | Test-evident | Low |
| H-50 | CI | Pipeline does not start the Engine or run the JAR | Test-evident | Medium |
| H-51 | Tests | `@MockBean EngineClient` hides contract drift | Test-evident | Medium |

## A. Access and security

### H-01 — Any recruiter can download any student document

- **Reason:** the download guard allows the whole `RECRUITER` role with no company, posting, or application relationship check.
- **Evidence:** `StudentController.java:99-105`.
- **Confirm at runtime:** log in as `recruiter.qtech`, `recruiter.desertbloom` (pending company), and `recruiter.newco` (no company); request `GET /documents/1` through `/documents/4`. All returned 200 `application/pdf` in this baseline; the owner student received 403 for documents 3–4.
- **Impact:** every uploaded CV and transcript is readable by any recruiter account, including unapproved and company-less recruiters.

### H-02 — Any authenticated user can view any application

- **Reason:** `ApplicationController.view` loads by id and renders without calling `AccessGuard`.
- **Evidence:** `ApplicationController.java:97-104`; contrast the guarded write paths at `:119-256`.
- **Confirm at runtime:** as `student.omar`, request `GET /applications/{id}` for one of `student.amal`'s applications; look for name, GPA, letter, interviews, and offer terms in the response.
- **Impact:** horizontal disclosure of motivation letters, academic data, and offer terms to any logged-in student, recruiter, or coordinator.

### H-03 — Any recruiter can edit any company's posting

- **Reason:** `POST /recruiter/postings/{id}` calls `postings.update(id, form)` without `guard.assertRecruiterOwns`, and the service has no user parameter.
- **Evidence:** `PostingController.java:160-171`; `PostingService.java:136-146`; guarded siblings at `:149-158`, `:173-175`, `:185-187`.
- **Confirm at runtime:** as `recruiter.qtech`, submit a valid edit form to a Gulf Bank draft posting id while it is still editable.
- **Impact:** cross-company tampering with title, description, skills, GPA/credit thresholds, seats, dates, hours, and allowance.

### H-04 — Settings endpoints accept cross-site POSTs

- **Reason:** `/coordinator/settings/**` is excluded from CSRF protection.
- **Evidence:** `SecurityConfig.java:25`; `SettingsController.java:89-130`.
- **Confirm at runtime:** with a coordinator session, `POST /coordinator/settings` without `_csrf`; observed 302 to the settings page instead of 403.
- **Impact:** a logged-in coordinator who visits a hostile page can have the Engine URL, AI key, SMTP credentials, or limits changed.

### H-05 — Custom logout leaves the server session valid

- **Reason:** the UI posts to `/account/logout`, which only expires the browser cookie.
- **Evidence:** `AccountController.java:51-59`; `layout.html:42`; Spring Security logout at `SecurityConfig.java:50-54` is not used by the UI.
- **Confirm at runtime:** capture `JSESSIONID`, call `/account/logout`, then replay the cookie against `/dashboard` before the 30-minute timeout.
- **Impact:** a stolen or cached session may remain usable after the user believes they logged out.

### H-06 — Company name is a stored-XSS sink

- **Reason:** the posting list renders the company name with `th:utext` (unescaped).
- **Evidence:** `postings/list.html:29`; company validation at `CompanyService.java:114-130` only checks non-blank and uniqueness.
- **Confirm at runtime:** register a company named `<img src=x onerror=alert(1)>`, approve it as coordinator, then open `/postings` as a student.
- **Impact:** script execution in every student/recruiter/coordinator session that views the list; could drive the CSRF-exempt settings endpoint.

### H-07 — Secret setting values are written to the log

- **Reason:** `SettingsController.save` logs old and new values in clear text, although the form and audit record are masked.
- **Evidence:** `SettingsController.java:113-119`, `:160-165`.
- **Confirm at runtime:** set `ai.apiKey` to a canary value, then search the application log for it.
- **Impact:** credential disclosure to anyone with log access; secrets also persist in `SettingEntry` unencrypted (`SettingsService.java:84-95`).

### H-08 — Error pages disclose stack traces and messages

- **Reason:** all three error-detail switches are enabled.
- **Evidence:** `application.properties:19-22`.
- **Confirm at runtime:** trigger an unhandled exception (for example, a numeric overflow or malformed binding) and inspect the HTML response.
- **Impact:** internal class names, SQL fragments, and file paths may reach the browser.

### H-09 — Student PII and raw letters go to an external AI provider

- **Reason:** the prompt includes full name, student number, GPA, posting, and the unescaped letter; the letter is concatenated into the system message.
- **Evidence:** `PromptBuilder.java:42-48`, `:34-40`.
- **Confirm at runtime:** configure a local mock AI endpoint, submit a letter containing `Ignore previous instructions`, and inspect the outbound request.
- **Impact:** privacy exposure and prompt injection; the provider response is only parsed as JSON, but injected text can steer scores and questions.

### H-10 — Change-password policy is weaker than account creation

- **Reason:** account creation requires ≥10 characters and a digit; the change path only requires ≥10 characters.
- **Evidence:** `UserService.java:18-20`, `:54-68` versus `:70-80`.
- **Confirm at runtime:** change a password to ten letters with no digit; observed acceptance path in code.
- **Impact:** users can downgrade their own password below the stated policy.

## B. Application, offer, and posting behaviour

### H-11 — Withdrawn applications still count against the active limit

- **Reason:** `countActive` excludes only `REJECTED` and `ACCEPTED`, while `ApplicationState.ACTIVE` excludes `WITHDRAWN` too.
- **Evidence:** `ApplicationService.java:87-92`; `ApplicationState.java:9-13`.
- **Confirm at runtime:** as a student with four withdrawn applications and one submitted, try a sixth application; expect refusal even though only one is active.
- **Impact:** students are blocked earlier than the documented active-application limit.

### H-12 — Applications are refused on the deadline day itself

- **Reason:** the check is `today.isBefore(deadline)`, although the comment says "until the end of the deadline day".
- **Evidence:** `PostingService.java:50-55`.
- **Confirm at runtime:** use seeded posting `p2`, whose deadline is `today` (`DataSeeder.java:162-163`), and submit an application with a valid letter.
- **Impact:** the last day of every posting is unusable; the seeded data contains an immediately demonstrative case.

### H-13 — Response deadline is truncated to the start of a day

- **Reason:** `issuedAt.toLocalDate().plusDays(hours / 24).atStartOfDay()` discards the issue time and truncates any non-multiple-of-24 window.
- **Evidence:** `OfferService.java:80-83`.
- **Confirm at runtime:** issue an offer at 23:30 with the default 72 hours and read the displayed deadline; it is three calendar days at 00:00, not 72 hours later.
- **Impact:** students lose up to almost a full day, and a non-24-multiple setting (for example 36 hours) silently becomes one day.

### H-14 — Seat check and increment are not atomic

- **Reason:** `accept` reads the posting through the application, checks `seatsLeft()`, then increments, without a row lock; two concurrent accepts can both pass.
- **Evidence:** `OfferService.java:134-155`; no `findByIdForUpdate` in the path.
- **Confirm at runtime:** issue two pending offers for a one-seat posting and accept both concurrently (two sessions or a parallel HTTP script).
- **Impact:** overbooking; `seatsTaken` can exceed `seats`, and the posting may still not become `FILLED` correctly.

### H-15 — Academic-record edits do not recompute standing

- **Reason:** `updateAcademicRecord` sets GPA, credits, internships, and program but never adjusts `standing`.
- **Evidence:** `StudentService.java:69-82`; `Student` derives/stores standing elsewhere.
- **Confirm at runtime:** lower a good-standing student's GPA below the probation threshold through `/coordinator/students/{id}`, then check the dashboard limit.
- **Impact:** limits and eligibility continue to use the old standing until another path recalculates it.

### H-16 — A mail failure rolls back the business action

- **Reason:** `notify` writes the notification and synchronously calls SMTP inside the same transaction; a send failure throws `BusinessRuleException`.
- **Evidence:** `NotificationService.java:35-40`, `:42-68`.
- **Confirm at runtime:** set an unreachable SMTP host and submit an application; the application and its notification should roll back.
- **Impact:** configuring a broken mail server blocks applications and other notified actions instead of degrading to Outbox-only.

### H-17 — Same user/action within one second is silently dropped

- **Reason:** the audit dedupe key is `username|action|truncated-second`, ignoring target and values.
- **Evidence:** `AuditService.java:27-36`.
- **Confirm at runtime:** perform two shortlist overrides on different applications within one second; only one audit row appears.
- **Impact:** legitimate audit evidence is lost; two distinct targets in the same second collide.

### H-18 — Score equal to the threshold needs an override

- **Reason:** the override flag uses `<= threshold`, so a score exactly at the configured cut-off is treated as below it.
- **Evidence:** `ApplicationService.java:188-193`.
- **Confirm at runtime:** set `shortlist.threshold` to a student's exact score and shortlist without justification.
- **Impact:** a boundary score is refused or requires an unnecessary justification and audit entry.

### H-19 — More than 500 candidates blocks the whole ranking view

- **Reason:** the ranking service throws instead of paging or truncating.
- **Evidence:** `RankingService.java:25`, `:47-50`.
- **Confirm at runtime:** generate more than 500 active applications on one posting through `/coordinator/tools/generate`, then open the candidates page.
- **Impact:** recruiters cannot rank a large posting at all; the UI has no fallback.

## C. Engine integration

### H-20 — Eligibility uses the global default minimum GPA

- **Reason:** the mapper sends `settings.defaultMinGpa` rather than `posting.getMinGpa()`.
- **Evidence:** `EngineRequestMapper.java:37-45`, especially `:43`; posting thresholds are set at `PostingService.java:195-198`.
- **Confirm at runtime:** create a posting with `minGpa=3.00`, apply as a student with GPA 2.50, and compare the Portal decision with a direct `/eligibility` call using 3.00.
- **Impact:** students below a posting-specific GPA requirement can be declared eligible, or the reverse when the global default is stricter.

### H-21 — `priorInternships` is never sent to the eligibility check

- **Reason:** the request includes GPA, credits, standing, minimums, date, and documents but not the student's prior-internship count, although the contract accepts it.
- **Evidence:** `EngineRequestMapper.java:37-55`; `openapi.yaml` eligibility schema.
- **Confirm at runtime:** set a student's prior internships above a threshold that the Engine uses, then compare Portal and direct Engine decisions.
- **Impact:** any Engine rule based on prior internships is silently bypassed.

### H-22 — Eligibility fails open when the Engine is down

- **Reason:** `checkEligibility` catches `EngineUnavailableException` and returns `EligibilityResult.ok()`.
- **Evidence:** `EngineClient.java:87-94`.
- **Confirm at runtime:** stop the Engine, submit an application as an ineligible student, and observe success.
- **Impact:** the compliance check is optional in practice; an outage admits anyone.

### H-23 — Match score fails open as zero when the Engine is down

- **Reason:** `match` catches the same exception and returns score 0 with empty lists.
- **Evidence:** `EngineClient.java:96-102`.
- **Confirm at runtime:** stop the Engine and submit an application; the stored score is 0 and shortlisting then requires an override.
- **Impact:** transient outages permanently lower stored scores and distort ranking and reports.

### H-24 — Match-score cache key ignores student skills

- **Reason:** `key` includes only the posting id and the posting's required skills; the student's skills are not part of the key.
- **Evidence:** `MatchScoreService.java:44-51`, used at `:25-33`.
- **Confirm at runtime:** submit two applications to the same posting from students with very different skills and compare stored scores.
- **Impact:** the first student's score is served to every later applicant for that posting, until the posting is edited or the JVM restarts.

### H-25 — Eligibility document cache is never invalidated

- **Reason:** `forEligibility` caches a student's document list in a map; `clearCache` has no caller, and the upload path does not refresh or evict the entry.
- **Evidence:** `DocumentService.java:40-41`, `:55-63`, `:65-107`; no call to `clearCache` elsewhere in `src/main/java`.
- **Confirm at runtime:** upload a replacement transcript, then submit an application; compare the documents sent in a mock Engine request with the current database rows.
- **Impact:** eligibility may use stale document metadata, including an expired transcript or a superseded issue date.

### H-26 — Every Engine failure becomes "unavailable"

- **Reason:** `post` catches all `RestClientException`s and throws `EngineUnavailableException`.
- **Evidence:** `EngineClient.java:70-85`.
- **Confirm at runtime:** return a 400 validation body from a mock Engine; the Portal reports the Engine as unavailable.
- **Impact:** operators cannot distinguish a contract/validation error from an outage; the fail-open paths in H-22/H-23 then hide the error.

### H-27 — `EngineRejectedException` is never thrown

- **Reason:** the exception type exists but no call site throws it.
- **Evidence:** `engine/EngineRejectedException.java`; search of `engine/` shows only the declaration.
- **Confirm at runtime:** static search is sufficient; no runtime path reaches it.
- **Impact:** dead code suggests an intended error path (reject versus unavailable) that was never wired.

### H-28 — Missing required fields return 500, not 400

- **Reason:** the contract promises 400 with `{error, field, message}` for validation failures and no 500 for well-formed JSON.
- **Evidence:** `engine/openapi.yaml:130-140`; direct probes missing `gpa`, `creditsCompleted`, `standing`, or `asOf`.
- **Confirm at runtime:** `POST /api/v1/eligibility` with a body missing `gpa`; observed HTTP 500.
- **Impact:** the Portal maps 500 to "unavailable", so a caller error looks like an outage and may trigger fail-open behaviour.

### H-29 — GPA range and standing enum are not validated

- **Reason:** `gpa 9` and `standing "good"`/`"BAD"` were accepted, although the contract constrains GPA to 0–4 and standing to an enum.
- **Evidence:** `engine/openapi.yaml` eligibility schema; direct probes.
- **Confirm at runtime:** send `gpa: 9` and `standing: "BAD"`; both returned a normal eligibility result.
- **Impact:** malformed or malicious values can produce nonsensical decisions instead of a contract error.

### H-30 — Undocumented bounds exist for internships and seats

- **Reason:** `priorInternships` above 100 and `seats` above 1000 were rejected, but the contract does not state these limits.
- **Evidence:** direct probes; `engine/openapi.yaml` rank/eligibility schemas.
- **Confirm at runtime:** send `priorInternships: 101` and `seats: 1001`.
- **Impact:** a valid-looking request can fail for an undocumented reason, and the Portal surfaces it as an outage.

### H-31 — Transcript validity window is 179 days, not 180

- **Reason:** a transcript issued exactly 180 days before the as-of date was rejected; 179 days was accepted.
- **Evidence:** direct `/eligibility` probes on 2026-11-14; seeded `noor` transcript is `today.minusDays(179)` (`DataSeeder.java:196-197`).
- **Confirm at runtime:** submit two eligibility requests with `issueDate = asOf-180` and `asOf-179`.
- **Impact:** the documented 180-day rule is off by one; the seeded borderline student may flip depending on the run date.

### H-32 — Match normalisation differs from the contract

- **Reason:** the contract says matching ignores case and surrounding spaces; the Engine treated `"Java"`, `"java"`, and `" Java "` differently.
- **Evidence:** direct `/match` probes; `engine/openapi.yaml` match description.
- **Confirm at runtime:** send the same skill in different casing/spacing and compare scores.
- **Impact:** scores depend on how a student typed a skill, and the Portal cache (H-24) amplifies the first spelling seen.

### H-33 — Missing `niceToHaveSkills` awards free points

- **Reason:** omitting the field produced a higher score than sending an empty array.
- **Evidence:** direct `/match` probes.
- **Confirm at runtime:** compare `{"niceToHaveSkills":[]}` with the field omitted.
- **Impact:** non-conforming clients get inflated scores; the Portal always sends the field, so this is mainly a contract/robustness issue.

### H-34 — Rank ignores the documented GPA tie-break

- **Reason:** the contract lists score, then GPA, then submission time; equal-score probes were ordered by score, time, and id instead.
- **Evidence:** direct `/rank` probes; `engine/openapi.yaml` rank description.
- **Confirm at runtime:** send two candidates with equal score and different GPA/submission times.
- **Impact:** the wrong candidate can be above the seat cut when scores tie.

### H-35 — Schedule boundaries differ from the contract

- **Reason:** a slot of exactly 120 minutes was rejected, ends after 18:00 were accepted, and adjacent slots were reported as overlapping.
- **Evidence:** direct `/schedule/validate` probes; `engine/openapi.yaml` schedule constraints.
- **Confirm at runtime:** validate 09:00–11:00, 17:00–18:01, and 11:00–12:00 after a 10:00–11:00 booking.
- **Impact:** valid interviews are blocked or invalid evening interviews are allowed; the Portal surfaces these as scheduling failures.

### H-36 — Allowance boundaries differ from the contract

- **Reason:** no transport supplement was paid at exactly 30 hours (paid at 31), and a cap adjustment of +300 was ignored when the request already exceeded the cap.
- **Evidence:** direct `/allowance` probes; `engine/openapi.yaml` allowance formula.
- **Confirm at runtime:** compare `weeklyHours: 30` with `31`, and `monthlyAllowance: 5000` plus cap 5000 with a cap adjustment.
- **Impact:** off-by-one underpayment and an unenforced cap adjustment; statements disagree with policy.

## D. AI assistant

### H-37 — Feedback cache key ignores the letter text

- **Reason:** `key` returns only `studentId|postingId`; the letter parameter is unused.
- **Evidence:** `FeedbackCache.java:29-39`, `:45-47`; `sha256` is defined but not used in the key.
- **Confirm at runtime:** request feedback for two different letters for the same student and posting within 10 minutes; the second response repeats the first.
- **Impact:** edited letters receive stale feedback, misleading students and hiding prompt changes.

### H-38 — Parser does not enforce item counts

- **Reason:** `list` ignores its `min`/`max` parameters and returns every array element.
- **Evidence:** `AiResponseParser.java:59-69`; callers at `:29-30`, `:35`.
- **Confirm at runtime:** return a mock AI response with zero or ten strengths and inspect the rendered page.
- **Impact:** empty or oversized lists reach the UI; the "exactly three strengths" instruction is not enforced.

### H-39 — Letter counter script throws on some browsers

- **Reason:** `navigator.userAgentData.mobile` is read without checking that `userAgentData` exists.
- **Evidence:** `static/js/letter.js:13`.
- **Confirm at runtime:** open a posting as a student in Firefox or Safari and watch the console; the character counter should fail.
- **Impact:** the counter and client-side length hint stop working on browsers without the API.

### H-40 — Prompt hard-codes pay and duration

- **Reason:** the system prompt says the internship lasts 3 months and pays QAR 3,000, regardless of the actual posting.
- **Evidence:** `PromptBuilder.java:13`; posting values vary (`DataSeeder.java:160-187`).
- **Confirm at runtime:** request feedback for a 6-month, QAR 5,000 posting and inspect the prompt sent to the mock provider.
- **Impact:** AI feedback is judged against the wrong terms; scores are not comparable across postings.

## E. Storage, settings, reporting

### H-41 — Uploads go to a new temp folder on every start

- **Reason:** `DocumentService` takes only the configured path's file name and calls `Files.createTempDirectory` at construction.
- **Evidence:** `DocumentService.java:43-48`, `:109-116`; `application.properties:26`.
- **Confirm at runtime:** upload a CV, restart the Portal, and try to download the same document; the database row remains but the file is gone.
- **Impact:** uploaded documents are effectively ephemeral, and seeded documents disappear after a restart while their rows persist.

### H-42 — Three settings are cached forever in static fields

- **Reason:** `maxActiveGood`, `maxActiveProbation`, and `shortlistThreshold` are static volatile fields filled on first use.
- **Evidence:** `SettingsService.java:101-124`.
- **Confirm at runtime:** change `limits.active.good` from 5 to 3 in the UI, then submit a sixth application; the old limit still applies until restart.
- **Impact:** coordinator changes appear saved but do not take effect; behaviour depends on process lifetime.

### H-43 — Average-score report performs N+1 queries

- **Reason:** for each company it loads postings, then loads applications per posting.
- **Evidence:** `ReportService.java:52-71`; repository calls at `:55`, `:58-59`.
- **Confirm at runtime:** enable SQL logging and open `/coordinator/reports`; count the queries as companies/postings grow.
- **Impact:** report time grows multiplicatively; large generated datasets make the page slow.

### H-44 — Days-to-offer includes pending and declined offers

- **Reason:** the report iterates every offer, not only issued or accepted ones, and divides by all offers.
- **Evidence:** `ReportService.java:82-93`.
- **Confirm at runtime:** decline a pending offer and compare `days.csv` before and after.
- **Impact:** the average is diluted by offers that were never accepted, so the metric does not measure time-to-placement.

### H-45 — CSV cells are not protected against formula injection

- **Reason:** CSV escaping handles commas, quotes, and newlines but not leading `=`, `+`, `-`, or `@`.
- **Evidence:** `ReportService.java:109-128`.
- **Confirm at runtime:** register a company named `=cmd|' /C calc'!A0`, approve it, export `scores.csv`, and open it in a spreadsheet.
- **Impact:** a coordinator opening an export may execute a formula; combined with H-06 this is a plausible attack chain.

## F. Tests and CI

### H-46 — `DataSeeder` has 0% line coverage

- **Reason:** tests disable seeding (`uniplace.seed.enabled=false`) and no test exercises `DataSeeder`.
- **Evidence:** `portal/src/test/resources/application.properties:6`; JaCoCo CSV row for `DataSeeder` (0/264 lines).
- **Confirm at runtime:** inspect `portal/target/site/jacoco/jacoco.csv`.
- **Impact:** the 466-line seeding class, including its many fixed states, is entirely unverified; regressions appear only at first start.

### H-47 — Web controllers have very low coverage

- **Reason:** most controller logic is exercised only through a few integration tests; several controllers are below 20% line coverage.
- **Evidence:** JaCoCo CSV: `CoordinatorController` 13/99, `PostingController` 11/94, `ApplicationController` 11/119, `StudentController` 8/43, `HomeController` 13/43, `SettingsController` 32/95.
- **Confirm at runtime:** inspect the JaCoCo CSV or HTML report.
- **Impact:** the access-control and CSRF findings above sit in largely untested code; controller regressions are likely to escape.

### H-48 — Engine validation-error test asserts the wrong behaviour

- **Reason:** the test named for an Engine validation error expects `EngineUnavailableException`, matching H-26 rather than the contract's 400/rejected distinction.
- **Evidence:** `EngineClientTest.validationErrorFromTheEngineIsReported`; `EngineClient.java:70-85`.
- **Confirm at runtime:** read the test and its mock stub; run the class in isolation.
- **Impact:** the test locks in the conflation of validation errors and outages, so a fix would break the suite.

### H-49 — Match-score test only repeats one skill set

- **Reason:** the test scores the same skills twice and therefore cannot catch the cache-key defect.
- **Evidence:** `MatchScoreServiceTest`; `MatchScoreService.java:44-51`.
- **Confirm at runtime:** add a second student with different skills in a scratch test; the cached score is reused.
- **Impact:** H-24 survives a green suite.

### H-50 — CI does not start the Engine or run the JAR

- **Reason:** the workflow runs `mvnw test` only; it never launches `engine/uniplace-engine.jar` or `bin/uniplace-portal.jar`.
- **Evidence:** `.github/workflows/tests.yml:16-32`.
- **Confirm at runtime:** inspect the workflow and the uploaded artefact list.
- **Impact:** contract drift (H-28–H-36) and packaging/start-up problems are invisible to CI.

### H-51 — `@MockBean EngineClient` hides contract drift

- **Reason:** service tests replace the real client, so request mapping and response parsing are never checked against the running Engine.
- **Evidence:** multiple service tests declare `@MockBean EngineClient`; `EngineClientTest` uses WireMock with hand-written stubs.
- **Confirm at runtime:** compare the stubs with `engine/openapi.yaml` and direct Engine responses.
- **Impact:** the Portal can pass its suite while sending fields the Engine rejects or misreading new fields.

## What would change these conclusions

| Hypothesis group | Falsified if |
| --- | --- |
| Access findings | A deployment-level proxy or filter adds the missing ownership checks before the controllers. |
| Business-rule findings | A scheduled or coordinator-only path recalculates the values, or the documented rule differs from the source comments. |
| Engine contract findings | The delivered Engine is a different build from the one described by `engine/openapi.yaml`; all probes used the delivered JAR. |
| AI findings | The provider or a wrapper sanitises prompts and normalises responses outside the Portal. |
| Test/CI findings | Another workflow or a local gate enforces coverage and Engine integration. |

## Open questions

- Which findings are intended behaviour for the customer's recruiting model, and which are implementation slips?
- Is any recruiter supposed to see every document, or only documents attached to applications they own?
- Should `/applications/{id}` be visible to the owning student and recruiter only?
- Is the Engine the authoritative validator, or does the Portal own eligibility policy?
- Is fail-open eligibility an accepted availability trade-off, and who owns the risk?
- Should a settings change take effect immediately, or is a restart part of the operating procedure?
- Are the Engine's undocumented bounds and off-by-one boundaries defects or deliberate hardening?
- Is the AI provider allowed to receive student identifiers, and is there a retention agreement?
- Is the uploaded-document loss after restart acceptable for a demonstration build?
- Which of these hypotheses should become automated regression tests first?
