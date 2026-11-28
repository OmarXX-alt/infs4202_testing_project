package com.halcyon.uniplace;

/**
 * JUnit tag for team tests that assert a requirement the delivered system violates. Each tagged test names its
 * GitHub issue. They are excluded from the default {@code ./mvnw test} run (property {@code excludedGroups} in
 * pom.xml) so that CI fails only on unexpected breakage; run them with
 * {@code ./mvnw test -Dgroups=known-defect -DexcludedGroups=none}. When a defect is fixed, its test passes and the
 * tag is removed.
 */
public final class KnownDefect {

    public static final String TAG = "known-defect";

    private KnownDefect() {
    }
}
