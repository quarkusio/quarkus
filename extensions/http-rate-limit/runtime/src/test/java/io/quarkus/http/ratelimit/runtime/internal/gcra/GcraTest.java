package io.quarkus.http.ratelimit.runtime.internal.gcra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GcraTest {

    static final long T = 12_000_000_000L; // 5 permits per minute
    static final long TAU = 5 * T;

    @Test
    void freshBucketAdmitsBurstThenRejectsWithExactWait() {
        long tat = 0, now = 1_000_000L;
        for (int i = 0; i < 5; i++) {
            var o = Gcra.tryConsume(tat, now, 1, T, TAU);
            assertThat(o.allowed()).isTrue();
            assertThat(o.remaining()).isEqualTo(4 - i);
            tat = o.newTat();
        }
        var sixth = Gcra.tryConsume(tat, now, 1, T, TAU);
        assertThat(sixth.allowed()).isFalse();
        assertThat(sixth.retryAfterNanos()).isEqualTo(T);
        assertThat(sixth.newTat()).isEqualTo(tat);
    }

    @Test
    void rejectionDoesNotConsume() {
        long now = 1_000;
        long tat = exhaust(now);
        var first = Gcra.tryConsume(tat, now, 1, T, TAU);
        var second = Gcra.tryConsume(first.newTat(), now, 1, T, TAU);
        assertThat(first.allowed()).isFalse();
        assertThat(second.allowed()).isFalse();
        assertThat(second.retryAfterNanos()).isEqualTo(first.retryAfterNanos());
        assertThat(second.newTat()).isEqualTo(first.newTat()).isEqualTo(tat);
    }

    @Test
    void onePermitRefillsAfterOneEmissionInterval() {
        long now = 1_000;
        long tat = exhaust(now);
        var o = Gcra.tryConsume(tat, now + T, 1, T, TAU);
        assertThat(o.allowed()).isTrue();
        assertThat(o.remaining()).isEqualTo(0);
    }

    @Test
    void pastTatIsIndistinguishableFromNoState() {
        assertThat(Gcra.tryConsume(500, 1_000, 1, T, TAU)).isEqualTo(Gcra.tryConsume(Gcra.NO_STATE, 1_000, 1, T, TAU));
    }

    @Test
    void negativeClockOriginHasNoSpuriousDebt() {
        var o = Gcra.tryConsume(Gcra.NO_STATE, -5_000_000_000L, 1, T, TAU);
        assertThat(o.allowed()).isTrue();
        assertThat(o.remaining()).isEqualTo(4);
    }

    @Test
    void staleTatNeverReportsMoreThanBurst() {
        assertThat(Gcra.tryConsume(1_000, 1_000 + 100 * T, 0, T, TAU).remaining()).isEqualTo(5);
    }

    @Test
    void costClaimsSeveralPermitsAtOnce() {
        var o = Gcra.tryConsume(Gcra.NO_STATE, 1_000, 4, T, TAU);
        assertThat(o.allowed()).isTrue();
        assertThat(o.remaining()).isEqualTo(1);
        var r = Gcra.tryConsume(o.newTat(), 1_000, 2, T, TAU);
        assertThat(r.allowed()).isFalse();
        assertThat(r.retryAfterNanos()).isEqualTo(T); // wait until 2 permits exist
    }

    @Test
    void probeCostZeroEvaluatesWithoutConsuming() {
        var o = Gcra.tryConsume(Gcra.NO_STATE, 1_000, 0, T, TAU);
        assertThat(o.allowed()).isTrue();
        assertThat(o.newTat()).isEqualTo(1_000);
        assertThat(o.remaining()).isEqualTo(5);
    }

    @Test
    void nearWrapClockOriginIsHandled() {
        long now = Long.MAX_VALUE - T / 2;
        var o = Gcra.tryConsume(Gcra.NO_STATE, now, 1, T, TAU);
        assertThat(o.allowed()).isTrue();
        long later = now + T; // wraps to negative
        var o2 = Gcra.tryConsume(o.newTat(), later, 1, T, TAU);
        assertThat(o2.allowed()).isTrue();
        // one permit regenerated across the wrap: elapsed-time arithmetic, never absolute comparisons
        assertThat(o2.remaining()).isEqualTo(4);
    }

    @Test
    void probeOfExhaustedBucketIsRejected() {
        long tat = exhaust(1_000);
        var p = Gcra.tryConsume(tat, 1_000, 0, T, TAU);
        assertThat(p.allowed()).isFalse();
        assertThat(p.newTat()).isEqualTo(tat);
    }

    @Test
    void tatNeverExceedsNowPlusTau() {
        long tat = Gcra.NO_STATE;
        for (int i = 0; i < 1_000; i++) {
            var o = Gcra.tryConsume(tat, 1_000, 1, T, TAU);
            if (o.allowed()) {
                tat = o.newTat();
            }
            assertThat(tat).isLessThanOrEqualTo(1_000 + TAU);
        }
    }

    private static long exhaust(long now) {
        long tat = Gcra.NO_STATE;
        for (int i = 0; i < 5; i++) {
            tat = Gcra.tryConsume(tat, now, 1, T, TAU).newTat();
        }
        return tat;
    }

    /**
     * A debt above the burst tolerance cannot come from this policy: it was written under a larger one, before an
     * override or a configuration change, or by a clock that jumped. It is capped at the tolerance, so that it costs
     * the key at most one emission interval instead of locking it out for the leftover debt, up to 30 days.
     */
    @Test
    void debtAboveTheBurstToleranceIsCapped() {
        long now = 1_000;
        long thirtyDaysAhead = now + 30L * 24 * 3600 * 1_000_000_000L;
        var o = Gcra.tryConsume(thirtyDaysAhead, now, 1, T, TAU);
        assertThat(o.allowed()).isFalse();
        assertThat(o.retryAfterNanos()).isEqualTo(T);
        assertThat(o.remaining()).isZero();
        // one emission interval later, a permit is available
        assertThat(Gcra.tryConsume(o.newTat(), now + T, 1, T, TAU).allowed()).isTrue();
    }
}
