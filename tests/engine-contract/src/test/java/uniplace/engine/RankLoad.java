package uniplace.engine;

import java.util.Random;

/** Builds valid, reproducible /rank requests of a given size (fixed random seed). */
final class RankLoad {

    private RankLoad() {
    }

    static String request(int candidates, int seats) {
        Random random = new Random(42);
        StringBuilder sb = new StringBuilder("{\"seats\":").append(seats).append(",\"candidates\":[");
        for (int i = 0; i < candidates; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(RankContractTest.candidate(i + 1, random.nextInt(101),
                    String.format(java.util.Locale.ROOT, "%.2f", 2 + random.nextDouble() * 2),
                    String.format("2026-11-%02dT10:00:00+03:00", 1 + i % 28)));
        }
        return sb.append("]}").toString();
    }
}
