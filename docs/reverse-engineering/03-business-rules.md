# 03 — Business rules

## Rule inventory approach

The source does not contain a separate rules specification. Rules are reconstructed from comments, constants, validation branches, and service methods. Each rule below states the enforced behaviour, the evidence, and any observed gap. Gaps are not labelled as defects here; they are carried to `07-hypotheses.md`.

## Global constants

| Constant | Value | Meaning | Evidence |
| --- | --- | --- | --- |
| `ApplicationService.PAGE_SIZE` | 20 | Page size for application lists | `ApplicationService.java:38` |
| `ApplicationService.MIN_LETTER` | 100 | Minimum motivation-letter length | `:39` |
| `ApplicationService.MAX_LETTER` | 2000 | Maximum motivation-letter length | `:40` |
| `PostingService.CLOSED_VISIBLE_DAYS` | 7 | Days closed/filled postings remain visible to students | `PostingService.java:26` |
| `PostingService.MAX_SEATS` | 20 | Maximum seats per posting | `:27` |
| `PostingService.MIN_HOURS` | 10 | Minimum weekly hours | `:28` |
| `PostingService.MAX_HOURS` | 40 | Maximum weekly hours | `:29` |
| `PostingService.MAX_ALLOWANCE` | 5000 | Maximum monthly allowance (QAR) | `:30` |
| `OfferService` response window | settings default 72 hours | Offer response time | `SettingsService.java:48`; `OfferService.java:80-83` |
| `RankingService.MAX_CANDIDATES` | 500 | Maximum active candidates ranked per request | `RankingService.java:25` |
| `TestDataService.MAX_PER_RUN` | 10,000 | Maximum generated applications per run | `TestDataService.java:38` |
| `UserService.MIN_PASSWORD_LENGTH` | 10 | Minimum password length | `UserService.java:18` |
| `LoginAttemptService.MAX_FAILURES` | 5 | Failed attempts before lockout | `LoginAttemptService.java:14` |
| `LoginAttemptService.LOCK_MINUTES` | 15 | Lockout duration in minutes | `:15` |
| `DocumentService.CV_MAX_BYTES` | 2 MiB (2,097,152) | CV size limit | `DocumentService.java:32` |
| `DocumentService.TRANSCRIPT_MAX_BYTES` | 5 MiB (5,242,880) | Transcript size limit | `:33` |
| `StudentService.MAX_GPA` | 4.00 | Maximum GPA | `StudentService.java:19` |
| `StudentService.MAX_CREDITS` | 140 | Maximum completed credits | `:20` |
| `StudentService.parseSkills` max | 30 skills, 60 chars each | Skill-list limits | `:95-104` |
| `TestDataService` generation ratios | 1 student per 10 applications, 1 posting per 50 | Synthetic volume shape | `TestDataService.java:82`, `:93` |

## Student rules

### STU-01 — academic standing

| Condition | Standing | Evidence |
| --- | --- | --- |
| GPA < 2.00 | PROBATION | `Standing.java:8-12` |
| GPA ≥ 2.00 | GOOD | `Standing.java:10-12` |

Standing is derived in the `Student` constructor (`Student.java:61-71`). It is not recomputed when the coordinator edits GPA through `updateAcademicRecord` (`StudentService.java:69-82`), so an existing student's standing can become stale. This is recorded as a hypothesis.

### STU-02 — editable fields

| Actor | Editable | Not editable through this path | Evidence |
| --- | --- | --- | --- |
| Student | Full name, email, skills | GPA, credits, prior internships, program | `StudentService.java:57-67` |
| Coordinator | GPA, credits, prior internships, program | Name, email, student number | `StudentService.java:69-82` |

Validation:

| Field | Rule | Evidence |
| --- | --- | --- |
| Student number | Exactly 8 digits, unique | `StudentService.java:22`, `:43-48` |
| Full name | Non-blank, ≤120 | `:120-124` |
| Email | Matches `[^@\s]+@[^@\s]+\.[^@\s]+`, ≤120 | `:21`, `:126-130` |
| GPA | 0.00–4.00, stored scale 2 HALF_UP | `:108-111`, `:52-53`, `:75` |
| Credits | 0–140 | `:112-114` |
| Prior internships | 0–20 | `:115-117` |
| Skills | Comma-separated, trimmed, case-insensitive dedupe, ≤60 chars each, ≤30 total | `:84-106` |

### STU-03 — profile and document access

- A student sees only their own profile via `GET /student/profile` (`StudentController.java:57-65`).
- Document download permits coordinator, the owning student, or **any recruiter** (`StudentController.java:94-105`). The recruiter branch has no relation check. This is a confirmed runtime behaviour (see `05-security.md`).

### STU-04 / SEC-06 — document upload

| Rule | Detail | Evidence |
| --- | --- | --- |
| File required | Null or empty rejected | `DocumentService.java:66-69` |
| Size limit | CV 2 MiB; transcript 5 MiB | `:70-73` |
| Type | First five bytes must be `%PDF-` | `:35`, `:74-76`, `:126-133` |
| Transcript issue date | Required, not in the future | `:77-84` |
| Original filename | Sanitised to last path segment, unsafe characters replaced, ≤100 chars, defaults to `document.pdf` | `:135-146` |
| Stored filename | Random UUID + `.pdf` | `:86` |
| Replacement | Previous document of the same type is deleted and its file removed | `:95-103` |
| Storage location | OS temp working directory, despite configured path | `:43-48`, `:109-116` |

The upload transaction deletes the previous DB row and flushes before creating the new row (`:95-106`). If the subsequent save fails, the old document is already gone.

## Posting rules

### POS-01/POS-02 — create and edit

- Only a recruiter with a company can create a posting; otherwise `BusinessRuleException("Register your company before creating postings.")` (`PostingService.java:125-134`).
- A CLOSED or FILLED posting cannot be edited (`:136-141`).
- A draft can be edited and re-saved; the controller's update path does not check ownership (`PostingController.java:160-171`), while the edit form does (`:149-158`).

### POS-03/POS-04 — acceptance window

```java
return posting.getStatus() == OPEN
        && posting.getCompany().getStatus() == APPROVED
        && today.isBefore(posting.getDeadline());
```

Evidence: `PostingService.java:50-55`.

The comment says "accepted until the end of the deadline day", but `isBefore` rejects the deadline day itself. A posting with deadline `2026-11-14` is not accepting applications on `2026-11-14`. This is both a source/comment mismatch and a boundary rule to test.

### POS-05 — publishing

| Precondition | Failure message | Evidence |
| --- | --- | --- |
| Status is DRAFT | "Only a draft posting can be published." | `PostingService.java:148-152` |
| Company is APPROVED | "Only approved companies can publish postings." | `:154-156` |
| Deadline is not before today | "The deadline has passed; change it before publishing." | `:157-159` |

### POS-06 — search

- Case-insensitive substring match over required and nice-to-have skills (`PostingService.java:68-87`).
- Optional company filter and minimum-allowance filter (`:75-81`).
- Only postings from APPROVED companies, status in OPEN/CLOSED/FILLED, and either OPEN or deadline ≥ today − 7 days (`:73-77`; `PostingRepository.java:26-32`).
- Ordering differs by repository query: global search by deadline then id; company-filtered search by id (`PostingRepository.java:27-42`).

### POS-07 — closed visibility

Closed and filled postings remain visible for seven days after their deadline:

```java
return posting.getStatus() != DRAFT
        && !posting.getDeadline().isBefore(today.minusDays(CLOSED_VISIBLE_DAYS));
```

Evidence: `PostingService.java:57-66`.

### Posting validation matrix

| Field | Rule | Evidence |
| --- | --- | --- |
| Title | Required, ≤150 | `PostingService.java:188-189` |
| Description | Required, ≤4000 | `:190-191` |
| Required skills | At least one after parsing | `:192-193` |
| Nice-to-have skills | Parsed with the same skill rules | `:194` |
| Minimum GPA | 0.00–4.00, default from settings | `:195-197` |
| Minimum credits | 0–140, default from settings | `:196-198` |
| Seats | 1–20 | `:199` |
| Dates | All required | `:200-201` |
| Deadline | Today or later, unless unchanged during edit | `:202` |
| Start date | After deadline | `:203` |
| Duration | At least 4 weeks, at most 6 months | `:204-205` |
| Weekly hours | 10–40 | `:206-207` |
| Monthly allowance | 0–5000 | `:208-209` |

### POS-08 — close

Only an OPEN posting can be closed (`PostingService.java:164-172`). The scheduler also closes postings whose deadline is before today (`:174-180`), using a strict `< today` query, so a posting with today's deadline stays OPEN until the next day.

## Application rules

### APP-01 — submit preconditions

| Check | Rule | Evidence |
| --- | --- | --- |
| Role | Must be STUDENT with a student record | `ApplicationService.java:136-139` |
| Posting exists | `postingService.get` throws 404 | `:142` |
| Open and approved | `acceptsApplications` | `:145-148` |
| Letter length | 100–2000 after trim | `:149-153` |
| Duplicate | No non-WITHDRAWN application for the same student/posting | `:154-156` |
| Active limit | `countActive < limit` | `:157-162` |
| Eligibility | Engine result `eligible` must be true | `:164-168` |
| Score | Engine match score | `:169` |

Allowed states for the duplicate check: any state except WITHDRAWN. Therefore REJECTED and ACCEPTED applications also block re-application, while WITHDRAWN does not.

### APP-02 — active limit

| Standing | Limit | Source |
| --- | --- | --- |
| GOOD | 5 | `SettingsService.java:45` |
| PROBATION | 2 | `SettingsService.java:46` |

`activeLimitFor` selects by exact string `"PROBATION"` (`ApplicationService.java:95-99`). `countActive` counts applications whose state is **not** REJECTED or ACCEPTED (`:88-92`). That includes WITHDRAWN, which is not in `ApplicationState.ACTIVE`. The dashboard and the submission guard therefore use different definitions:

- dashboard `Active applications` → `applications.countActive(...)` (`HomeController.java:97`) — counts WITHDRAWN.
- Engine/ranking active set → `ApplicationState.ACTIVE` = SUBMITTED/SHORTLISTED/INTERVIEW/OFFERED (`ApplicationState.java:9`).

### APP-03/APP-04 — eligibility request construction

`EngineRequestMapper.toEligibilityRequest` sends:

| Engine field | Portal value | Evidence |
| --- | --- | --- |
| `gpa` | student GPA | `EngineRequestMapper.java:40` |
| `creditsCompleted` | student credits | `:41` |
| `standing` | student standing name | `:42` |
| `minGpa` | **global default** `posting.default.minGpa` | `:43` |
| `minCredits` | posting min credits | `:44` |
| `asOf` | submission date | `:45` |
| `documents[]` | type, size, MIME, issue date | `:46-53` |

The mapper does **not** send `priorInternships`, although the Engine schema requires it and the Engine uses it for the PROBATION rule (`openapi.yaml:152-158`). At runtime the Engine accepted requests without it in the probe, but the required-field list says otherwise. This is a contract mismatch.

The mapping also ignores `posting.getMinGpa()`, so a posting-specific minimum GPA is not evaluated; only the global default is. This is recorded as a hypothesis.

### APP-05 — score and notifications

- Score is obtained from `MatchScoreService.score(student.skills, posting)` (`ApplicationService.java:169`).
- Application is saved with `LocalDateTime.now(clock)`; notifications go to the student and all recruiters of the company (`:171-179`).
- Notification failure throws `BusinessRuleException` inside the transaction (`NotificationService.java:63-67`), which can roll back the application.

### APP-06 — transitions

The state machine allows only:

| From | To |
| --- | --- |
| SUBMITTED | SHORTLISTED, REJECTED, WITHDRAWN |
| SHORTLISTED | INTERVIEW, REJECTED, WITHDRAWN |
| INTERVIEW | OFFERED, SHORTLISTED, REJECTED, WITHDRAWN |
| OFFERED | ACCEPTED, REJECTED, WITHDRAWN |
| ACCEPTED / REJECTED / WITHDRAWN | none |

Evidence: `ApplicationStateMachine.java:21-45`.

All transitions go through `assertNotFrozen`, which rejects any transition when the posting's company is PENDING (`ApplicationService.java:248-266`).

### APP-07 — shortlist threshold

- Threshold default 60 (`SettingsService.java:47`).
- If `matchScore <= threshold`, a non-blank justification is required (`ApplicationService.java:188-193`).
- Equality is treated as below/at threshold, so score 60 requires justification; scores 61+ do not.
- Override writes an audit entry `SHORTLIST_OVERRIDE` (`:195-199`).

### APP-08 — withdrawal

- Only the owning student can withdraw (`ApplicationService.java:221-223`).
- Frozen companies block withdrawal (`:224`).
- Pending offers are set to DECLINED (`:227-228`).
- No notification is sent to the recruiter on withdrawal.

### APP-09 — candidate ranking

- Only `ApplicationState.ACTIVE` applications are candidates (`RankingService.java:41-43`).
- More than 500 active candidates → `BusinessRuleException` (`:47-50`).
- Payload includes posting seats, each candidate's score, GPA, submitted time (`:51-57`).
- Engine returns ordered ids and above-cut ids; unknown ids are skipped (`:58-68`).

### APP-10 — coordinator application list

- Coordinator can filter by state, posting, and company (`CoordinatorController.java:183-196`).
- `pageForCoordinator` loads **all** applications and filters/sorts in memory, then sublists (`ApplicationService.java:116-130`). This is a scalability risk at volume.

## Offer rules

### OFF-01 — issue

| Rule | Detail | Evidence |
| --- | --- | --- |
| Owner | Recruiter must own the posting | `OfferService.java:96-99` |
| Required fields | Allowance, start, end, weekly hours | `:100-103` |
| Allowance | ≥0 and ≤ posting allowance | `:104-107` |
| Dates | Within posting start/end and end ≥ start | `:108-112` |
| Hours | 10–40 | `:113-115` |
| One offer | No existing offer for the application | `:116-118` |
| Company state | Not PENDING | `:119` |
| Effect | Application → OFFERED, offer saved PENDING | `:120-124` |

### OFF-02 — response deadline

```java
return issuedAt.toLocalDate().plusDays(settings.offerResponseHours() / 24).atStartOfDay();
```

Evidence: `OfferService.java:80-83`.

With the default 72 hours, the deadline is two calendar days later at **00:00**, not three days later at the issue time. Example: issued 2026-11-14 15:00 → respond by 2026-11-16 00:00, about 33 hours, not 72. Integer division also truncates non-multiples of 24 (e.g. 36 hours → 1 day). This is a hypothesis.

### OFF-03/OFF-04 — accept and decline

| Action | Preconditions | Effects | Evidence |
| --- | --- | --- | --- |
| Accept | Owner student, status PENDING, company not PENDING, seats left > 0 | Increment seats, offer ACCEPTED, application ACCEPTED, maybe posting FILLED, compute allowance | `OfferService.java:134-164` |
| Decline | Owner student, status PENDING | Offer DECLINED, application REJECTED, notify recruiters | `:166-181` |

The seat check uses the posting loaded through the application, not `findByIdForUpdate`, so concurrent accepts can oversubscribe. This is a hypothesis.

### OFF-05/OFF-07 — agreement and allowance

- Agreement can only be recorded for an ACCEPTED application (`ApplicationService.java:232-244`).
- Received date cannot be in the future (`:239-241`).
- Late days = days between offer start and agreement date, floored at 0 (`OfferService.java:185-189`).
- Allowance is recomputed via the Engine (`:206-214`).
- Recalculation is allowed only for ACCEPTED offers (`:193-204`).

### OFF-06 — expiry and reminders

- PENDING offers past `respondBy` become EXPIRED; OFFERED applications become WITHDRAWN (`OfferService.java:237-256`).
- Reminder window: `now >= respondBy − 24h` and `now < respondBy` (`:258-273`).
- `reminderSent` is never set to true, so the same offer can be reminded on every scheduler run inside the window. This is a hypothesis.

## Interview rules

| Rule | Detail | Evidence |
| --- | --- | --- |
| Owner | Recruiter must own the application | `InterviewService.java:57-58` |
| State | Only SHORTLISTED can be invited | `:59-61` |
| Fields | Date, start, end, location required; location ≤200 | `:62-67` |
| Frozen | Company not PENDING | `:68` |
| Ordering | End must be after start | `:69-73` |
| Engine validation | Candidate + recruiter slots + student slots | `:75-82` |
| Effect | Interview saved, application → INTERVIEW, reschedule flag cleared, student notified | `:84-89` |
| Cancel | Only INTERVIEW state; interview cancelled; application → SHORTLISTED; student notified | `:93-109` |
| Reschedule | Student owner; only INTERVIEW; only once; notifies recruiters | `:111-129` |

The Engine is the authority for 15–120 minutes, Sunday–Thursday, 08:00–18:00, and overlap. Observed Engine behaviour deviates on the 120-minute boundary and 18:00 end (see `04-http-and-engine-api.md`).

## Company rules

| Rule | Detail | Evidence |
| --- | --- | --- |
| Register | Only a recruiter without a company | `CompanyService.java:51-64` |
| Update | Only the recruiter's own company | `:66-77` |
| Name | Required ≤120 and unique | `:114-130` |
| Sector | Required ≤80 | `:118-120` |
| Contact e-mail | Valid pattern | `:121-123` |
| Approve | Any company id → APPROVED; notifies recruiters | `:79-84` |
| Suspend | Only APPROVED → SUSPENDED; notifies recruiters and active applicants | `:86-106` |
| Frozen applications | PENDING company blocks transition | `ApplicationService.java:261-266` |

SUSPENDED companies are not explicitly handled by `assertNotFrozen`; only PENDING is. That means suspended-company applications are **not** frozen by this guard, despite the suspension notification saying they are on hold. This is a hypothesis.

## Coordinator and administration rules

| Rule | Detail | Evidence |
| --- | --- | --- |
| Student creation | Password policy and username availability checked before student creation | `CoordinatorController.java:112-119` |
| User creation | STUDENT role rejected; recruiter may be linked to a company | `:157-172` |
| Unlock | Resets failed attempts and lock timestamp | `UserService.java:82-87` |
| Reports | Four fixed reports: placement, scores, offers, days | `ReportService.java:95-107` |
| CSV export | Escapes commas/quotes/newlines, CRLF line endings | `:109-128` |
| Test data | 1–10,000 applications, at least one approved company | `TestDataService.java:67-78` |
| Settings | Only known keys; non-negative numbers; provider/url checks | `SettingsController.java:89-158` |

### Report definitions

| Report | Computation | Evidence |
| --- | --- | --- |
| Placement rate per program | Distinct students and distinct accepted students per program, percentage with one decimal | `ApplicationRepository.java:71-74`; `ReportService.java:41-50` |
| Average score per company | Loops every company, every posting, every application; average match score one decimal | `ReportService.java:52-71` |
| Offers issued vs accepted | `offers.count()` and `countByStatus(ACCEPTED)`; acceptance percentage | `:73-80` |
| Average days to offer | Sum of whole days between submission date and issue date over all offers | `:82-93` |

`ReportService.averageScorePerCompany` performs N+1 queries and loads all applications in memory. `daysToOffer` counts **all** offers, including declined and expired ones, not only accepted placements.

## AI rules

| Rule | Detail | Evidence |
| --- | --- | --- |
| Provider configured | If a key is required and missing, simulated provider is used | `AiClient.java:38-40`; `AiAssistantService.java:33-35`, `:43-45` |
| Feedback shape | JSON object with numeric `score` and three arrays | `PromptBuilder.java:11-25`; `AiResponseParser.java:23-31` |
| Question shape | JSON object with `questions` | `AiResponseParser.java:33-36` |
| Cache | 10-minute cache keyed by student + posting only | `FeedbackCache.java:20`, `:45-47` |
| Prompt content | Student name, number, GPA, posting title/skills, raw letter | `PromptBuilder.java:42-48` |
| Temperature | 1.3 | `AiClient.java:24` |
| Default model | `gpt-4o-mini` | `:25` |

The parser's `list(...)` ignores its `min` and `max` parameters (`AiResponseParser.java:59-69`), so any number of items is accepted. The feedback prompt contains contradictory instructions ("List 1 to 5" and "Always give exactly three"). The cache key ignores the letter text, so two different letters for the same student/posting share a cached review for ten minutes.

## Settings defaults and effects

| Key | Default | Used by | Live reload? |
| --- | --- | --- | --- |
| `limits.active.good` | 5 | `ApplicationService.activeLimitFor` | No — static cache |
| `limits.active.probation` | 2 | same | No |
| `shortlist.threshold` | 60 | shortlist guard, views | No |
| `offer.response.hours` | 72 | `OfferService.responseDeadline` | Yes (read each time) |
| `posting.default.minGpa` | 2.00 | posting form, eligibility mapper | Yes |
| `posting.default.minCredits` | 45 | posting form | Yes |
| `allowance.transport` | 300 | allowance request | Yes |
| `allowance.monthlyCap` | 5000 | allowance request | Yes (but mapper never sends it) |
| `allowance.penaltyRate` | 0.05 | allowance request | Yes |
| `allowance.penaltyCap` | 0.20 | allowance request | Yes |
| `engine.baseUrl` | `http://127.0.0.1:8081/api/v1` | `EngineClient` constructor | No — captured at construction |
| `ai.provider` | `openai` | AI client | Yes |
| `ai.baseUrl` / `ai.apiKey` / `ai.model` | empty | AI client | Yes |
| `smtp.host` | empty (mail disabled) | notification service | Yes |
| `smtp.port` | 587 | notification service | Yes |
| `smtp.username` / `smtp.password` | empty | notification service | Yes |
| `smtp.from` | `uniplace@localhost` | notification service | Yes |

Evidence: `SettingsService.java:42-65`, `:101-132`; `EngineRequestMapper.java:93-95`.

`toAllowanceRequest` never sets `monthlyCap`, so the Engine uses its default 5000 regardless of the coordinator's setting (`EngineRequestMapper.java:86-97`).

## Boundary table for test design

| Area | Lower boundary | Upper boundary | Equality behaviour | Evidence |
| --- | --- | --- | --- | --- |
| Letter length | 100 accepted | 2000 accepted | Inclusive | `ApplicationService.java:149-153` |
| GPA | 0.00 accepted | 4.00 accepted | Inclusive | `StudentService.java:108-111` |
| Credits | 0 accepted | 140 accepted | Inclusive | `:112-114` |
| Prior internships | 0 accepted | 20 accepted | Inclusive | `:115-117` |
| Seats | 1 accepted | 20 accepted | Inclusive | `PostingService.java:199` |
| Weekly hours | 10 accepted | 40 accepted | Inclusive | `:206-207` |
| Allowance | 0 accepted | 5000 accepted | Inclusive | `:208-209` |
| Internship duration | exactly 4 weeks accepted | exactly 6 months accepted | Inclusive | `:204-205` |
| Shortlist threshold | ≤60 requires justification | >60 does not | Equality requires justification | `ApplicationService.java:188-193` |
| Active limit | count < limit allowed | count == limit rejected | Inclusive rejection at limit | `:157-162` |
| Closed visibility | deadline today−7 visible | deadline today−8 hidden | **Inclusive** at −7 | `PostingService.java:64-65` |
| Application deadline | before deadline accepted | deadline day rejected | Strict `isBefore` | `:50-55` |
| Interview duration | 15 min valid | 120 min **rejected at runtime** | Contract says inclusive | Engine probe |
| Interview end | 18:00 valid | 18:01 **accepted at runtime** | Contract says ≤18:00 | Engine probe |
| Transcript validity | issue date day 1 | day 180 **rejected at runtime** | Contract says 180 days inclusive | Engine probe |

## Open questions

- Which active-application definition is authoritative: `ApplicationState.ACTIVE` or "not REJECTED/ACCEPTED"?
- Should the deadline day itself be open for applications?
- Should a posting-specific minimum GPA override the global default?
- Should a suspended company's applications be frozen, as the suspension notification claims?
- Is `respondBy` intended to be a precise 72-hour instant or an end-of-day date?
- Should reminders be sent once only, and where is `reminderSent` expected to be persisted?
- Should offers be unique per application at the database level?
- Should acceptance be serialised to prevent seat oversubscription?
- Should AI feedback be cached per letter text rather than per student/posting pair?
