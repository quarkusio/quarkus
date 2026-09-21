package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.load.shedding.RequestClassifier;
import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;

/**
 * Tests how the two halves of the algorithm compose: the overload depth that
 * {@link OverloadDetector#overloadDepth()} reports, and the shedding decision that
 * {@link PriorityLoadShedding#shedLoad(Object, double)} derives from it.
 * <p>
 * The two combine into a single smooth, monotonic response. {@code OverloadDetector} maps
 * the number of concurrent requests to a depth in {@code [0.0, 1.0]} using the right half
 * of a sigmoid, where the depth only reaches 1.0 once the concurrency has climbed by
 * {@code max(maxLimit - limit, 0.3 * maxLimit)} past the current limit.
 * {@code PriorityLoadShedding} then turns that depth into a threshold of
 * {@code 640 * (1 - depth^3)} (640 = 5 priorities * 128 cohorts) and sheds any request whose
 * score of {@code priorityBaseline + cohort} reaches it. Because depth is flat near 0,
 * a slight excess over the limit only sheds the worst cohorts of the least important priorities,
 * and the most important requests are only shed once the depth approaches 1.0.
 * <p>
 * Note that the limit in {@link OverloadDetector} never adapts here, because requests
 * only ever begin and never end.
 */
public class OverloadDepthAndPriorityLoadSheddingTest {
    // same as default production setting
    private static final int MAX_LIMIT = 1_000;

    // the request is only ever passed to the prioritizer and the classifier,
    // both of which are fixed here, so the value is irrelevant
    private static final Object REQUEST = new Object();

    // enough concurrency to reach full saturation (depth 1.0) for any limit up to `MAX_LIMIT`
    // the worst case is the limit sitting at the max limit, where depth reaches 1.0 only 30% past it
    private static final int SATURATION_BOUND = 13 * MAX_LIMIT / 10;

    @Test
    public void nothingIsShedAtExactlyTheLimit() {
        for (int limit : new int[] { 1, 100, MAX_LIMIT / 2, MAX_LIMIT }) {
            for (RequestPriority priority : RequestPriority.levels()) {
                assertThat(shedsAt(limit, limit, priority, RequestClassifier.maxCohort())).isFalse();
                assertThat(shedsAt(limit, limit, priority, RequestClassifier.minCohort())).isFalse();
            }
        }
    }

    @Test
    public void moreImportantPrioritiesAreShedLaterThanLessImportantOnes() {
        for (int cohort : new int[] { RequestClassifier.minCohort(), RequestClassifier.maxCohort() }) {
            int previous = -1;
            for (RequestPriority priority : RequestPriority.levels().reversed()) {
                int firstShed = lowestConcurrencyThatSheds(MAX_LIMIT / 2, priority, cohort);
                if (previous != -1) {
                    assertThat(firstShed).isGreaterThan(previous);
                }
                previous = firstShed;
            }
        }
    }

    @Test
    public void worstCohortsAreShedEarlierThanBestOnes() {
        for (RequestPriority priority : RequestPriority.levels()) {
            int worst = lowestConcurrencyThatSheds(MAX_LIMIT / 2, priority, RequestClassifier.maxCohort());
            int best = lowestConcurrencyThatSheds(MAX_LIMIT / 2, priority, RequestClassifier.minCohort());
            assertThat(worst).isLessThan(best);
        }
    }

    @Test
    public void sameExcessMeansDeeperOverloadWhenThereIsLessHeadroom() {
        int excess = 150;
        RequestPriority priority = RequestPriority.DEGRADED;
        int cohort = RequestClassifier.minCohort();

        // limit 100 leaves 900 of headroom, so 150 requests past the limit barely registers
        assertThat(shedsAt(100, 100 + excess, priority, cohort)).isFalse();

        // limit 500 leaves only 500 of headroom, so the same excess sheds this very same group
        assertThat(shedsAt(MAX_LIMIT / 2, MAX_LIMIT / 2 + excess, priority, cohort)).isTrue();
    }

    @Test
    public void fullSaturationShedsEvenTheMostImportantRequest() {
        assertThat(depthAt(MAX_LIMIT, SATURATION_BOUND - 1)).isCloseTo(1.0, within(0.01));
        assertThat(shedsAt(MAX_LIMIT, SATURATION_BOUND - 1, RequestPriority.CRITICAL, RequestClassifier.minCohort())).isFalse();

        assertThat(depthAt(MAX_LIMIT, SATURATION_BOUND)).isEqualTo(1.0);
        assertThat(shedsAt(MAX_LIMIT, SATURATION_BOUND, RequestPriority.CRITICAL, RequestClassifier.minCohort())).isTrue();
    }

    @Test
    public void neverIsNeverShedEvenAtFullSaturation() {
        for (int cohort = RequestClassifier.minCohort(); cohort <= RequestClassifier.maxCohort(); cohort++) {
            assertThat(shedsAt(MAX_LIMIT, SATURATION_BOUND, RequestPriority.NEVER, cohort)).isFalse();
        }
    }

    @Test
    public void overloadDepthIsContinuousAcrossTheHeadroomSwitch() {
        // `headroom` is `max(maxLimit - limit, 0.3 * maxLimit)`, so the two operands swap
        // which one wins at `limit == 0.7 * maxLimit`
        int switchpoint = MAX_LIMIT * 7 / 10;

        OverloadDetector beforeSwitch = detector(switchpoint - 1);
        begin(beforeSwitch, switchpoint - 1 + 300);

        OverloadDetector atSwitch = detector(switchpoint);
        begin(atSwitch, switchpoint + 300);

        OverloadDetector afterSwitch = detector(switchpoint + 1);
        begin(afterSwitch, switchpoint + 1 + 300);

        assertThat(beforeSwitch.overloadDepth()).isCloseTo(atSwitch.overloadDepth(), within(0.01));
        assertThat(atSwitch.overloadDepth()).isCloseTo(afterSwitch.overloadDepth(), within(0.01));
    }

    @Test
    public void overloadDepthIsContinuousAcrossTheLimitBoundary() {
        OverloadDetector belowMax = detector(MAX_LIMIT - 1);
        begin(belowMax, MAX_LIMIT);

        OverloadDetector atMax = detector(MAX_LIMIT);
        begin(atMax, MAX_LIMIT + 1);

        assertThat(belowMax.overloadDepth()).isCloseTo(atMax.overloadDepth(), within(0.01));
    }

    @Test
    public void responseIsMonotonicInOverload() {
        assertMonotonicResponse(100);
        assertMonotonicResponse(MAX_LIMIT / 2);
        assertMonotonicResponse(MAX_LIMIT);
    }

    private void assertMonotonicResponse(int limit) {
        // using `values()` here because `ordinal()` is used as an index
        boolean[] shed = new boolean[RequestPriority.values().length];
        OverloadDetector detector = detector(limit);
        for (int concurrency = 1; concurrency <= SATURATION_BOUND; concurrency++) {
            detector.requestBegin();
            for (RequestPriority priority : RequestPriority.levels()) {
                boolean now = sheds(detector, priority, RequestClassifier.maxCohort());
                if (shed[priority.ordinal()]) {
                    assertThat(now)
                            .as("%s is not accepted again at concurrency %s", priority, concurrency)
                            .isTrue();
                }
                shed[priority.ordinal()] = now;
            }
        }
    }

    private double depthAt(int currentLimit, int currentRequests) {
        OverloadDetector detector = detector(currentLimit);
        begin(detector, currentRequests);
        return detector.overloadDepth();
    }

    private boolean shedsAt(int currentLimit, int currentRequests, RequestPriority priority, int cohort) {
        OverloadDetector detector = detector(currentLimit);
        begin(detector, currentRequests);
        return sheds(detector, priority, cohort);
    }

    private boolean sheds(OverloadDetector detector, RequestPriority priority, int cohort) {
        PriorityLoadShedding shedding = new PriorityLoadShedding(new TestConfig.Builder().build());
        shedding.requestPrioritizers = List.of(new FixedPrioritizer(priority));
        shedding.requestClassifiers = List.of(new FixedClassifier(cohort));
        return shedding.shedLoad(REQUEST, detector.overloadDepth());
    }

    private int lowestConcurrencyThatSheds(int currentLimit, RequestPriority priority, int cohort) {
        OverloadDetector detector = detector(currentLimit);
        for (int concurrency = 1; concurrency <= SATURATION_BOUND; concurrency++) {
            detector.requestBegin();
            if (sheds(detector, priority, cohort)) {
                return concurrency;
            }
        }
        throw new AssertionError("never shed: " + priority + " in cohort " + cohort);
    }

    private OverloadDetector detector(int initialLimit) {
        return new OverloadDetector(new TestConfig.Builder()
                .withInitialLimit(initialLimit)
                .withMaxLimit(MAX_LIMIT)
                .build());
    }

    private void begin(OverloadDetector detector, int count) {
        for (int i = 0; i < count; i++) {
            detector.requestBegin();
        }
    }

    private static final class FixedPrioritizer implements RequestPrioritizer<Object> {
        private final RequestPriority priority;

        private FixedPrioritizer(RequestPriority priority) {
            this.priority = priority;
        }

        @Override
        public boolean appliesTo(Object request) {
            return true;
        }

        @Override
        public RequestPriority priority(Object request) {
            return priority;
        }
    }

    private static final class FixedClassifier implements RequestClassifier<Object> {
        private final int cohort;

        private FixedClassifier(int cohort) {
            this.cohort = cohort;
        }

        @Override
        public boolean appliesTo(Object request) {
            return true;
        }

        @Override
        public int cohort(Object request) {
            return cohort;
        }
    }
}
