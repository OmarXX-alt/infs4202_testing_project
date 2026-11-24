package uniplace.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * POST /rank. Oracle: APP-09 and openapi.yaml: score desc, then GPA desc, then earliest submittedAt, then
 * applicationId asc; aboveCut = the first `seats` ids of ranked; at most 500 candidates.
 */
@DisplayName("Engine /rank")
class RankContractTest {

    static String candidate(long id, int score, String gpa, String submittedAt) {
        return "{\"applicationId\":" + id + ",\"score\":" + score + ",\"gpa\":" + gpa + ",\"submittedAt\":\""
                + submittedAt + "\"}";
    }

    static String request(int seats, String... candidates) {
        return "{\"seats\":" + seats + ",\"candidates\":[" + String.join(",", candidates) + "]}";
    }

    static List<Long> ids(EngineApi.Response r, String field) {
        List<Long> out = new ArrayList<>();
        r.body().get(field).forEach(n -> out.add(n.asLong()));
        return out;
    }

    static final String T1 = "2026-11-01T10:00:00+03:00";
    static final String T2 = "2026-11-02T10:00:00+03:00";

    @Test
    @DisplayName("TC-APP-013 [APP-09] candidates are ordered by match score descending")
    void scoreDescending() {
        EngineApi.Response r = EngineApi.post("/rank", request(3,
                candidate(1, 50, "3.0", T1), candidate(2, 90, "2.0", T1), candidate(3, 70, "2.0", T1)));
        assertEquals(200, r.status());
        assertEquals(List.of(2L, 3L, 1L), ids(r, "ranked"));
    }

    @Test
    @DisplayName("TC-APP-013b [APP-09] equal scores are ordered by GPA descending")
    void gpaTieBreakDescending() {
        EngineApi.Response r = EngineApi.post("/rank", request(3,
                candidate(1, 80, "2.5", T1), candidate(2, 80, "3.5", T1), candidate(3, 80, "3.0", T1)));
        assertEquals(200, r.status());
        assertEquals(List.of(2L, 3L, 1L), ids(r, "ranked"));
    }

    @Test
    @DisplayName("TC-APP-014 [APP-09] equal score and GPA: earliest submission first, then lowest id")
    void submissionThenIdTieBreak() {
        EngineApi.Response r = EngineApi.post("/rank", request(3,
                candidate(9, 80, "3.0", T2), candidate(7, 80, "3.0", T1), candidate(4, 80, "3.0", T2)));
        assertEquals(200, r.status());
        assertEquals(List.of(7L, 4L, 9L), ids(r, "ranked"));
    }

    @Test
    @DisplayName("TC-APP-014b [APP-09] submittedAt offsets are respected; no offset means UTC")
    void offsetsAreRespected() {
        // 09:00+03:00 = 06:00Z, which is earlier than 07:00 UTC (no offset).
        EngineApi.Response r = EngineApi.post("/rank", request(1,
                candidate(1, 80, "3.0", "2026-11-01T07:00:00"), candidate(2, 80, "3.0", "2026-11-01T09:00:00+03:00")));
        assertEquals(200, r.status());
        assertEquals(List.of(2L, 1L), ids(r, "ranked"));
    }

    @Test
    @DisplayName("TC-APP-015 [APP-09] aboveCut holds exactly the first `seats` candidates")
    void aboveCutHasSeatsEntries() {
        List<String> c = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            c.add(candidate(i, 100 - i, "3.0", T1));
        }
        EngineApi.Response one = EngineApi.post("/rank", request(1, c.toArray(String[]::new)));
        EngineApi.Response three = EngineApi.post("/rank", request(3, c.toArray(String[]::new)));
        assertEquals(List.of(1L), ids(one, "aboveCut"), "seats=1");
        assertEquals(List.of(1L, 2L, 3L), ids(three, "aboveCut"), "seats=3");
    }

    @Test
    @DisplayName("TC-APP-015b [APP-09] fewer candidates than seats: all are above the cut")
    void fewerCandidatesThanSeats() {
        EngineApi.Response r = EngineApi.post("/rank", request(5, candidate(1, 50, "3.0", T1)));
        assertEquals(List.of(1L), ids(r, "aboveCut"));
    }

    @Test
    @DisplayName("TC-APP-016 [APP-09, REL-02] input bounds: 500 candidates accepted, 501 and seats=0 refused with 400")
    void inputBounds() {
        assertEquals(200, EngineApi.post("/rank", RankLoad.request(500, 3)).status());
        EngineApi.Response tooMany = EngineApi.post("/rank", RankLoad.request(501, 3));
        assertEquals(400, tooMany.status());
        EngineApi.Response noSeats = EngineApi.post("/rank", request(0, candidate(1, 50, "3.0", T1)));
        assertEquals(400, noSeats.status());
        assertEquals("seats", noSeats.body().path("field").asText());
    }
}
