package com.halcyon.uniplace.service;

import com.halcyon.uniplace.domain.Application;
import com.halcyon.uniplace.domain.ApplicationState;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.CompanyStatus;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Standing;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.domain.User;
import com.halcyon.uniplace.engine.EngineClient;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityResult;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import com.halcyon.uniplace.KnownDefect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Module 2, Applications and Eligibility: white-box tests of ApplicationService and PostingService.
 * Each test targets a branch identified by reading the code; the expected result comes from the
 * Requirements (never from the current behaviour). The Engine is mocked so that only Portal logic is tested.
 */
@DisplayName("Module 2: application rules (Portal, white-box)")
class Module2ApplicationRulesTest extends IntegrationTest {

    static final String LETTER = "I am applying because this internship matches my studies in information systems "
            + "and my experience with databases in two course projects.";

    @Autowired ApplicationService service;
    @Autowired PostingService postingService;
    @MockBean EngineClient engine;

    Company company;
    Posting posting;
    Student student;
    User studentUser;
    User recruiter;

    @BeforeEach
    void setUp() {
        company = company("QTech", CompanyStatus.APPROVED);
        posting = openPosting(company, "Backend Intern", List.of("Java", "SQL"), 2);
        student = student("3.20", List.of("Java", "SQL"));
        studentUser = studentUser(student);
        recruiter = recruiter(company);
        validDocuments(student);
        when(engine.checkEligibility(any())).thenReturn(EligibilityResult.ok());
        when(engine.match(any())).thenReturn(new MatchResult(70, List.of("Java", "SQL"), List.of(), List.of()));
    }

    // ------------------------------------------------------------------ APP-02 active-application limits

    @Tag(KnownDefect.TAG) // issue #6
    @Test
    @DisplayName("TC-APP-020 [APP-02] a student on PROBATION may hold at most 2 active applications (Settings) (defect #6)")
    void probationLimitIsTwo() {
        Student weak = student("1.80", List.of("Excel"));
        assertEquals(Standing.PROBATION, weak.getStanding(), "precondition: GPA 1.80 gives PROBATION (STU-03)");

        // Branch: activeLimitFor() compares the String "PROBATION" with the Standing enum.
        assertEquals(2, service.activeLimitFor(weak));
    }

    @Tag(KnownDefect.TAG) // issue #6
    @Test
    @DisplayName("TC-APP-020b [APP-02] a third application from a PROBATION student is refused (defect #6)")
    void probationStudentCannotSubmitThird() {
        Student weak = student("1.80", List.of("Java", "SQL"));
        User weakUser = studentUser(weak);
        validDocuments(weak);
        application(weak, openPosting(company, "P1", List.of("Java"), 1), ApplicationState.SUBMITTED, 50);
        application(weak, openPosting(company, "P2", List.of("Java"), 1), ApplicationState.SHORTLISTED, 50);

        assertThrows(BusinessRuleException.class, () -> service.submit(weakUser, posting.getId(), LETTER));
    }

    @Tag(KnownDefect.TAG) // issue #7
    @Test
    @DisplayName("TC-APP-021 [APP-02] WITHDRAWN and REJECTED applications are not active and do not count (defect #7)")
    void onlyActiveStatesCount() {
        application(student, openPosting(company, "W1", List.of("Java"), 1), ApplicationState.WITHDRAWN, 40);
        application(student, openPosting(company, "W2", List.of("Java"), 1), ApplicationState.WITHDRAWN, 40);
        application(student, openPosting(company, "R1", List.of("Java"), 1), ApplicationState.REJECTED, 40);
        application(student, openPosting(company, "S1", List.of("Java"), 1), ApplicationState.SUBMITTED, 40);
        application(student, openPosting(company, "O1", List.of("Java"), 1), ApplicationState.OFFERED, 40);

        // Active = SUBMITTED, SHORTLISTED, INTERVIEW, OFFERED.
        assertEquals(2, service.countActive(student));
    }

    @Tag(KnownDefect.TAG) // issue #7
    @Test
    @DisplayName("TC-APP-021b [APP-02] a GOOD student with 4 withdrawn applications can still apply (defect #7)")
    void withdrawnApplicationsDoNotBlockSubmission() {
        for (int i = 0; i < 5; i++) {
            application(student, openPosting(company, "Old " + i, List.of("Java"), 1), ApplicationState.WITHDRAWN, 40);
        }

        assertDoesNotThrow(() -> service.submit(studentUser, posting.getId(), LETTER));
    }

    // ------------------------------------------------------------------ APP-03 re-application

    @Tag(KnownDefect.TAG) // issue #9
    @Test
    @DisplayName("TC-APP-022 [APP-03] after a REJECTED application the student may apply again to the OPEN posting (defect #9)")
    void reapplyAfterRejection() {
        application(student, posting, ApplicationState.REJECTED, 50);

        Application again = assertDoesNotThrow(() -> service.submit(studentUser, posting.getId(), LETTER));
        assertEquals(ApplicationState.SUBMITTED, again.getState());
    }

    @Test
    @DisplayName("TC-APP-022b [APP-03] after a WITHDRAWN application the student may apply again")
    void reapplyAfterWithdrawal() {
        application(student, posting, ApplicationState.WITHDRAWN, 50);

        assertDoesNotThrow(() -> service.submit(studentUser, posting.getId(), LETTER));
    }

    // ------------------------------------------------------------------ POS-04 deadline is inclusive

    @Tag(KnownDefect.TAG) // issue #8
    @Test
    @DisplayName("TC-APP-023 [POS-04] an application on the deadline date is accepted (deadline is inclusive) (defect #8)")
    void deadlineDayIsAccepted() {
        posting.setDeadline(today());
        postings.save(posting);

        assertEquals(true, postingService.acceptsApplications(posting, today()), "deadline = today");
        assertDoesNotThrow(() -> service.submit(studentUser, posting.getId(), LETTER));
    }

    @Test
    @DisplayName("TC-APP-023b [POS-04] boundary: deadline tomorrow accepted, deadline yesterday refused")
    void deadlineNeighbours() {
        posting.setDeadline(today().plusDays(1));
        assertEquals(true, postingService.acceptsApplications(posting, today()), "deadline = tomorrow");
        posting.setDeadline(today().minusDays(1));
        assertEquals(false, postingService.acceptsApplications(posting, today()), "deadline = yesterday");
    }

    // ------------------------------------------------------------------ APP-07 shortlist threshold

    @Tag(KnownDefect.TAG) // issue #10
    @Test
    @DisplayName("TC-APP-024 [APP-07] a score equal to the threshold (60) is shortlisted without justification (defect #10)")
    void scoreEqualToThresholdNeedsNoJustification() {
        Application a = application(student, posting, ApplicationState.SUBMITTED, 60);

        assertDoesNotThrow(() -> service.shortlist(recruiter, a.getId(), null));
        assertEquals(null, applications.findById(a.getId()).orElseThrow().getOverrideJustification());
    }

    @Test
    @DisplayName("TC-APP-024b [APP-07] boundary: 59 needs a justification, 61 does not")
    void thresholdNeighbours() {
        Application below = application(student, posting, ApplicationState.SUBMITTED, 59);
        Application above = application(student(
                "3.00", List.of("Java")), posting, ApplicationState.SUBMITTED, 61);

        assertThrows(BusinessRuleException.class, () -> service.shortlist(recruiter, below.getId(), null));
        assertDoesNotThrow(() -> service.shortlist(recruiter, above.getId(), null));
    }

    // ------------------------------------------------------------------ APP-06 / APP-08 withdraw

    @Tag(KnownDefect.TAG) // issue #11
    @Test
    @DisplayName("TC-APP-025 [APP-08, APP-06] an ACCEPTED application cannot be withdrawn (defect #11)")
    void acceptedCannotBeWithdrawn() {
        Application a = application(student, posting, ApplicationState.ACCEPTED, 80);

        assertThrows(BusinessRuleException.class, () -> service.withdraw(studentUser, a.getId()));
        assertEquals(ApplicationState.ACCEPTED, applications.findById(a.getId()).orElseThrow().getState());
    }

    @Tag(KnownDefect.TAG) // issue #11
    @ParameterizedTest(name = "{0} -> WITHDRAWN is refused")
    @EnumSource(value = ApplicationState.class, names = {"REJECTED", "WITHDRAWN"})
    @DisplayName("TC-APP-025b [APP-06] withdraw is refused from states with no transition to WITHDRAWN (defect #11)")
    void withdrawFromFinalStatesRefused(ApplicationState from) {
        Application a = application(student, posting, from, 50);

        assertThrows(BusinessRuleException.class, () -> service.withdraw(studentUser, a.getId()));
    }

    @ParameterizedTest(name = "{0} -> WITHDRAWN is allowed")
    @EnumSource(value = ApplicationState.class, names = {"SUBMITTED", "SHORTLISTED", "INTERVIEW", "OFFERED"})
    @DisplayName("TC-APP-025c [APP-08] a student may withdraw any active application")
    void withdrawFromActiveStatesAllowed(ApplicationState from) {
        Application a = application(student, posting, from, 50);

        service.withdraw(studentUser, a.getId());

        assertEquals(ApplicationState.WITHDRAWN, applications.findById(a.getId()).orElseThrow().getState());
    }

    // ------------------------------------------------------------------ APP-01 letter length (BVA)

    @ParameterizedTest(name = "letter of {0} characters")
    @org.junit.jupiter.params.provider.ValueSource(ints = {100, 2000})
    @DisplayName("TC-APP-026 [APP-01] letters of exactly 100 and 2,000 characters are accepted")
    void letterBoundariesAccepted(int length) {
        assertDoesNotThrow(() -> service.submit(studentUser, posting.getId(), "x".repeat(length)));
    }

    @ParameterizedTest(name = "letter of {0} characters")
    @org.junit.jupiter.params.provider.ValueSource(ints = {99, 2001})
    @DisplayName("TC-APP-026b [APP-01] letters of 99 and 2,001 characters are refused")
    void letterBoundariesRefused(int length) {
        assertThrows(BusinessRuleException.class,
                () -> service.submit(studentUser, posting.getId(), "x".repeat(length)));
    }
}
