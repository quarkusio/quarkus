package io.quarkus.it.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Verifies that cleanup happened correctly after AFirstTest failed to start.
 * This test uses the default profile and should see app.limit=100 from application.properties.
 * <p>
 * If cleanup did NOT happen, this test would fail with: expected: &lt;100&gt; but was: &lt;3&gt;
 * (because AFirstTest's profile set app.limit=3 as a system property that would leak).
 */
@QuarkusTest
@Order(2)
class BSecondTest {

    @Inject
    Limits limits;

    @Test
    void seesTheConfiguredLimit() {
        assertEquals(100, limits.limit(), "app.limit from application.properties should not leak from failed profile");
    }
}
