package io.quarkus.smallrye.faulttolerance.test.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.faulttolerance.api.RateLimitException;

public class RateLimitTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(RateLimitBean.class));

    @Inject
    RateLimitBean rateLimit;

    @Test
    public void test() {
        assertEquals(1, rateLimit.hello());
        assertEquals(2, rateLimit.hello());
        assertEquals(3, rateLimit.hello());
        assertEquals(4, rateLimit.hello());
        assertEquals(5, rateLimit.hello());
        assertThrows(RateLimitException.class, () -> rateLimit.hello());
    }
}
