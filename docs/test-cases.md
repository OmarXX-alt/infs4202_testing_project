# Test design

A spreadsheet `docs/test-cases.xlsx` may replace this file. At least 10 test cases for each of the three primary modules.

Test-case IDs follow the strategy (section 9): `TC-<PREFIX>-<nnn>`. Expected results are written from the
Requirements and `engine/openapi.yaml`, never from observed output. Status values: Pass, Fail, Blocked.

## Test case matrix

| Test case ID | Requirement | Module | Level | Technique | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |  |  |  |  |  |

## Module 2: Applications and Eligibility Decisions (`TC-APP`, `TC-INT`)

Owner: Ibrahim Al Shouli. Requirements in scope: APP-01 to APP-09, POS-04, STU-04 (eligibility part), INT-01 to
INT-03 (Engine slot validation), REL-01, REL-02, PERF-02.

Levels: **API** = black-box contract test against the running Engine (`tests/engine-contract`); **Service** =
white-box test of Portal services with the Engine mocked (`portal/src/test`); **Integration** = Portal Engine
client against WireMock with injected faults. Type: F = functional, NF = non-functional.

First execution: 24/11/2026, macOS, Java 22, Engine 1.0.0.

### Engine (black-box)

| Test case ID | Requirement | Module | Level | Type | Technique | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-APP-001 | APP-04 | Applications | API | F | Equivalence partitioning (valid class) | Engine running | POST /eligibility | GPA 3.20, min 2.50, credits 60/30, valid CV and transcript | `eligible: true`, empty `reasons` | As expected | Pass | `EligibilityContractTest.eligibleStudent` | |
| TC-APP-002 | APP-04 | Applications | API | F | Boundary value analysis | Engine running | POST /eligibility with GPA around minGpa | (2.49, 2.50), (2.50, 2.50), (2.51, 2.50), (3.00, 3.00), (2.00, 2.00) | Eligible when GPA ≥ minGpa ("at least") | GPA equal to minGpa refused: "GPA 2.50 is below the minimum 2.50" | Fail | `EligibilityContractTest.gpaBoundary` | |
| TC-APP-003 | APP-04 | Applications | API | F | Boundary value analysis | Engine running | POST /eligibility with credits around minCredits | credits 29, 30, 31; min 30 | 29 refused, 30 and 31 eligible | As expected | Pass | `EligibilityContractTest.creditsBoundary` | |
| TC-APP-004 | APP-04 | Applications | API | F | Decision table (all rules false) | Engine running | POST /eligibility failing four rules | GPA 1.0, credits 10, no documents | 4 reasons: GPA, credits, CV missing, transcript missing | As expected | Pass | `EligibilityContractTest.everyFailedRuleIsReported` | |
| TC-APP-005 | APP-04 | Applications | API | F | Decision table (standing × prior internships) | Engine running | POST /eligibility | GOOD/3, PROBATION/0, PROBATION/1, PROBATION/2 | Only PROBATION with 2 refused | As expected | Pass | `EligibilityContractTest.probationPriorInternships` | |
| TC-APP-006 | STU-04, APP-04 | Applications | API | F | Boundary value analysis (dates) | Engine running | POST /eligibility, transcript issued 2026-01-01, vary asOf | asOf day 0, 1, 179, 180, 181 | Valid on days 1 to 180 | Day 180 (2026-06-29) refused: valid only 179 days | Fail | `EligibilityContractTest.transcriptValidity` | |
| TC-APP-007 | STU-04 | Applications | API | F | Boundary value analysis, equivalence partitioning | Engine running | POST /eligibility | CV 2,097,152 / +1 bytes; transcript 5,242,880 / +1 bytes; PNG CV; Word transcript | Limit accepted, limit + 1 and non-PDF refused | As expected | Pass | `EligibilityContractTest.documentBoundaries` | |
| TC-APP-008 | REL-02 | Applications | API | NF (reliability) | Equivalence partitioning (invalid classes) | Engine running | POST /eligibility with out-of-domain values | GPA 4.01, GPA -0.01, standing EXCELLENT, credits 141, minGpa 4.5 | 400 with `field` naming the value | GPA 4.01, GPA -0.01 and standing EXCELLENT answered 200 (evaluated as normal input) | Fail | `EligibilityContractTest.outOfDomainValues` | |
| TC-APP-009 | REL-02 | Applications | API | NF (reliability) | Error guessing, equivalence partitioning | Engine running | POST /eligibility with one required field removed | each of the 7 required fields | 400 naming the field, never 500 | gpa, creditsCompleted, priorInternships, minGpa, minCredits missing: HTTP 500 "Unexpected error" | Fail | `EligibilityContractTest.missingRequiredField` | |
| TC-APP-010 | APP-04 | Applications | API | F | Equivalence partitioning, special values (rounding) | Engine running | POST /match | full match; 2/3 + 1/2; 1/2 + 1/4 (42.5); no skills | 100, 62, 43, 0 | As expected | Pass | `MatchContractTest.formula` | |
| TC-APP-011 | APP-04, APP-07 | Applications | API | F | Boundary value analysis (empty list) | Engine running | POST /match with empty or omitted nice-to-have list | 1/1, 1/2, 0/1 required | 70, 35, 0 (second term is 0) | 100, 65, 30: 30 points added when the posting has no nice-to-have skills | Fail | `MatchContractTest.emptyNiceToHaveList`, `omittedNiceToHaveList` | |
| TC-APP-012 | APP-04 | Applications | API | F | Equivalence partitioning (case, spaces) | Engine running | POST /match | "JAVA", "Java", " java", "java ", "  Java  " vs "java" | Each matches (score 70) | Score 0: comparison is case and space sensitive | Fail | `MatchContractTest.caseAndSpaceInsensitive` | |
| TC-APP-013 | APP-09 | Applications | API | F | Decision table (sort keys) | Engine running | POST /rank | distinct scores; equal scores with GPA 2.5, 3.5, 3.0 | Score desc, then GPA desc: [2, 3, 1] | Score order correct; GPA order ascending: [1, 3, 2] | Fail | `RankContractTest.scoreDescending`, `gpaTieBreakDescending` | |
| TC-APP-014 | APP-09 | Applications | API | F | Decision table, special values (time zones) | Engine running | POST /rank | equal score and GPA, different submittedAt; offsets +03:00 vs none | Earliest first, then lowest id; no offset = UTC | As expected | Pass | `RankContractTest.submissionThenIdTieBreak`, `offsetsAreRespected` | |
| TC-APP-015 | APP-09 | Applications | API | F | Boundary value analysis (seats) | Engine running | POST /rank, 10 candidates | seats 1, 3, 5 (with 1 candidate) | `aboveCut` has exactly `seats` ids | `aboveCut` has seats + 1 ids | Fail | `RankContractTest.aboveCutHasSeatsEntries`, `fewerCandidatesThanSeats` | |
| TC-APP-016 | APP-09, REL-02 | Applications | API | F | Boundary value analysis (list size) | Engine running | POST /rank | 500 and 501 candidates; seats 0 | 500 accepted; 501 and seats 0 give 400 | As expected | Pass | `RankContractTest.inputBounds` | |
| TC-APP-017 | PERF-02 | Applications | API | NF (performance) | Load measurement, p95 over 20 runs after 5 warm-up | Engine running | Repeat POST /eligibility, /match, /rank (200 candidates) | fixed seed data | p95 ≤ 200 ms (eligibility, match), ≤ 500 ms (rank 200) | eligibility and match < 5 ms; rank 200: p95 1,843 ms | Fail | `EnginePerformanceTest` | |
| TC-INT-001 | INT-01, INT-02 | Interviews | API | F | Boundary value analysis | Engine running | POST /schedule/validate, start 10:00 Sunday | 14, 15, 119, 120, 121 minutes | 15 to 120 valid | 120 minutes refused | Fail | `ScheduleContractTest.durationBoundary` | |
| TC-INT-002 | INT-02 | Interviews | API | F | Boundary value analysis (times) | Engine running | POST /schedule/validate | start 07:59 / 08:00; end 18:00 / 18:01 / 18:59 | Start ≥ 08:00 and end ≤ 18:00 | Ends at 18:01 and 18:59 accepted | Fail | `ScheduleContractTest.workingHours` | |
| TC-INT-003 | INT-02 | Interviews | API | F | Equivalence partitioning (weekday) | Engine running | POST /schedule/validate | Sun, Tue, Thu, Fri, Sat | Sun to Thu valid | As expected | Pass | `ScheduleContractTest.weekdays` | |
| TC-INT-004 | INT-03 | Interviews | API | F | Boundary value analysis (touching intervals) | Engine running | POST /schedule/validate with an adjacent existing slot | recruiter 10:00-11:00 / student 12:00-13:00; candidate 11:00-12:00 | Valid (back-to-back allowed) | Refused as overlap | Fail | `ScheduleContractTest.backToBackRecruiter`, `backToBackStudent` | |
| TC-INT-005 | INT-03 | Interviews | API | F | Equivalence partitioning (overlap classes) | Engine running | POST /schedule/validate | 1-minute overlap (recruiter); contained (student) | Invalid | As expected | Pass | `ScheduleContractTest.overlapRefused` | |
| TC-INT-006 | INT-02 | Interviews | API | F | Special values (time zone) | Engine running | POST /schedule/validate without offset | 05:00-06:00 and 04:30-05:30 (UTC) | 08:00 Doha valid, 07:30 Doha invalid | As expected | Pass | `ScheduleContractTest.noOffsetIsUtc` | |

### Portal (white-box and integration)

| Test case ID | Requirement | Module | Level | Type | Technique | Preconditions | Steps | Test data | Expected result | Actual result | Status | Automated test | Issue |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| TC-APP-020 | APP-02 | Applications | Service | F | Branch coverage of `activeLimitFor` (PROBATION branch), BVA (limit, limit + 1) | Student with GPA 1.80 (PROBATION) | Call `activeLimitFor`; submit a 3rd application with 2 active | limit setting 2 | Limit 2; 3rd application refused | Limit 5: the String "PROBATION" is compared with the `Standing` enum, so the branch is never taken | Fail | `Module2ApplicationRulesTest.probationLimitIsTwo`, `probationStudentCannotSubmitThird` | |
| TC-APP-021 | APP-02 | Applications | Service | F | Condition coverage of `countActive`, equivalence partitioning of states | Student with WITHDRAWN, REJECTED, SUBMITTED, OFFERED applications | Call `countActive`; submit with 5 withdrawn | 2 WITHDRAWN, 1 REJECTED, 1 SUBMITTED, 1 OFFERED | 2 active; withdrawn do not block | 4: WITHDRAWN counted; 5 withdrawn block a new application | Fail | `Module2ApplicationRulesTest.onlyActiveStatesCount`, `withdrawnApplicationsDoNotBlockSubmission` | |
| TC-APP-022 | APP-03 | Applications | Service | F | State-based, condition coverage of the duplicate check | REJECTED (or WITHDRAWN) application to an OPEN posting | Submit again | — | New SUBMITTED application | WITHDRAWN: as expected. REJECTED: refused "You already have an active application" | Fail | `Module2ApplicationRulesTest.reapplyAfterRejection`, `reapplyAfterWithdrawal` | |
| TC-APP-023 | POS-04 | Applications | Service | F | Boundary value analysis (dates) | OPEN posting | Submit with deadline yesterday, today, tomorrow | — | Today and tomorrow accepted, yesterday refused | Deadline today refused (`today.isBefore(deadline)`) | Fail | `Module2ApplicationRulesTest.deadlineDayIsAccepted`, `deadlineNeighbours` | |
| TC-APP-024 | APP-07 | Applications | Service | F | Boundary value analysis | SUBMITTED application, threshold 60 | Shortlist without justification | scores 59, 60, 61 | 59 needs justification; 60 and 61 do not ("at least") | 60 refused: "score 60 is below the shortlist threshold 60" | Fail | `Module2ApplicationRulesTest.scoreEqualToThresholdNeedsNoJustification`, `thresholdNeighbours` | |
| TC-APP-025 | APP-06, APP-08 | Applications | Service | F | State transition testing (valid and invalid transitions to WITHDRAWN) | Application in each state | Student withdraws | ACCEPTED, REJECTED, WITHDRAWN; SUBMITTED, SHORTLISTED, INTERVIEW, OFFERED | Refused from ACCEPTED, REJECTED, WITHDRAWN; allowed from active states | Active states: as expected. ACCEPTED, REJECTED and WITHDRAWN are moved to WITHDRAWN (state machine bypassed) | Fail | `Module2ApplicationRulesTest.acceptedCannotBeWithdrawn`, `withdrawFromFinalStatesRefused`, `withdrawFromActiveStatesAllowed` | |
| TC-APP-026 | APP-01 | Applications | Service | F | Boundary value analysis | OPEN posting | Submit letters | 99, 100, 2,000, 2,001 characters | 100 and 2,000 accepted; 99 and 2,001 refused | As expected | Pass | `Module2ApplicationRulesTest.letterBoundariesAccepted`, `letterBoundariesRefused` | |
| TC-APP-027 | APP-04, POS-01 | Applications | Service | F | Statement coverage of `EngineRequestMapper.toEligibilityRequest` | Posting min GPA 3.00, Settings default 2.00 | Build the eligibility request | — | `minGpa` = 3.00 (the posting's) | 2.00 (Settings default) is sent | Fail | `Module2EngineIntegrationTest.postingMinimumGpaIsSent` | |
| TC-APP-028 | APP-04 | Applications | Service | F | Statement coverage (field never assigned) | Student with 2 prior internships | Build the eligibility request | — | `priorInternships` = 2 | 0 is sent | Fail | `Module2EngineIntegrationTest.priorInternshipsAreSent` | |
| TC-APP-029 | REL-01 | Applications | Integration | NF (reliability) | Fault injection (HTTP 503, connection refused) | Engine stub returns 503 / port closed | Check eligibility | — | Operation refused; never eligible | `EligibilityResult.ok()` returned: student treated as eligible | Fail | `Module2EngineIntegrationTest.eligibilityFailsClosed`, `eligibilityFailsClosedWhenUnreachable` | |
| TC-APP-030 | REL-01, APP-04 | Applications | Integration | NF (reliability) | Fault injection (HTTP 500) | Engine stub returns 500 on /match | Request match score | — | Operation refused | Score 0 returned and stored | Fail | `Module2EngineIntegrationTest.matchFailsClosed` | |
| TC-APP-031 | APP-04, APP-07, APP-09 | Applications | Service | F | Data-flow (cache key definition and use) | Same posting, two students | Score "Java" student, then "Cooking" student | — | 70 and 0 | Both 70: the cache key ignores student skills | Fail | `Module2EngineIntegrationTest.scoreCacheDependsOnStudentSkills` | |

**Summary (24/11/2026):** 35 designed test cases (23 Engine black-box, 12 Portal white-box or integration;
30 functional, 5 non-functional). 12 pass, 23 fail. Automated runs: Engine suite 81 tests (29 failures), Portal
Module 2 tests 27 tests (16 failures).

## Requirement to test traceability matrix

| Requirement | Test cases | Automated tests | Issues found | Covered? |
| --- | --- | --- | --- | --- |
| APP-01 | TC-APP-026 | `Module2ApplicationRulesTest` | | Yes |
| APP-02 | TC-APP-020, TC-APP-021 | `Module2ApplicationRulesTest` | | Yes |
| APP-03 | TC-APP-022 | `Module2ApplicationRulesTest` | | Yes |
| APP-04 | TC-APP-001 to 005, 010 to 012, 027, 028, 031 | `EligibilityContractTest`, `MatchContractTest`, `Module2EngineIntegrationTest` | | Yes |
| APP-06 | TC-APP-025 | `Module2ApplicationRulesTest` | | Partly (withdraw path only) |
| APP-07 | TC-APP-011, TC-APP-024, TC-APP-031 | `Module2ApplicationRulesTest`, `MatchContractTest` | | Yes |
| APP-08 | TC-APP-025 | `Module2ApplicationRulesTest` | | Yes |
| APP-09 | TC-APP-013 to 016, TC-APP-031 | `RankContractTest` | | Yes |
| POS-04 | TC-APP-023 | `Module2ApplicationRulesTest` | | Yes |
| STU-04 | TC-APP-006, TC-APP-007 | `EligibilityContractTest` | | Yes (Engine part) |
| INT-01 | TC-INT-001 | `ScheduleContractTest` | | Yes (Engine part) |
| INT-02 | TC-INT-001 to 003, TC-INT-006 | `ScheduleContractTest` | | Yes (Engine part) |
| INT-03 | TC-INT-004, TC-INT-005 | `ScheduleContractTest` | | Yes (Engine part) |
| REL-01 | TC-APP-029, TC-APP-030 | `Module2EngineIntegrationTest` | | Yes |
| REL-02 | TC-APP-008, 009, 016 | `EligibilityContractTest`, `RankContractTest`, `MatchContractTest` | | Yes |
| PERF-02 | TC-APP-017 | `EnginePerformanceTest` | | Yes |
| APP-05, APP-10 | — | — | | Not yet |
