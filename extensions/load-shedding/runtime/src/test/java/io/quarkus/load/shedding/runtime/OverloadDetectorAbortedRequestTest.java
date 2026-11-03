package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

public class OverloadDetectorAbortedRequestTest {
    private static final int LIMIT = 10;

    private final OverloadDetector detector = new OverloadDetector(new TestConfig.Builder()
            .withMinLimit(1)
            .withInitialLimit(LIMIT)
            .withProbeFactor(Double.MAX_VALUE)
            .build());

    @Test
    public void abortedRequestIsNotASample() throws Exception {
        detector.requestBegin();
        detector.requestEnd(1_000);
        long lowestBefore = lowestRequestTime();
        assertThat(lowestBefore).isEqualTo(1_000);

        detector.requestBegin();
        detector.requestAborted();

        assertThat(lowestRequestTime()).isEqualTo(lowestBefore);
        assertThat(detector.currentLimit()).isEqualTo(LIMIT);
        assertThat(detector.currentRequests()).isZero();
    }

    @Test
    public void abortedRequestLeavesTheOverloadedState() {
        for (int i = 0; i < LIMIT; i++) {
            detector.requestBegin();
        }
        assertThat(detector.isOverloaded()).isTrue();

        for (int i = 0; i < LIMIT; i++) {
            detector.requestAborted();
        }

        assertThat(detector.currentRequests()).isZero();
        assertThat(detector.isOverloaded()).isFalse();
    }

    private long lowestRequestTime() throws Exception {
        Field field = OverloadDetector.class.getDeclaredField("lowestRequestTime");
        field.setAccessible(true);
        return ((AtomicLong) field.get(detector)).get();
    }
}
