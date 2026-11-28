package com.halcyon.uniplace.engine;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.halcyon.uniplace.domain.Company;
import com.halcyon.uniplace.domain.Posting;
import com.halcyon.uniplace.domain.Student;
import com.halcyon.uniplace.engine.EngineDtos.EligibilityRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchRequest;
import com.halcyon.uniplace.engine.EngineDtos.MatchResult;
import com.halcyon.uniplace.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import com.halcyon.uniplace.KnownDefect;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Module 2, Portal-to-Engine integration (white-box and fault injection). Covers what the Portal sends to the
 * Engine (APP-04), how it reacts when the Engine fails (REL-01), and the match-score cache (APP-04, APP-07,
 * APP-09). WireMock stands in for the Engine so that failures can be injected deterministically.
 */
@WireMockTest
@DisplayName("Module 2: Portal to Engine integration")
class Module2EngineIntegrationTest {

    final Clock clock = Clock.fixed(Instant.parse("2026-11-20T06:00:00Z"), ZoneId.of("Asia/Qatar"));
    SettingsService settings;
    EngineClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        settings = Mockito.mock(SettingsService.class);
        when(settings.engineBaseUrl()).thenReturn("http://127.0.0.1:" + wm.getHttpPort() + "/api/v1");
        when(settings.getDecimal(SettingsService.DEFAULT_MIN_GPA)).thenReturn(new BigDecimal("2.00"));
        client = new EngineClient(RestClient.builder(), settings);
    }

    static Posting posting(long id, String minGpa, List<String> required) {
        Posting p = new Posting(new Company("QTech", "Technology", "hr@qtech.qa"), "Intern", "d");
        ReflectionTestUtils.setField(p, "id", id);
        p.setRequiredSkills(required);
        p.setNiceToHaveSkills(List.of("Git"));
        p.setMinGpa(new BigDecimal(minGpa));
        p.setMinCredits(60);
        return p;
    }

    static Student student(int priorInternships) {
        return new Student("20990001", "Test Student", "t@s.qa", "BSc", new BigDecimal("1.90"), 75, priorInternships);
    }

    static EligibilityRequest someRequest() {
        EligibilityRequest r = new EligibilityRequest();
        r.gpa = new BigDecimal("3.00");
        r.creditsCompleted = 60;
        r.standing = "GOOD";
        r.minGpa = new BigDecimal("2.00");
        r.minCredits = 45;
        r.asOf = LocalDate.of(2026, 11, 20);
        return r;
    }

    // ------------------------------------------------------------------ APP-04: what is sent

    @Tag(KnownDefect.TAG) // issue #4
    @Test
    @DisplayName("TC-APP-027 [APP-04, POS-01] eligibility uses the posting's own minimum GPA, not the default (defect #4)")
    void postingMinimumGpaIsSent() {
        EngineRequestMapper mapper = new EngineRequestMapper(settings, clock);

        EligibilityRequest r = mapper.toEligibilityRequest(student(0), posting(1, "3.00", List.of("Java")),
                List.of(), LocalDate.of(2026, 11, 20));

        assertEquals(0, new BigDecimal("3.00").compareTo(r.minGpa), "minGpa sent: " + r.minGpa);
    }

    @Tag(KnownDefect.TAG) // issue #5
    @Test
    @DisplayName("TC-APP-028 [APP-04] the student's prior internships are sent to the Engine (defect #5)")
    void priorInternshipsAreSent() {
        EngineRequestMapper mapper = new EngineRequestMapper(settings, clock);

        EligibilityRequest r = mapper.toEligibilityRequest(student(2), posting(1, "2.00", List.of("Java")),
                List.of(), LocalDate.of(2026, 11, 20));

        // A PROBATION student with 2 prior internships must be refused by the Engine; that needs the value.
        assertEquals(2, r.priorInternships);
    }

    // ------------------------------------------------------------------ REL-01: Engine failures

    @Tag(KnownDefect.TAG) // issue #3
    @Test
    @DisplayName("TC-APP-029 [REL-01] Engine error on eligibility: the student is never treated as eligible (defect #3)")
    void eligibilityFailsClosed() {
        stubFor(post(urlEqualTo("/api/v1/eligibility")).willReturn(aResponse().withStatus(503)));

        // Expected: the operation is refused (exception), or at least the student is not eligible.
        boolean eligible;
        try {
            eligible = client.checkEligibility(someRequest()).eligible();
        } catch (EngineUnavailableException expected) {
            eligible = false;
        }
        assertFalse(eligible, "the Portal treated the student as eligible although the Engine did not answer");
    }

    @Tag(KnownDefect.TAG) // issue #3
    @Test
    @DisplayName("TC-APP-029b [REL-01] Engine unreachable on eligibility: the student is never treated as eligible (defect #3)")
    void eligibilityFailsClosedWhenUnreachable() {
        when(settings.engineBaseUrl()).thenReturn("http://127.0.0.1:1/api/v1");
        EngineClient unreachable = new EngineClient(RestClient.builder(), settings);

        boolean eligible;
        try {
            eligible = unreachable.checkEligibility(someRequest()).eligible();
        } catch (EngineUnavailableException expected) {
            eligible = false;
        }
        assertFalse(eligible, "the Portal treated the student as eligible although the Engine was unreachable");
    }

    @Tag(KnownDefect.TAG) // issue #3
    @Test
    @DisplayName("TC-APP-030 [REL-01, APP-04] Engine error on match: refused, not stored as a score of 0 (defect #3)")
    void matchFailsClosed() {
        stubFor(post(urlEqualTo("/api/v1/match")).willReturn(aResponse().withStatus(500)));
        MatchRequest m = new MatchRequest();
        m.studentSkills = List.of("Java");
        m.requiredSkills = List.of("Java");
        m.niceToHaveSkills = List.of();

        assertThrows(EngineUnavailableException.class, () -> client.match(m));
    }

    // ------------------------------------------------------------------ APP-04: score cache

    @Tag(KnownDefect.TAG) // issue #12
    @Test
    @DisplayName("TC-APP-031 [APP-04, APP-07] two students with different skills get different scores for one posting (defect #12)")
    void scoreCacheDependsOnStudentSkills() {
        EngineClient engine = Mockito.mock(EngineClient.class);
        when(engine.match(any())).thenAnswer(inv -> {
            MatchRequest req = inv.getArgument(0);
            return new MatchResult(req.studentSkills.contains("Java") ? 70 : 0, List.of(), List.of(), List.of());
        });
        MatchScoreService scores = new MatchScoreService(engine, new EngineRequestMapper(settings, clock));
        Posting p = posting(7, "2.00", List.of("Java"));

        int strong = scores.score(List.of("Java"), p);
        int weak = scores.score(List.of("Cooking"), p);

        assertEquals(70, strong);
        assertNotEquals(strong, weak, "the second student received the first student's cached score");
    }
}
