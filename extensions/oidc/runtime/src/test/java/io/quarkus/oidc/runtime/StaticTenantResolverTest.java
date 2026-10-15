package io.quarkus.oidc.runtime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StaticTenantResolverTest {

    @Test
    public void firstAttemptAlwaysAllowed() {
        assertTrue(StaticTenantResolver.shouldRetryInitialization(0, 1_000, 0));
        assertTrue(StaticTenantResolver.shouldRetryInitialization(0, 1_000, 30_000));
    }

    @Test
    public void zeroIntervalKeepsSingleAttemptBehavior() {
        assertTrue(StaticTenantResolver.shouldRetryInitialization(0, 5_000, 0));
        assertFalse(StaticTenantResolver.shouldRetryInitialization(1_000, 5_000, 0));
        assertFalse(StaticTenantResolver.shouldRetryInitialization(1_000, Long.MAX_VALUE, 0));
    }

    @Test
    public void positiveIntervalThrottlesRetries() {
        long interval = 30_000;
        long last = 100_000;
        assertFalse(StaticTenantResolver.shouldRetryInitialization(last, last + 1, interval));
        assertFalse(StaticTenantResolver.shouldRetryInitialization(last, last + interval - 1, interval));
        assertTrue(StaticTenantResolver.shouldRetryInitialization(last, last + interval, interval));
        assertTrue(StaticTenantResolver.shouldRetryInitialization(last, last + interval + 5_000, interval));
    }
}
