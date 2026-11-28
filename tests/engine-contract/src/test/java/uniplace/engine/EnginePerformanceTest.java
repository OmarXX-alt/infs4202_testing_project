package uniplace.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PERF-02: eligibility and match answer within 200 ms, and ranking 200 candidates within 500 ms, at the
 * 95th percentile. Each request is sent 5 times to warm up, then measured 20 times.
 */
@DisplayName("Engine performance (PERF-02)")
class EnginePerformanceTest {

    static final int WARM_UP = 5;
    static final int RUNS = 20;

    static long p95(Supplier<EngineApi.Response> call) {
        for (int i = 0; i < WARM_UP; i++) {
            assertEquals(200, call.get().status());
        }
        List<Long> times = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            EngineApi.Response r = call.get();
            assertEquals(200, r.status());
            times.add(r.millis());
        }
        Collections.sort(times);
        return times.get((int) Math.ceil(0.95 * RUNS) - 1);
    }

    @Test
    @DisplayName("TC-APP-017 [PERF-02] eligibility p95 <= 200 ms")
    void eligibilityLatency() {
        String body = EligibilityContractTest.validStudent("3.00", "2.00");
        long p95 = p95(() -> EngineApi.post("/eligibility", body));
        assertTrue(p95 <= 200, "p95 = " + p95 + " ms");
    }

    @Test
    @DisplayName("TC-APP-017b [PERF-02] match p95 <= 200 ms")
    void matchLatency() {
        String body = MatchContractTest.request("\"a\",\"b\"", "\"a\",\"c\"", "\"b\"");
        long p95 = p95(() -> EngineApi.post("/match", body));
        assertTrue(p95 <= 200, "p95 = " + p95 + " ms");
    }

    @Test
    @DisplayName("TC-APP-017c [PERF-02] ranking 200 candidates p95 <= 500 ms")
    void rank200Latency() {
        String body = RankLoad.request(200, 5);
        long p95 = p95(() -> EngineApi.post("/rank", body));
        assertTrue(p95 <= 500, "p95 = " + p95 + " ms for 200 candidates");
    }
}
