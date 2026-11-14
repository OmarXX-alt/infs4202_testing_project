# UniPlace Test Plan: Work Breakdown and Schedule

| | |
|---|---|
| **Document** | `docs/test-plan.md` |
| **Version** | 1.1 |
| **Date** | 14 Nov 2026 |
| **Governing document** | `docs/test-strategy.md` (scope, risks, techniques, criteria) |
| **Delivery date** | Sat 28 Nov 2026 |
| **Internal freeze** | Wed 25 Nov 2026, with 26 to 27 Nov held as contingency |

## 1. Planning assumptions

| Item | Assumption |
|---|---|
| Team | Three QA engineers (QA-1, QA-2, QA-3), each leading one primary module |
| Capacity | About 3 to 4 focused hours per engineer per working day (Sunday to Thursday); Friday and Saturday are flexible capacity, not committed capacity |
| Dates | Fixed: freeze 25 Nov, delivery 28 Nov |
| Environment | Every engineer runs a private Portal and Engine instance (see strategy section 10) |
| Work order | Risk-first: Critical modules before High, High before Medium |
| Estimates | Planned effort is about 40 hours per engineer in total (mobilisation 7, first wave 10, second wave 10, consolidation 8, contingency 5) |

## 2. Final deliverables and how the plan links to them

The engagement ends with one consistent delivery package. Every work package in section 4 exists to produce one of these items.

| # | Deliverable | Location | Content at delivery | Work packages | Ready by | Owner |
|---|---|---|---|---|---|---|
| D1 | Test strategy | `docs/test-strategy.md` | Risks, scope, levels, techniques, tools, roles, Git workflow; change log showing updates made during the work | WP-01, 03, 17 | v1.0 15 Nov; final 25 Nov | All |
| D2 | Test cases | `docs/test-cases.md` or `.xlsx` | At least 15 cases for each of the three primary modules (minimum required: 10), functional and non-functional, black-box and white-box, each traced to a requirement ID; cases of other modules tested | WP-03, 06, 14 | G1 19 Nov; final 25 Nov | Each, own module |
| D3 | Automated tests | `portal/src/test/`, `tests/` | White-box Portal tests; black-box Engine contract tests; integration, performance and AI-evaluation suites; commands and prerequisites in `tests/README.md`; results and coverage | WP-02, 07 to 13, 16 | G1 19 Nov (two levels); G2 23 Nov (all) | Each, own suites |
| D4 | Defect reports | GitHub Issues | At least 10 (planned 25 or more), each with the full template, category, severity and priority labels, evidence and assignee; reproduced by a second engineer | WP-05, 14, 15, 16 | 10 by G1; 20 by G2; final 25 Nov | Discoverer |
| D5 | AI usage log | `docs/ai-usage-log.md` | Dated entry for each meaningful use | WP-03, 12 | Continuous; final 25 Nov | All |
| D6 | Final report | `docs/final-report.pdf` and team submission | At most 15 pages plus appendices: system understanding, process, test design and execution, findings, root causes with recommendations, use of AI, lessons learned | WP-17, 18 | Draft 24 Nov; final 25 Nov | By section (table below) |
| D7 | Results readout | Presentation material | About 4 minutes for the team, then about 2 minutes per engineer (10 minutes in total) and questions; each engineer demonstrates one of their own defects live on the running system | WP-18 | 25 Nov; rehearsal 26 Nov | All |
| D8 | Release | Repository, tag `final` | Tagged state with documents, tests and report; collaborators set; no secrets and no `portal/data/` committed | WP-19 | 27 Nov | QA-2 |

### Delivery package

```
repository (tag: final)
  docs/
    test-strategy.md          D1
    test-plan.md
    test-cases.md | .xlsx     D2
    ai-usage-log.md           D5
    final-report.pdf          D6
    reverse-engineering/      system analysis
  tests/                      D3 (README.md, engine-contract, integration, performance, ai-evaluation, security)
  portal/src/test/            D3 (white-box additions beside vendor tests)
  GitHub Issues               D4
```

### Report sections and their sources

| Report section | Owner | Built from |
|---|---|---|
| System understanding | QA-1 | `docs/reverse-engineering/`, strategy sections 2 and 4, contract probes |
| Process | QA-1 | Strategy and change log, Git history, pull requests, CI runs, board |
| Test design and execution | QA-1 | `docs/test-cases.*`, `tests/README.md`, suite results and coverage |
| Findings | QA-2 | Issues table (number, title, category, severity, requirement, discoverer); totals by category and severity; duplicates and invalid |
| Root causes and recommendations | QA-2 | WP-17 groups, each linked to issues and prioritised |
| Use of AI | QA-3 | `docs/ai-usage-log.md`, AI evaluation results |
| Lessons learned | QA-3 | Change log, checkpoint reviews |

### Consistency rule

Every figure in the report and the readout (test counts, pass rates, coverage, defects by severity and category, duplicates and invalid reports) is taken from the repository and from GitHub Issues on the day of the freeze, and re-checked on 26 November.

## 3. Ownership

| Engineer | Primary module | Other responsibilities |
|---|---|---|
| **QA-1** | Applications and Eligibility Decisions (`TC-APP`, `TC-INT`) | Engine contract tests for `/eligibility`, `/match`, `/rank`, `/schedule/validate`; complexity and CK analysis; list-page volume tests; specification review of APP, INT and POS |
| **QA-2** | Offers and Allowance (`TC-OFF`) | Engine `/allowance` contract tests and oracle; concurrency; restart and durability; failure-injection tests; load and stress; CI extension; specification review of OFF, NOT, ADM, PERF and REL |
| **QA-3** | Users and Access Control (`TC-USR`) | Security probes; upload validation; AI evaluation; administration and settings; exploratory charters; test-case workbook and clarification register; specification review of USR, STU, COM, AI, SEC and USA |

**Shared:** defect triage, strategy upkeep, final report, readout. Every defect is reproduced by a second engineer; every pull request is reviewed by a second engineer.

| Activity | QA-1 | QA-2 | QA-3 |
|---|---|---|---|
| Test cases and automation for own module | R/A | R/A | R/A |
| Engine contract suite | R/A (4 endpoints) | R/A (`/allowance`) | C |
| Failure injection and CI | C | R/A | C |
| AI evaluation | C | C | R/A |
| Performance scripts | R (volume) | R/A (load, stress) | C |
| Clarification register | C | C | R/A |
| Final report assembly | R/A (sections 1 to 3) | R/A (sections 4 and 5) | R/A (AI, lessons, appendices) |

(R = responsible, A = accountable, C = consulted.)

## 4. Work breakdown

| ID | Work package | Owner | Output | Depends on |
|---|---|---|---|---|
| WP-01 | Environment, repository hygiene, labels, branch protection, CI check | All | Three reproducible environments; protected `main` | none |
| WP-02 | Test-suite skeletons: `tests/engine-contract`, `tests/integration`, `tests/README.md`, `run-all` scripts | QA-1 (contract), QA-2 (integration, README) | Skeletons that run and report | WP-01 |
| WP-03 | Test-case workbook, AI usage log, defect board, clarification register | QA-3 | `docs/test-cases.*`, `docs/ai-usage-log.md`, board | WP-01 |
| WP-04 | Specification review per requirement group | Each, by group | Clarifications and specification defects | WP-03 |
| WP-05 | Verification of Critical-module leads (strategy Appendix B) | Each, own module | Confirmed or discarded leads; first defect reports | WP-01 |
| WP-06 | Test design for primary modules (at least 15 each) | Each, own module | Test cases with traceability | WP-03, WP-05 |
| WP-07 | Portal white-box tests | Each, own module | Tests plus branch-coverage report for targeted methods | WP-02 |
| WP-08 | Engine contract suite (about 60 rows) | QA-1, QA-2 | Passing and failing rows, each tied to a clause | WP-02 |
| WP-09 | Security matrix and gray-box probes | QA-3 | Automated matrix; probe scripts | WP-02 |
| WP-10 | Failure-injection tests (strategy 8.4) | QA-2 | Matrix results | WP-02 |
| WP-11 | Concurrency and durability scenarios | QA-2 | Repeatable scenarios with failure rates | WP-07 |
| WP-12 | AI evaluation (deterministic checks, repeated real-model runs) | QA-3 | Harness, run results | WP-03 |
| WP-13 | Performance tests (volume, load, stress) | QA-1, QA-2 | k6 scripts and percentile reports | WP-02 |
| WP-14 | Exploratory charters X-01 to X-06 | QA-3 (lead), others by charter | Session notes and defects | WP-03 |
| WP-15 | Review of vendor tests and CI | QA-1 | Findings in category `tests` | WP-01 |
| WP-16 | Regression tests for confirmed defects | Each, own defects | Tests linked to issues | WP-05 |
| WP-17 | Root-cause analysis and recommendations | All | Grouped causes linked to issues | G2 |
| WP-18 | Final report (at most 15 pages plus appendices) and readout | All, by section | PDF in `docs/final-report.pdf`; readout material | WP-17 |
| WP-19 | Reconciliation and tag | All | Numbers consistent across repository, issues and report; tag `final` | WP-18 |

## 5. Daily schedule

| Date | QA-1 | QA-2 | QA-3 | Team events |
|---|---|---|---|---|
| **Sun 15 Nov** | Set up environment; contract-suite skeleton and health test (WP-01, 02) | Set up environment; README, run-all and integration skeletons; CI check (WP-01, 02) | Set up environment; test-case workbook, AI log, board, clarification register (WP-01, 03) | Strategy v1.0 committed by pull request; stand-up |
| **Mon 16 Nov** | Verify leads H-11, 12, 20 to 26; spec review of APP, INT, POS (WP-04, 05) | Verify leads H-13, 14, 36, 41; spec review of OFF, NOT, ADM, PERF, REL (WP-04, 05) | Verify leads H-01 to 08, 10, 15; spec review of USR, STU, COM, AI, SEC, USA; configure AI provider and fix the model (WP-04, 05) | **Gate G0:** environments reproducible; first confirmed defects filed; clarifications raised |
| **Tue 17 Nov** | Application state machine, all 49 pairs; eligibility decision table (WP-06, 07, 08) | Allowance oracle and boundary rows; offer-service white-box tests (WP-06, 07, 08) | Role × route matrix; XSS probes across roles (WP-06, 09) | Stand-up; **triage 1** |
| **Wed 18 Nov** | Match and rank contract rows; mapper and score-cache tests (H-20, 21, 24); reach 15 test cases (WP-06, 08) | Last-seat concurrency test; response-deadline tests (H-13); reach 15 test cases (WP-06, 11) | Upload validation, session, lockout and CSRF tests; AI harness skeleton; first real-model runs (WP-06, 09, 12) | Stand-up |
| **Thu 19 Nov** | Execute all test cases; coverage baseline; file defects | Execute all test cases; file defects | Execute all test cases; file defects | **Gate G1;** triage 2; strategy v1.1 |
| **Fri 20 Nov** (flex) | `/schedule/validate` rows and `TC-INT` cases (WP-08) | Failure-injection matrix with WireMock and with the Engine stopped (WP-10) | Prompt, PII and injection checks with WireMock; real-model runs (WP-12) | |
| **Sat 21 Nov** (flex) | Volume data and list-page performance script (WP-13) | Load and stress scripts (WP-13) | Charters X-03 and X-04 (WP-14) | |
| **Sun 22 Nov** | Complexity and CK report; white-box additions to reach 80% branch coverage on targeted methods; vendor-test review (WP-07, 15) | Restart and durability scenarios; expiry and reminder jobs; reminder timing (WP-11, 06) | Settings immediacy; reports and CSV; charters X-01, X-02, X-05, X-06; real-model runs (WP-14, 12) | Stand-up |
| **Mon 23 Nov** | Regression tests for own confirmed defects; metrics snapshot | Regression tests; CI extended with contract suite | Regression tests; analysis of AI runs | **Gate G2;** triage 3; root-cause grouping begins |
| **Tue 24 Nov** | Report sections 1 to 3; quality read of all own issues | Report sections 4 and 5; quality read of all own issues | AI section, lessons, appendices; quality read of all own issues | Stand-up; cross-read of every issue by a second engineer |
| **Wed 25 Nov** | Final numbers reconciled; readout material; live walkthrough ready | Final numbers reconciled; readout material; live walkthrough ready | Final numbers reconciled; readout material; live walkthrough ready | **Freeze at end of day:** documents final; release candidate built |
| **Thu 26 Nov** | Contingency: reproduce every defect again on a fresh start | Contingency: CI green; tests run from `tests/README.md` on a clean checkout | Contingency: documents proofread; strategy change log complete | Readout rehearsal |
| **Fri 27 Nov** | Contingency | Contingency; tag `final` | Contingency | Final check against the delivery checklist |
| **Sat 28 Nov** | Delivery | Delivery | Delivery | Submit; system running with demonstration data |

## 6. Checkpoints

| Gate | Date | Criteria |
|---|---|---|
| **G0** | 16 Nov | Three environments start with `--reset`; protected `main` and CI in place; first confirmed defects filed; clarification register opened; AI model fixed |
| **G1** | 19 Nov | At least 15 test cases per primary module executed; at least two automated levels running through documented commands; at least 10 defects filed with full templates and second reproduction; AI log current |
| **G2** | 23 Nov | Every Critical module executed with at least two techniques; failure-injection, concurrency, performance and AI results available; at least 20 defects filed; vendor-test review done |
| **Freeze** | 25 Nov | Exit criteria in strategy section 14.2 met; report and readout material complete; numbers reconcile across repository, issues and report |
| **Delivery** | 28 Nov | Tag `final`; documents and report in place; system ready for live demonstration |

Defect-filing targets (cumulative, not quotas): at least 10 by 19 Nov, at least 20 by 23 Nov, and at least 25 by the freeze. Quality (valid, reproducible, correctly classified) takes precedence over count.

## 7. Cadence and coordination

| Item | Arrangement |
|---|---|
| Stand-up | 15 minutes, working days: done, next, blockers, any area to claim |
| Defect triage | 17 Nov, 19 Nov, 23 Nov: confirm severity and priority, close duplicates and invalid reports |
| Board columns | Candidate lead → Under investigation → Reproduced (second engineer) → Filed → Retested |
| Reviews | Pull requests reviewed within one working day |
| Escalation | A blocker unresolved for a day is raised at the next stand-up and replanned |

## 8. Dependencies and critical path

1. **WP-01 → WP-02 → WP-08, 07, 10:** skeletons must run by Mon 16 Nov, or contract and integration work slips.
2. **WP-05 → WP-06:** lead verification feeds test design; started first because Critical leads need early evidence.
3. **WP-12 (AI real-model runs):** limited by the daily provider quota, so runs start on 18 Nov and continue through 22 Nov.
4. **WP-17 → WP-18:** root-cause grouping starts at G2; the report cannot be completed earlier.

## 9. Schedule risks and contingency

| Risk | Response |
|---|---|
| Skeleton or environment problems on one machine | Pair on the setup the same day; fall back to the documented `--jar` start |
| A Critical lead is not reproducible | Record as discarded with evidence; move capacity to the next lead |
| Test cases or automation behind at G1 | Cut optional items first (UI journey automation, mutation testing, ZAP scan); never cut Critical-module execution |
| Many leads confirmed at once, filing backlog | Prioritise Critical, then Major; file Minor in batches; keep second-reproduction rule |
| Report or readout late | The 24 Nov cross-read and 26 to 27 Nov contingency are reserved for this; scope cuts apply to appendices only |

## 10. Definition of done

| Artefact | Done when |
|---|---|
| Test case | Traceable to a requirement; expected result from the requirement; executed; status recorded; linked to its automated test and issue where they exist |
| Automated test | Runs from the repository with a documented command; cites its test-case ID; reviewed in a pull request; passes or fails for a documented reason |
| Defect report | Reproduced by a second engineer on a fresh start; all mandatory fields completed; labels and assignee set; evidence attached; no duplicate |
| Root-cause group | Lists the defects in it, the cause, why it escaped the vendor's process, and recommendations linked to those defects with a priority |
| Report and readout | Every number matches the repository and the issues on the day |

## 11. Change log

| Version | Date | Change | Reason |
|---|---|---|---|
| 1.0 | 14 Nov 2026 | Initial work breakdown and schedule | Strategy v1.0 approved |
| 1.1 | 14 Nov 2026 | Added section 2, final deliverables and their link to the work packages | Make the target delivery package explicit |
