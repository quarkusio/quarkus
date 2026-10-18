package io.quarkus.vertx.http.runtime.filters;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class GracefulShutdownFilterUnitTest {

    @Test
    public void testUserAgentForJDK8335181IsAffected() {
        assertTrue(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/21.0.7"));
        assertTrue(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/17.0.16"));
    }

    @Test
    public void testUserAgentForJDK8335181IsNotAffected() {
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/21.0.8"));
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/17.0.17"));
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/25.0.0"));
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181(""));
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181("Mozilla"));
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181(null));
    }

    @Test
    public void testUserAgentForJDK8335181NotSure() {
        // Assume true if the patch version can't be parsed.f
        assertTrue(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/21.0.8-something"));
        // Assume false if the major version indicates it is not a problem.
        assertFalse(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/25.0.0-something"));
        // Assume true when it is a Java client, but we can't parse the version that should be separated by dots.
        assertTrue(GracefulShutdownFilter.isAffectedByJDK8335181("Java-http-client/17-0-17-oops"));
    }

}
