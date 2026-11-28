package uniplace.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * POST /schedule/validate. Oracle: INT-01 to INT-03, INT-05 and openapi.yaml: 15 to 120 minutes, Sunday to
 * Thursday, start at or after 08:00, end at or before 18:00 (Asia/Qatar), no overlap, back-to-back allowed.
 * 2026-11-22 is a Sunday.
 */
@DisplayName("Engine /schedule/validate")
class ScheduleContractTest {

    static final String DAY = "2026-11-22";

    static String at(String day, String time) {
        return day + "T" + time + ":00+03:00";
    }

    static String slot(String start, String end) {
        return "{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }

    static String request(String candidate, String recruiterSlots, String studentSlots) {
        return "{\"candidate\":" + candidate + ",\"recruiterSlots\":[" + recruiterSlots + "],\"studentSlots\":["
                + studentSlots + "]}";
    }

    static EngineApi.Response validate(String start, String end, String recruiter, String student) {
        return EngineApi.post("/schedule/validate", request(slot(start, end), recruiter, student));
    }

    @ParameterizedTest(name = "10:00 to {0} -> valid={1}")
    @CsvSource({"10:14, false", "10:15, true", "11:59, true", "12:00, true", "12:01, false"})
    @DisplayName("TC-INT-001 [INT-01] duration boundary: 15 to 120 minutes inclusive")
    void durationBoundary(String end, boolean valid) {
        EngineApi.Response r = validate(at(DAY, "10:00"), at(DAY, end), "", "");
        assertEquals(200, r.status());
        assertEquals(valid, r.bool("valid"), "body: " + r.body());
    }

    @ParameterizedTest(name = "{0} to {1} -> valid={2}")
    @CsvSource({
            "07:59, 08:30, false",
            "08:00, 08:30, true",
            "17:00, 18:00, true",   // ends exactly at 18:00
            "17:01, 18:01, false",  // ends after 18:00
            "17:59, 18:59, false"
    })
    @DisplayName("TC-INT-002 [INT-02] working hours: start at or after 08:00, end at or before 18:00")
    void workingHours(String start, String end, boolean valid) {
        EngineApi.Response r = validate(at(DAY, start), at(DAY, end), "", "");
        assertEquals(200, r.status());
        assertEquals(valid, r.bool("valid"), "body: " + r.body());
    }

    @ParameterizedTest(name = "{0} ({1}) -> valid={2}")
    @CsvSource({
            "2026-11-22, Sunday, true", "2026-11-24, Tuesday, true", "2026-11-26, Thursday, true",
            "2026-11-27, Friday, false", "2026-11-28, Saturday, false"
    })
    @DisplayName("TC-INT-003 [INT-02] interviews take place Sunday to Thursday")
    void weekdays(String day, String name, boolean valid) {
        EngineApi.Response r = validate(at(day, "10:00"), at(day, "11:00"), "", "");
        assertEquals(200, r.status());
        assertEquals(valid, r.bool("valid"), "body: " + r.body());
    }

    @Test
    @DisplayName("TC-INT-004 [INT-03] back-to-back with the recruiter's previous interview is allowed")
    void backToBackRecruiter() {
        EngineApi.Response r = validate(at(DAY, "11:00"), at(DAY, "12:00"),
                slot(at(DAY, "10:00"), at(DAY, "11:00")), "");
        assertEquals(200, r.status());
        assertEquals(true, r.bool("valid"), "body: " + r.body());
    }

    @Test
    @DisplayName("TC-INT-004b [INT-03] back-to-back with the student's next interview is allowed")
    void backToBackStudent() {
        EngineApi.Response r = validate(at(DAY, "11:00"), at(DAY, "12:00"),
                "", slot(at(DAY, "12:00"), at(DAY, "13:00")));
        assertEquals(200, r.status());
        assertEquals(true, r.bool("valid"), "body: " + r.body());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "1-minute overlap with recruiter | 10:59 | 12:00 | recruiter",
            "contained in student slot      | 10:15 | 10:45 | student"
    })
    @DisplayName("TC-INT-005 [INT-03] overlapping the recruiter's or the student's interview is refused")
    void overlapRefused(String name, String start, String end, String who) {
        String existing = slot(at(DAY, "10:00"), at(DAY, "11:00"));
        EngineApi.Response r = validate(at(DAY, start), at(DAY, end),
                who.equals("recruiter") ? existing : "", who.equals("student") ? existing : "");
        assertEquals(200, r.status());
        assertEquals(false, r.bool("valid"), "body: " + r.body());
    }

    @Test
    @DisplayName("TC-INT-006 [INT-02] a date-time without offset is read as UTC (05:00Z = 08:00 Doha)")
    void noOffsetIsUtc() {
        EngineApi.Response valid = validate("2026-11-22T05:00:00", "2026-11-22T06:00:00", "", "");
        EngineApi.Response early = validate("2026-11-22T04:30:00", "2026-11-22T05:30:00", "", "");
        assertEquals(true, valid.bool("valid"), "body: " + valid.body());
        assertEquals(false, early.bool("valid"), "body: " + early.body());
    }
}
