package uniplace.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * POST /match. Oracle: openapi.yaml: score = 70 x matchedRequired/required + 30 x matchedNice/nice (0 when
 * there is no nice-to-have list), rounded half up; skills compared ignoring case and surrounding spaces.
 * Used by APP-04 (score stored with the application) and APP-07 (shortlist threshold).
 */
@DisplayName("Engine /match")
class MatchContractTest {

    static String request(String student, String required, String nice) {
        return "{\"studentSkills\":[" + student + "],\"requiredSkills\":[" + required + "],\"niceToHaveSkills\":["
                + nice + "]}";
    }

    @ParameterizedTest(name = "{0} -> {4}")
    @CsvSource(delimiter = '|', value = {
            // case | student | required | nice | expected score
            "all required, all nice     | \"a\",\"x\"       | \"a\"         | \"x\"                 | 100",
            "2/3 required, 1/2 nice     | \"a\",\"b\",\"x\" | \"a\",\"b\",\"c\" | \"x\",\"y\"       | 62",
            "half rounds up: 35 + 7.5   | \"a\",\"x\"       | \"a\",\"b\"   | \"x\",\"y\",\"z\",\"w\" | 43",
            "no skills at all           |                   | \"a\"         | \"x\"                 | 0"
    })
    @DisplayName("TC-APP-010 [APP-04] match score follows the contract formula")
    void formula(String name, String student, String required, String nice, int expected) {
        EngineApi.Response r = EngineApi.post("/match",
                request(student == null ? "" : student, required, nice));
        assertEquals(200, r.status());
        assertEquals(expected, r.integer("score"), "body: " + r.body());
    }

    @ParameterizedTest(name = "{0} -> {3}")
    @CsvSource(delimiter = '|', value = {
            "1/1 required, empty nice list | \"a\"       | \"a\"       | 70",
            "1/2 required, empty nice list | \"a\"       | \"a\",\"b\" | 35",
            "0/1 required, empty nice list | \"z\"       | \"a\"       | 0"
    })
    @DisplayName("TC-APP-011 [APP-04] with no nice-to-have skills the second term is 0 (max score 70)")
    void emptyNiceToHaveList(String name, String student, String required, int expected) {
        EngineApi.Response r = EngineApi.post("/match", request(student, required, ""));
        assertEquals(200, r.status());
        assertEquals(expected, r.integer("score"), "body: " + r.body());
    }

    @Test
    @DisplayName("TC-APP-011b [APP-04] an omitted nice-to-have list behaves like an empty one")
    void omittedNiceToHaveList() {
        EngineApi.Response r = EngineApi.post("/match", "{\"studentSkills\":[\"a\"],\"requiredSkills\":[\"a\"]}");
        assertEquals(200, r.status());
        assertEquals(70, r.integer("score"), "body: " + r.body());
    }

    @ParameterizedTest(name = "student skill \"{0}\" matches required \"java\"")
    @CsvSource(delimiter = '|', value = {"JAVA", "Java", "' java'", "'java '", "'  Java  '"})
    @DisplayName("TC-APP-012 [APP-04] skills match ignoring letter case and surrounding spaces")
    void caseAndSpaceInsensitive(String skill) {
        EngineApi.Response r = EngineApi.post("/match", request("\"" + skill + "\"", "\"java\"", "\"x\""));
        assertEquals(200, r.status());
        assertEquals(70, r.integer("score"), "body: " + r.body());
    }

    @Test
    @DisplayName("TC-APP-010b [REL-02] an empty required list is a validation error naming requiredSkills")
    void emptyRequiredList() {
        EngineApi.Response r = EngineApi.post("/match", request("\"a\"", "", ""));
        assertEquals(400, r.status());
        assertEquals("requiredSkills", r.body().path("field").asText());
    }
}
