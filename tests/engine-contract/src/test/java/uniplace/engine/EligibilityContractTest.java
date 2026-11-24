package uniplace.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * POST /eligibility. Module 2 (Applications and Eligibility). Oracle: Requirements APP-04, STU-04, REL-02 and
 * engine/openapi.yaml. Expected values are written from the requirement, never from observed output.
 */
@DisplayName("Engine /eligibility")
class EligibilityContractTest {

    static final String AS_OF = "2026-11-24";
    static final String CV_OK = doc("CV", 100_000, "application/pdf", null);
    static final String TRANSCRIPT_OK = doc("TRANSCRIPT", 200_000, "application/pdf", "2026-11-01");

    static String doc(String type, long size, String mime, String issueDate) {
        return "{\"type\":\"" + type + "\",\"sizeBytes\":" + size + ",\"mimeType\":\"" + mime + "\""
                + (issueDate == null ? "" : ",\"issueDate\":\"" + issueDate + "\"") + "}";
    }

    static String request(String gpa, int credits, String standing, int prior, String minGpa, int minCredits,
                          String asOf, String... docs) {
        return "{\"gpa\":" + gpa + ",\"creditsCompleted\":" + credits + ",\"standing\":\"" + standing + "\""
                + ",\"priorInternships\":" + prior + ",\"minGpa\":" + minGpa + ",\"minCredits\":" + minCredits
                + ",\"asOf\":\"" + asOf + "\",\"documents\":[" + String.join(",", docs) + "]}";
    }

    static String validStudent(String gpa, String minGpa) {
        return request(gpa, 60, "GOOD", 0, minGpa, 30, AS_OF, CV_OK, TRANSCRIPT_OK);
    }

    static List<String> reasons(EngineApi.Response r) {
        List<String> out = new ArrayList<>();
        r.body().get("reasons").forEach(n -> out.add(n.asText()));
        return out;
    }

    @Test
    @DisplayName("TC-APP-001 [APP-04] a student who meets every rule is eligible with no reasons")
    void eligibleStudent() {
        EngineApi.Response r = EngineApi.post("/eligibility", validStudent("3.20", "2.50"));
        assertEquals(200, r.status());
        assertTrue(r.bool("eligible"));
        assertTrue(reasons(r).isEmpty());
    }

    @ParameterizedTest(name = "gpa={0}, minGpa={1} -> eligible={2}")
    @CsvSource({
            "2.49, 2.50, false",
            "2.50, 2.50, true",   // boundary: "GPA at least minGpa"
            "2.51, 2.50, true",
            "3.00, 3.00, true",   // boundary
            "2.00, 2.00, true"    // boundary
    })
    @DisplayName("TC-APP-002 [APP-04] GPA boundary: a GPA equal to the minimum is eligible")
    void gpaBoundary(String gpa, String minGpa, boolean eligible) {
        EngineApi.Response r = EngineApi.post("/eligibility", validStudent(gpa, minGpa));
        assertEquals(200, r.status());
        assertEquals(eligible, r.bool("eligible"), "reasons: " + reasons(r));
    }

    @ParameterizedTest(name = "credits={0}, minCredits=30 -> eligible={1}")
    @CsvSource({"29, false", "30, true", "31, true"})
    @DisplayName("TC-APP-003 [APP-04] credits boundary: credits equal to the minimum are eligible")
    void creditsBoundary(int credits, boolean eligible) {
        EngineApi.Response r = EngineApi.post("/eligibility",
                request("3.00", credits, "GOOD", 0, "2.00", 30, AS_OF, CV_OK, TRANSCRIPT_OK));
        assertEquals(200, r.status());
        assertEquals(eligible, r.bool("eligible"), "reasons: " + reasons(r));
    }

    @Test
    @DisplayName("TC-APP-004 [APP-04] every failed rule is reported, not only the first one")
    void everyFailedRuleIsReported() {
        EngineApi.Response r = EngineApi.post("/eligibility", request("1.00", 10, "GOOD", 0, "2.50", 30, AS_OF));
        assertEquals(200, r.status());
        assertFalse(r.bool("eligible"));
        assertEquals(4, reasons(r).size(), "GPA, credits, CV missing, transcript missing: " + reasons(r));
    }

    @ParameterizedTest(name = "standing={0}, priorInternships={1} -> eligible={2}")
    @CsvSource({"GOOD, 3, true", "PROBATION, 0, true", "PROBATION, 1, true", "PROBATION, 2, false"})
    @DisplayName("TC-APP-005 [APP-04] decision table: a student on probation has at most 1 prior internship")
    void probationPriorInternships(String standing, int prior, boolean eligible) {
        EngineApi.Response r = EngineApi.post("/eligibility",
                request("3.00", 60, standing, prior, "2.00", 30, AS_OF, CV_OK, TRANSCRIPT_OK));
        assertEquals(200, r.status());
        assertEquals(eligible, r.bool("eligible"), "reasons: " + reasons(r));
    }

    /** Issue date 2026-01-01 is day 1, so day 180 is 2026-06-29 and day 181 is 2026-06-30. */
    @ParameterizedTest(name = "asOf={0} -> valid={1}")
    @CsvSource({
            "2025-12-31, false",  // before the issue date
            "2026-01-01, true",   // day 1
            "2026-06-28, true",   // day 179
            "2026-06-29, true",   // day 180, last valid day
            "2026-06-30, false"   // day 181
    })
    @DisplayName("TC-APP-006 [STU-04] transcript is valid for 180 days counting the issue date as day 1")
    void transcriptValidity(String asOf, boolean valid) {
        EngineApi.Response r = EngineApi.post("/eligibility", request("3.00", 60, "GOOD", 0, "2.00", 30, asOf,
                CV_OK, doc("TRANSCRIPT", 1000, "application/pdf", "2026-01-01")));
        assertEquals(200, r.status());
        assertEquals(valid, r.bool("eligible"), "reasons: " + reasons(r));
    }

    @ParameterizedTest(name = "{0} {1} bytes {2} -> eligible={3}")
    @CsvSource({
            "CV, 2097152, application/pdf, true",
            "CV, 2097153, application/pdf, false",
            "CV, 1000, image/png, false",
            "TRANSCRIPT, 5242880, application/pdf, true",
            "TRANSCRIPT, 5242881, application/pdf, false",
            "TRANSCRIPT, 1000, application/msword, false"
    })
    @DisplayName("TC-APP-007 [STU-04] document type and size boundaries (CV 2 MB, transcript 5 MB, PDF only)")
    void documentBoundaries(String type, long size, String mime, boolean eligible) {
        String cv = type.equals("CV") ? doc("CV", size, mime, null) : CV_OK;
        String tr = type.equals("TRANSCRIPT") ? doc("TRANSCRIPT", size, mime, "2026-11-01") : TRANSCRIPT_OK;
        EngineApi.Response r = EngineApi.post("/eligibility", request("3.00", 60, "GOOD", 0, "2.00", 30, AS_OF, cv, tr));
        assertEquals(200, r.status());
        assertEquals(eligible, r.bool("eligible"), "reasons: " + reasons(r));
    }

    @ParameterizedTest(name = "{0} -> 400 naming {1}")
    @CsvSource(delimiter = '|', value = {
            "{\"gpa\":4.01,\"creditsCompleted\":60,\"standing\":\"GOOD\",\"priorInternships\":0,\"minGpa\":2,\"minCredits\":30,\"asOf\":\"2026-11-24\",\"documents\":[]} | gpa",
            "{\"gpa\":-0.01,\"creditsCompleted\":60,\"standing\":\"GOOD\",\"priorInternships\":0,\"minGpa\":2,\"minCredits\":30,\"asOf\":\"2026-11-24\",\"documents\":[]} | gpa",
            "{\"gpa\":3,\"creditsCompleted\":60,\"standing\":\"EXCELLENT\",\"priorInternships\":0,\"minGpa\":2,\"minCredits\":30,\"asOf\":\"2026-11-24\",\"documents\":[]} | standing",
            "{\"gpa\":3,\"creditsCompleted\":141,\"standing\":\"GOOD\",\"priorInternships\":0,\"minGpa\":2,\"minCredits\":30,\"asOf\":\"2026-11-24\",\"documents\":[]} | creditsCompleted",
            "{\"gpa\":3,\"creditsCompleted\":60,\"standing\":\"GOOD\",\"priorInternships\":0,\"minGpa\":4.5,\"minCredits\":30,\"asOf\":\"2026-11-24\",\"documents\":[]} | minGpa"
    })
    @DisplayName("TC-APP-008 [REL-02] values outside their domain are answered with 400 naming the field")
    void outOfDomainValues(String body, String field) {
        EngineApi.Response r = EngineApi.post("/eligibility", body);
        assertAll(
                () -> assertEquals(400, r.status(), "body: " + r.body()),
                () -> assertEquals(field, r.body().path("field").asText(), "body: " + r.body()));
    }

    @ParameterizedTest(name = "missing {0}")
    @CsvSource({"gpa", "creditsCompleted", "standing", "priorInternships", "minGpa", "minCredits", "asOf"})
    @DisplayName("TC-APP-009 [REL-02] a missing required field gives 400 naming the field, never 500")
    void missingRequiredField(String field) throws Exception {
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) EngineApi.JSON.readTree(validStudent("3.00", "2.00"));
        node.remove(field);
        EngineApi.Response r = EngineApi.post("/eligibility", node.toString());
        assertAll(
                () -> assertEquals(400, r.status(), "body: " + r.body()),
                () -> assertNotNull(r.body()),
                () -> assertEquals(field, r.body().path("field").asText(), "body: " + r.body()));
    }
}
