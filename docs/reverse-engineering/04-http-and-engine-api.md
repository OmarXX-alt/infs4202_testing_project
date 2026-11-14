# 04 — HTTP surface and Engine API

## Portal HTTP surface

All routes are server-rendered unless stated. "Guard" describes application-level checks in addition to the role rule in `SecurityConfig`.

| Method | Path | Access | Purpose | Application guard | Evidence |
| --- | --- | --- | --- | --- | --- |
| GET | `/login` | Public | Login page; optional `error=locked|bad` message | none | `HomeController.java:67-75` |
| GET | `/` | Authenticated | Redirect to `/dashboard` | Security redirects anonymous users first | `HomeController.java:77-80` |
| GET | `/denied` | Authenticated | Access-denied page | none | `HomeController.java:82-86` |
| GET | `/dashboard` | Authenticated | Role-specific dashboard | none | `HomeController.java:88-113` |
| GET | `/postings` | Authenticated | Posting search | none | `PostingController.java:62-72` |
| GET | `/postings/{id}` | Authenticated | Posting detail | Students: visibility; recruiters: own or visible | `PostingController.java:74-94` |
| POST | `/postings/{id}/apply` | Authenticated | Submit application | Service: role, eligibility, limits | `ApplicationController.java:61-75` |
| POST | `/postings/{id}/feedback` | Authenticated | AI letter feedback | Student only | `PostingController.java:97-116` |
| GET | `/applications` | Authenticated | Student or recruiter list | Role split in controller | `ApplicationController.java:77-95` |
| GET | `/applications/{id}` | Authenticated | Application detail | **None in controller** | `ApplicationController.java:97-104` |
| POST | `/applications/{id}/withdraw` | Authenticated | Withdraw | Service: student owner | `ApplicationController.java:119-128` |
| POST | `/applications/{id}/shortlist` | Authenticated | Shortlist | Service: recruiter owner | `ApplicationController.java:130-140` |
| POST | `/applications/{id}/reject` | Authenticated | Reject | Service: recruiter owner | `ApplicationController.java:142-151` |
| POST | `/applications/{id}/interview` | Authenticated | Schedule interview | Service: recruiter owner + Engine | `ApplicationController.java:153-166` |
| POST | `/interviews/{interviewId}/cancel` | Authenticated | Cancel interview | Service: recruiter owner | `ApplicationController.java:168-178` |
| POST | `/applications/{id}/reschedule` | Authenticated | Request reschedule | Service: student owner | `ApplicationController.java:180-190` |
| POST | `/applications/{id}/offer` | Authenticated | Issue offer | Service: recruiter owner | `ApplicationController.java:198-215` |
| POST | `/applications/{id}/questions` | Authenticated | AI interview questions | `guard.assertRecruiterOwns` | `ApplicationController.java:218-232` |
| POST | `/offers/{id}/accept` | Authenticated | Accept offer | Service: student owner | `ApplicationController.java:234-244` |
| POST | `/offers/{id}/decline` | Authenticated | Decline offer | Service: student owner | `ApplicationController.java:246-256` |
| GET | `/student/profile` | STUDENT | Profile and documents | none | `StudentController.java:57-65` |
| POST | `/student/profile` | STUDENT | Update name/email/skills | Service validation | `StudentController.java:67-78` |
| POST | `/student/documents` | STUDENT | Upload CV/transcript | Service validation | `StudentController.java:80-92` |
| GET | `/documents/{id}` | Authenticated | Download PDF | Coordinator, owner, **any recruiter** | `StudentController.java:94-115` |
| GET | `/recruiter/company` | RECRUITER | Company form | none | `CompanyController.java:26-32` |
| POST | `/recruiter/company` | RECRUITER | Register/update company | Service validation | `CompanyController.java:34-50` |
| GET | `/recruiter/postings` | RECRUITER | Own postings | Company filter in controller | `PostingController.java:120-127` |
| GET | `/recruiter/postings/new` | RECRUITER | New posting form | none | `PostingController.java:129-134` |
| POST | `/recruiter/postings` | RECRUITER | Create draft | Service: company required | `PostingController.java:136-147` |
| GET | `/recruiter/postings/{id}/edit` | RECRUITER | Edit form | `assertRecruiterOwns` | `PostingController.java:149-158` |
| POST | `/recruiter/postings/{id}` | RECRUITER | Update posting | **No ownership check** | `PostingController.java:160-171` |
| POST | `/recruiter/postings/{id}/publish` | RECRUITER | Publish draft | `assertRecruiterOwns` | `PostingController.java:173-183` |
| POST | `/recruiter/postings/{id}/close` | RECRUITER | Close open posting | `assertRecruiterOwns` | `PostingController.java:185-195` |
| GET | `/recruiter/postings/{id}/candidates` | RECRUITER | Ranked candidates | `assertRecruiterOwns` | `PostingController.java:197-211` |
| GET | `/coordinator/students` | COORDINATOR | Student list | role rule | `CoordinatorController.java:71-75` |
| GET | `/coordinator/students/{id}` | COORDINATOR | Student detail | role rule | `:77-81` |
| POST | `/coordinator/students/{id}` | COORDINATOR | Update academic record | role rule | `:83-94` |
| POST | `/coordinator/students` | COORDINATOR | Create student + user | role rule | `:96-110` |
| GET | `/coordinator/companies` | COORDINATOR | Company list | role rule | `:123-127` |
| POST | `/coordinator/companies/{id}/approve` | COORDINATOR | Approve | role rule | `:129-134` |
| POST | `/coordinator/companies/{id}/suspend` | COORDINATOR | Suspend | role rule | `:136-145` |
| GET | `/coordinator/users` | COORDINATOR | User list | role rule | `:149-155` |
| POST | `/coordinator/users` | COORDINATOR | Create non-student user | role rule | `:157-172` |
| POST | `/coordinator/users/{id}/unlock` | COORDINATOR | Unlock account | role rule | `:174-179` |
| GET | `/coordinator/applications` | COORDINATOR | All applications | role rule | `:183-196` |
| POST | `/coordinator/applications/{id}/agreement` | COORDINATOR | Record agreement date | role rule | `:198-209` |
| POST | `/coordinator/offers/{id}/recalculate` | COORDINATOR | Recompute allowance | role rule | `:211-220` |
| GET | `/coordinator/audit` | COORDINATOR | Audit log | role rule | `:224-228` |
| GET | `/coordinator/outbox` | COORDINATOR | All notifications | role rule | `:231-237` |
| GET | `/coordinator/reports` | COORDINATOR | Four report tables | role rule | `:241-245` |
| GET | `/coordinator/reports/{name}.csv` | COORDINATOR | CSV export | role rule | `:247-254` |
| GET | `/coordinator/tools` | COORDINATOR | Test-data form | role rule | `:258-261` |
| POST | `/coordinator/tools/generate` | COORDINATOR | Generate volume data | role rule | `:263-273` |
| GET | `/coordinator/settings` | COORDINATOR | Settings form | role rule | `SettingsController.java:76-87` |
| POST | `/coordinator/settings` | COORDINATOR | Save settings | role rule; **CSRF exempt** | `SettingsController.java:89-122`; `SecurityConfig.java:25` |
| POST | `/coordinator/settings/test-ai` | COORDINATOR | Probe AI provider | role rule; CSRF exempt | `SettingsController.java:125-130` |
| GET | `/account/password` | Authenticated | Password form | none | `AccountController.java:30-33` |
| POST | `/account/password` | Authenticated | Change password | current password check | `:35-49` |
| POST | `/account/logout` | Authenticated | Clear JSESSIONID and redirect | none | `:52-59` |
| GET | `/notifications` | Authenticated | Own notifications | recipient filter | `:62-68` |

### Route-level status behaviour observed

| Request | Result | Evidence |
| --- | --- | --- |
| Anonymous `GET /dashboard`, `/h2-console/login.jsp`, `/actuator/health`, `/no-such-page`, `/api/...` | 302 to `/login` | runtime probes; `SecurityConfig.java:26-34` |
| Authenticated student `GET /coordinator/students` | 403 | runtime probe; role rule |
| Authenticated recruiter `GET /coordinator/*` | 403 | runtime probe |
| `GET /coordinator/postings/999999` | 404 | `NotFoundException` → `WebExceptionHandler` |
| `GET /applications/999999` | 404 | same |
| `GET /coordinator/reports/placement.csv` | 200, `text/csv;charset=UTF-8` | runtime probe |
| `GET /coordinator/reports/application.csv` | 404 | report name not in switch |
| `GET /documents/3` as non-owner student | 403 | runtime probe |
| `POST /coordinator/settings` without CSRF token | 302 (accepted) | runtime probe; CSRF exemption |
| `GET /h2-console/login.jsp` authenticated | 404 | console disabled |

### View/template surface

| Template | Purpose | Notable behaviour |
| --- | --- | --- |
| `fragments/layout.html` | Shared head/nav/flash/pager | Role-based nav; logout POST form |
| `dashboard.html` | Role dashboard | Coordinator figures, student limits/interviews, recruiter postings |
| `postings/list.html` | Search results | Company name rendered with `th:utext` |
| `postings/detail.html` | Posting + application form | Letter counter script |
| `postings/form.html` | Create/edit posting | Good/bad flash |
| `postings/mine.html` | Recruiter's postings | Status links |
| `postings/candidates.html` | Ranked candidates | Above-cut marker |
| `applications/list.html` | Filtered applications | State/posting filters |
| `applications/view.html` | Application detail | Offer/interview/AI actions by role |
| `student/profile.html` | Profile + uploads | Documents list |
| `coordinator/*.html` | Administration | Students, companies, users, applications, audit, settings, reports, tools |
| `company/form.html` | Company details | Register/update |
| `notifications.html` | Inbox/outbox | Paged |
| `account/password.html` | Password change | Three fields |
| `login.html`, `denied.html`, `not-found.html`, `error.html` | Entry/error pages | Error page can expose details |

## Engine API contract

Base URL: `http://127.0.0.1:8081/api/v1` (`openapi.yaml:10-11`). All bodies JSON. Dates `yyyy-MM-dd`; date-times ISO-8601 with offset, no offset means UTC (`:5-9`). The contract states validation failures return 400 with `{error, field, message}` and "never answers 500 to well-formed JSON" (`:130-140`).

| Method | Path | Request | Response | Portal caller |
| --- | --- | --- | --- | --- |
| GET | `/health` | — | `{status, version}` | start script |
| POST | `/eligibility` | GPA, credits, standing, prior internships, min GPA, min credits, as-of date, documents | `{eligible, reasons[]}` | `ApplicationService.submit` via `EngineClient.checkEligibility` |
| POST | `/match` | Student skills, required skills, nice-to-have skills | `{score, matchedRequired, missingRequired, matchedNiceToHave}` | `MatchScoreService.score` |
| POST | `/rank` | Seats, candidates (`applicationId`, `score`, `gpa`, `submittedAt`) | `{ranked[], aboveCut[]}` | `RankingService.rank` |
| POST | `/schedule/validate` | Candidate slot, recruiter slots, student slots | `{valid, conflicts[]}` | `InterviewService.schedule` |
| POST | `/allowance` | Allowance, dates, hours, late days, transport, cap, penalty rate/cap | `{total, lines[]}` | `OfferService.computeAllowance` |

### Engine business formulas from the contract

| Endpoint | Contract formula | Evidence |
| --- | --- | --- |
| Match | `70 × matchedRequired/required + 30 × matchedNice/nice`; nice term 0 when no nice skills; case and trim insensitive; round half up | `openapi.yaml:47-66` |
| Rank | Score desc, GPA desc, submittedAt asc, applicationId asc; `aboveCut` = first `seats` | `:67-85` |
| Slot | 15–120 minutes, Sunday–Thursday, start ≥08:00, end ≤18:00 same day, no overlap; adjacent slots do not overlap | `:86-105` |
| Allowance | Months = whole months inclusive + remaining days/30; transport if weekly hours ≥30; cap adjustment when base+transport > cap; penalty = subtotal × min(cap, rate × started late weeks); total rounded 2 dp | `:106-128` |
| Eligibility | GPA/credits thresholds; PROBATION ≤1 prior internship; CV present/PDF/≤2 MiB; transcript present/PDF/≤5 MiB/valid on asOf; transcript valid 180 days counting issue date as day 1 | `:26-46` |

## Portal → Engine mapping

| Portal DTO field | Source | Notes |
| --- | --- | --- |
| `EligibilityRequest.gpa` | `Student.gpa` | `EngineRequestMapper.java:40` |
| `creditsCompleted` | `Student.creditsCompleted` | `:41` |
| `standing` | `Student.standing.name()` | `:42` |
| `minGpa` | global setting | **not posting min GPA** (`:43`) |
| `minCredits` | posting | `:44` |
| `asOf` | submission date | `:45` |
| `documents` | cached document list | `:46-53` |
| `MatchRequest.studentSkills` | student skills | `EngineRequestMapper.java:57-62` |
| `requiredSkills` | posting required skills | same |
| `niceToHaveSkills` | posting nice skills | same |
| `RankCandidate` | application id, score, student GPA, submission time with offset | `:65-72` |
| `AllowanceRequest` | offer allowance/dates/hours, late days, transport, penalty rate/cap | `:86-97` |

`AllowanceRequest.monthlyCap` is left null, so the Engine default applies (`EngineRequestMapper.java:86-97`; `openapi.yaml:234-237`).

## Observed Engine behaviour vs contract

All probes were sent to a freshly started Engine on 2026-11-14. They are black-box observations; no Engine internals were inspected.

| ID | Probe | Expected from contract | Observed | HTTP | Status |
| --- | --- | --- | --- | --- | --- |
| E-01 | `/health` | `{status:"UP",version:"1.0.0"}` | matched | 200 | Pass |
| E-02 | `/eligibility` missing required fields | 400 validation error | `{"error":"internal","message":"Unexpected error"}` | 500 | **Contract violation** |
| E-03 | `/eligibility` empty object | 400 validation error | internal error | 500 | **Contract violation** |
| E-04 | GPA 9 | 400 (schema maximum 4) | `eligible:true` | 200 | **Contract violation** |
| E-05 | GPA −1 | 400 | `eligible:false`, reason "below minimum" | 200 | Schema minimum not enforced |
| E-06 | Credits 200 | 400 (max 140) | 400 field `creditsCompleted` "must be between 0 and 140" | 400 | Pass |
| E-07 | `standing:"good"` | 400 (enum GOOD/PROBATION) | `eligible:true` | 200 | **Enum not validated** |
| E-08 | `standing:"BAD"` | 400 | `eligible:true` | 200 | **Enum not validated** |
| E-09 | `priorInternships:-1` | 400 (minimum 0) | 400 "must be between 0 and 100" | 400 | Pass, undocumented max 100 |
| E-10 | PROBATION with 2 prior internships | ineligible | ineligible, reason given | 200 | Pass |
| E-11 | Transcript issued 2026-05-19, asOf 2026-11-14 | valid (day 180) | ineligible "not valid" | 200 | **Off-by-one** |
| E-12 | Transcript issued 2026-05-20 | valid | eligible | 200 | Effective window 179 days |
| E-13 | Transcript issue date missing | 400 | 400 field `documents[1].issueDate` | 400 | Pass |
| E-14 | Unknown document type `PASSPORT` | 400 enum error | 400 enum error | 400 | Pass |
| E-15 | Duplicate CV | no rule; likely invalid | `eligible:true` | 200 | Undefined |
| E-16 | Transcript MIME `image/png` | ineligible | ineligible "must be a PDF" | 200 | Pass |
| E-17 | Match exact, mixed case | 100 | 62 with one of three required + one nice | 200 | Formula plausible |
| E-18 | Match only case differs (`java` vs `Java`) | case-insensitive, 100 | 30 | 200 | **Case-sensitive** |
| E-19 | Match only whitespace differs | trim-insensitive, 100 | 30 | 200 | **Not trimmed** |
| E-20 | Match no nice-to-have list, no required match | 0 | 30 | 200 | **Free 30 points** |
| E-21 | Match zero matched required, no nice list | 0 | 30 | 200 | **Free 30 points** |
| E-22 | Match one of four required, no nice | 17.5 → 18 | 18 | 200 | Pass |
| E-23 | Match empty student skills | 0 + free nice only | 30 with no nice list | 200 | Consistent with E-20 |
| E-24 | Match `studentSkills` null | 400 | 400 "is required" | 400 | Pass |
| E-25 | Match empty required list | 400 | 400 "must contain at least one skill" | 400 | Pass |
| E-26 | Rank GPA tie-break (scores 90/90/80, GPAs 2.0/4.0/4.0) | ids 2,1,3 | ids 1,2,3 | 200 | **GPA tie-break ignored** |
| E-27 | Rank time tie-break | earliest first | correct | 200 | Pass |
| E-28 | Rank id tie-break | ascending id | correct | 200 | Pass |
| E-29 | Rank seats 0 | 400 | 400 "must be between 1 and 1000" | 400 | Pass, undocumented max 1000 |
| E-30 | Rank 501 candidates | 400 | 400 "at most 500" | 400 | Pass |
| E-31 | Rank empty candidate list | empty arrays | empty arrays | 200 | Pass |
| E-32 | Schedule 15 min | valid | valid | 200 | Pass |
| E-33 | Schedule 119 min | valid | valid | 200 | Pass |
| E-34 | Schedule exactly 120 min | valid | invalid "Duration must be between 15 and 120" | 200 | **Boundary bug** |
| E-35 | Schedule 121 min | invalid | invalid | 200 | Pass |
| E-36 | Schedule start 08:00 | valid | valid | 200 | Pass |
| E-37 | Schedule start 07:59 | invalid | invalid | 200 | Pass |
| E-38 | Schedule end exactly 18:00 | valid | valid | 200 | Pass |
| E-39 | Schedule end 18:01 | invalid | valid | 200 | **End bound not enforced** |
| E-40 | Schedule end 18:59 | invalid | valid | 200 | **End bound not enforced** |
| E-41 | Schedule 19:00 end | invalid (duration likely too long) | invalid, reason only duration | 200 | Ambiguous |
| E-42 | Schedule adjacent slots (one ends when next starts) | no overlap | overlap conflict | 200 | **Adjacency treated as overlap** |
| E-43 | Schedule Friday | invalid | invalid | 200 | Pass |
| E-44 | Schedule end before start | 400 | 400 | 400 | Pass |
| E-45 | Schedule UTC offset input | interpreted correctly | valid | 200 | Pass |
| E-46 | Allowance 29 h | no transport | no transport | 200 | Pass |
| E-47 | Allowance 30 h | transport per contract | **no transport** | 200 | **Boundary bug** |
| E-48 | Allowance 31 h | transport | transport | 200 | Pass |
| E-49 | Allowance 40 h | transport | transport | 200 | Pass |
| E-50 | Allowance base 5000 + transport 300 | cap adjustment −300 | total 5300, no cap line | 200 | **Cap ignored** |
| E-51 | Allowance custom transport/penalty | custom values applied | 1000 + 100 − 110 = 990 | 200 | Pass |
| E-52 | Allowance late 100 days | 15 started weeks × 5% capped 20% | 20% applied | 200 | Pass |
| E-53 | Allowance monthly 5001 | 400 | 400 | 400 | Pass |
| E-54 | Allowance end before start | 400 | 400 | 400 | Pass |
| E-55 | Allowance weekly hours 9 | 400 | 400 | 400 | Pass |
| E-56 | Allowance one day (3000/month) | 0.03 months → 100 + 10 | 110 | 200 | Pass |
| E-57 | Allowance Feb leap span | 1.07 months | 1280 on 1200 base | 200 | Plausible |
| E-58 | Unknown path | 404 | 404 `not_found` | 404 | Pass |
| E-59 | Wrong method | 405 | 405 `method_not_allowed` | 405 | Pass |
| E-60 | Non-JSON body | 400 | 400 `Malformed JSON` | 400 | Pass |

### Contract-deviation clusters

1. **Validation envelope is inconsistent.** Missing required eligibility fields produce 500, while most other validation errors produce 400. This breaks the contract's explicit "never 500" promise.
2. **Schema bounds are unevenly enforced.** Credits, min GPA and allowance are checked; GPA input and standing enum are not.
3. **Transcript validity is one day short.** The contract's day-1 convention yields 180 days; the observed window accepts 2026-05-20 but rejects 2026-05-19 for an as-of date of 2026-11-14.
4. **Match scoring silently awards the nice-to-have term when the list is absent.** A posting with no nice skills and no required match scores 30, not 0.
5. **Skill comparison is case- and whitespace-sensitive.** This contradicts the contract and changes scores whenever a student's skill spelling differs from the posting's.
6. **Rank ignores the documented GPA tie-break.**
7. **Schedule boundaries differ:** 120 minutes is rejected, but 18:01 and 18:59 are accepted; adjacent slots are treated as overlapping.
8. **Allowance boundary/cap differ:** transport starts at 31 hours, not 30; a base+transport total above the cap is not reduced.
9. **Undocumented limits:** `priorInternships` max 100, `seats` max 1000.
10. **Portal does not send all required fields.** `EligibilityRequest.priorInternships` is missing from the mapper, and `AllowanceRequest.monthlyCap` is never set.

## HTTP semantics and content types

| Response | Content type | Notes |
| --- | --- | --- |
| HTML views | `text/html;charset=UTF-8` | Thymeleaf |
| CSV reports | `text/csv;charset=UTF-8` | `CoordinatorController.java:247-254` |
| PDF download | `application/pdf` | `StudentController.java:110-114` |
| Redirects | 302 with `Location` | Login success, form posts |
| Denied | 403 HTML | `denied.html` |
| Not found | 404 HTML | `not-found.html` |
| Error | 500 HTML | Spring Boot error page; stack trace configurable |

The download endpoint always labels the response `application/pdf` even if the stored bytes were not a PDF. Upload validation prevents this through the normal path, but seeded files are only labelled, not magic-checked (`DataSeeder.java:367-379`).

## Error message catalogue

| Situation | Message | Evidence |
| --- | --- | --- |
| Posting not accepting | "This posting is not accepting applications. Its deadline was …" | `ApplicationService.java:145-148` |
| Letter length | "The motivation letter must have between 100 and 2000 characters (it has N)." | `:150-152` |
| Duplicate application | "You already have an active application for this posting." | `:154-155` |
| Active limit | "You already have N active applications, the maximum for students in … standing." | `:159-161` |
| Shortlist override | "The match score N is below the shortlist threshold N. Give a justification…" | `:190-192` |
| Frozen company | "… is not approved at the moment; its applications are frozen." | `:261-265` |
| Offer allowance | "Invalid offer: monthlyAllowance > posting.monthlyAllowance (N)." | `OfferService.java:104-107` |
| Offer dates | "Invalid offer: startDate/endDate outside [start, end]." | `:108-112` |
| Offer hours | "Invalid offer: weeklyHours not in [10, 40]." | `:113-115` |
| Offer duplicate | "An offer was already issued for this application." | `:116-118` |
| No seats | "All seats of … are already taken." | `:145-147` |
| Interview slot | "This slot cannot be used: …" | `InterviewService.java:80-82` |
| AI unavailable | "Assistant unavailable. Try again later." | `ApplicationController.java:227-229` |

## Open questions

- Which response should an Engine validation error produce in the Portal: user-facing correction, generic failure, or "unavailable"?
- Should the Portal pass the posting-specific minimum GPA and `priorInternships` to the Engine?
- Is the Engine's 500 response for missing fields intentional, and does the client need to distinguish it from unavailability?
- Should the Engine treat the nice-to-have term as 0 when the array is omitted, or is a missing array meant to mean "full nice-to-have credit"?
- Which tie-break order is authoritative if the Engine and the contract disagree?
- Is 120 minutes meant to be allowed, and is 18:00 a hard end?
- Should `monthlyCap` be sent explicitly, and should the Engine cap base+transport?
- Are `priorInternships` max 100 and `seats` max 1000 deliberate safety limits or accidental validation artifacts?
