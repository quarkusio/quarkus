package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import io.quarkus.load.shedding.RequestClassifier;
import io.quarkus.load.shedding.RequestPrioritizer;
import io.quarkus.load.shedding.RequestPriority;

public class PriorityLoadSheddingTest {
    // the request is only passed to the prioritizer and the classifier, both of which
    // are fixed in this test, so the value is irrelevant
    private static final Object REQUEST = new Object();

    // the overload factor is probed in steps of 1/STEPS
    private static final int STEPS = 100;

    @Test
    public void disabledShedsEverything() {
        for (RequestPriority priority : RequestPriority.levels()) {
            PriorityLoadShedding shedding = priorityLoadShedding(false, priority, RequestClassifier.minCohort());
            assertThat(shedding.shedLoad(REQUEST, 0.0)).isTrue();
            assertThat(shedding.shedLoad(REQUEST, 1.0)).isTrue();

            shedding = priorityLoadShedding(false, priority, RequestClassifier.maxCohort());
            assertThat(shedding.shedLoad(REQUEST, 0.0)).isTrue();
            assertThat(shedding.shedLoad(REQUEST, 1.0)).isTrue();

        }
    }

    @Test
    public void noOverloadShedsNothing() {
        // the request that would be shed first
        PriorityLoadShedding shedding = priorityLoadShedding(true, RequestPriority.DEGRADED, RequestClassifier.maxCohort());

        assertThat(shedding.shedLoad(REQUEST, 0.0)).isFalse();
    }

    @Test
    public void fullOverloadShedsEverything() {
        for (RequestPriority priority : RequestPriority.levels()) {
            assertThat(priorityLoadShedding(true, priority, RequestClassifier.minCohort()).shedLoad(REQUEST, 1.0)).isTrue();
            assertThat(priorityLoadShedding(true, priority, RequestClassifier.maxCohort()).shedLoad(REQUEST, 1.0)).isTrue();
        }
    }

    @Test
    public void higherPriorityIsShedLater() {
        int cohort = RequestClassifier.minCohort();

        double overload = previousStep(lowestOverloadThatSheds(RequestPriority.BACKGROUND, RequestClassifier.minCohort()));

        for (RequestPriority priority : RequestPriority.levels()) {
            boolean shed = priority.ordinal() == RequestPriority.DEGRADED.ordinal();
            assertThat(priorityLoadShedding(true, priority, cohort).shedLoad(REQUEST, overload)).isEqualTo(shed);
        }

        overload = previousStep(lowestOverloadThatSheds(RequestPriority.IMPORTANT, RequestClassifier.minCohort()));

        for (RequestPriority priority : RequestPriority.levels()) {
            boolean shed = priority.ordinal() >= RequestPriority.NORMAL.ordinal();
            assertThat(priorityLoadShedding(true, priority, cohort).shedLoad(REQUEST, overload)).isEqualTo(shed);
        }
    }

    @Test
    public void higherCohortIsShedEarlier() {
        RequestPriority priority = RequestPriority.NORMAL;

        double overload = lowestOverloadThatSheds(priority, RequestClassifier.maxCohort());
        assertThat(priorityLoadShedding(true, priority, RequestClassifier.maxCohort()).shedLoad(REQUEST, overload)).isTrue();
        assertThat(priorityLoadShedding(true, priority, RequestClassifier.minCohort()).shedLoad(REQUEST, overload)).isFalse();
    }

    @Test
    public void deeperOverloadShedsMore() {
        // once a request group is shed, growing the overload never admits it again
        for (RequestPriority priority : RequestPriority.levels()) {
            IntStream.rangeClosed(RequestClassifier.minCohort(), RequestClassifier.maxCohort()).forEach(cohort -> {
                double shedFrom = lowestOverloadThatSheds(priority, cohort);

                for (int step = (int) Math.round(shedFrom * STEPS); step <= STEPS; step++) {
                    double overload = (double) step / STEPS;
                    assertThat(priorityLoadShedding(true, priority, cohort).shedLoad(REQUEST, overload))
                            .describedAs("%s at overload %s", priority, overload)
                            .isTrue();
                }
            });
        }
    }

    @Test
    public void neverIsNeverShed() {
        for (int cohort = 0; cohort < RequestClassifier.numCohorts(); cohort++) {
            PriorityLoadShedding shedding = priorityLoadShedding(true, RequestPriority.NEVER, cohort);
            for (int step = 0; step <= STEPS; step++) {
                double overload = (double) step / STEPS;
                assertThat(shedding.shedLoad(REQUEST, overload))
                        .describedAs("cohort %s at overload %s", cohort, overload)
                        .isFalse();
            }
        }
    }

    @Test
    public void outOfRangeCohortIsNormalizedByBitMasking() {
        double overload = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.maxCohort());

        // these mask down to the highest cohort, which is shed at this overload
        for (int cohort : new int[] { -1, 2 * RequestClassifier.maxCohort() + 1 }) {
            assertThat(priorityLoadShedding(true, RequestPriority.NORMAL, cohort).shedLoad(REQUEST, overload)).isTrue();
        }

        // these mask down to the lowest cohort, which is NOT shed at this overload
        for (int cohort : new int[] { RequestClassifier.numCohorts(), 2 * RequestClassifier.numCohorts() }) {
            assertThat(priorityLoadShedding(true, RequestPriority.NORMAL, cohort).shedLoad(REQUEST, overload)).isFalse();
        }
    }

    @Test
    public void missingPrioritizerAndClassifierUseDefaults() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, null);

        // the defaults are NORMAL priority in the middle cohort
        double overload = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.numCohorts() / 2);

        assertThat(shedding.shedLoad(REQUEST, overload)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(overload))).isFalse();
    }

    @Test
    public void firstApplicableClassifierWins() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, RequestPriority.DEGRADED, null);
        shedding.requestClassifiers = List.of(
                new FixedClassifier(false, RequestClassifier.minCohort()),
                new FixedClassifier(true, RequestClassifier.numCohorts() / 2));

        double overload = lowestOverloadThatSheds(RequestPriority.DEGRADED, RequestClassifier.numCohorts() / 2);
        assertThat(shedding.shedLoad(REQUEST, overload)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(overload))).isFalse();
    }

    @Test
    public void firstApplicablePrioritizerWins() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, RequestClassifier.minCohort());
        shedding.requestPrioritizers = List.of(
                new FixedPrioritizer(false, RequestPriority.CRITICAL),
                new FixedPrioritizer(true, RequestPriority.DEGRADED));

        assertThat(shedding.shedLoad(REQUEST, 0.7)).isTrue();
    }

    // A prioritizer and a classifier are user code, called on the first handler in the chain
    // while the application is overloaded. Letting an exception out of there would turn
    // requests that load shedding may well have admitted into 500s, at the worst possible
    // moment, so a failing implementation is treated as not applying to the request.

    @Test
    public void failingPrioritizerFallsBackToNormal() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, RequestClassifier.minCohort());
        shedding.requestPrioritizers = List.of(new FailingPrioritizer());

        double normal = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.minCohort());
        assertThat(shedding.shedLoad(REQUEST, normal)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(normal))).isFalse();
    }

    @Test
    public void failingClassifierFallsBackToTheMiddleCohort() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, RequestPriority.NORMAL, null);
        shedding.requestClassifiers = List.of(new FailingClassifier());

        double middle = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.numCohorts() / 2);
        assertThat(shedding.shedLoad(REQUEST, middle)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(middle))).isFalse();
    }

    // The interface takes an `Object` so that a single list can hold prioritizers for
    // different request types, which makes an `appliesTo()` that is more permissive than the
    // type parameter an easy mistake. The bridge method then throws before the implementation
    // is even entered.
    @Test
    public void prioritizerThatDoesNotMatchItsTypeParameterFallsBack() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, RequestClassifier.minCohort());
        shedding.requestPrioritizers = List.of(new StringOnlyPrioritizer());

        double normal = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.minCohort());
        assertThat(shedding.shedLoad(REQUEST, normal)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(normal))).isFalse();

        // and it still works for the request type it was written for
        assertThat(shedding.shedLoad("a string", normal)).isFalse();
    }

    @Test
    public void prioritizerReturningNullFallsBackToNormal() {
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, RequestClassifier.minCohort());
        shedding.requestPrioritizers = List.of(new FixedPrioritizer(true, null));

        double normal = lowestOverloadThatSheds(RequestPriority.NORMAL, RequestClassifier.minCohort());
        assertThat(shedding.shedLoad(REQUEST, normal)).isTrue();
        assertThat(shedding.shedLoad(REQUEST, previousStep(normal))).isFalse();
    }

    @Test
    public void failingImplementationDoesNotHideTheNextOne() {
        // a broken implementation must not take the ones behind it down
        PriorityLoadShedding shedding = priorityLoadShedding(true, null, RequestClassifier.maxCohort());
        shedding.requestPrioritizers = List.of(
                new FailingPrioritizer(),
                new FixedPrioritizer(true, RequestPriority.IMPORTANT));

        assertThat(shedding.shedLoad(REQUEST, 0.5)).isFalse();
    }

    // the lowest probed overload at which the given request group is shed
    private double lowestOverloadThatSheds(RequestPriority priority, int cohort) {
        PriorityLoadShedding shedding = priorityLoadShedding(true, priority, cohort);
        for (int step = 0; step <= STEPS; step++) {
            double overload = (double) step / STEPS;
            if (shedding.shedLoad(REQUEST, overload)) {
                return overload;
            }
        }
        throw new AssertionError("never shed: " + priority + " in cohort " + cohort);
    }

    private double previousStep(double overload) {
        return Math.max(0.0, overload - 1.0 / STEPS);
    }

    private PriorityLoadShedding priorityLoadShedding(boolean enabled, RequestPriority priority, Integer cohort) {
        PriorityLoadShedding result = new PriorityLoadShedding(
                new TestConfig.Builder().withPriorityEnabled(enabled).build());
        result.requestPrioritizers = priority == null
                ? List.of()
                : List.of(new FixedPrioritizer(true, priority));
        result.requestClassifiers = cohort == null
                ? List.of()
                : List.of(new FixedClassifier(true, cohort));
        return result;
    }

    private static class FixedPrioritizer implements RequestPrioritizer<Object> {
        private final boolean applies;
        private final RequestPriority priority;

        FixedPrioritizer(boolean applies, RequestPriority priority) {
            this.applies = applies;
            this.priority = priority;
        }

        @Override
        public boolean appliesTo(Object request) {
            return applies;
        }

        @Override
        public RequestPriority priority(Object request) {
            return priority;
        }
    }

    private static class FixedClassifier implements RequestClassifier<Object> {
        private final boolean applies;
        private final int cohort;

        FixedClassifier(boolean applies, int cohort) {
            this.applies = applies;
            this.cohort = cohort;
        }

        @Override
        public boolean appliesTo(Object request) {
            return applies;
        }

        @Override
        public int cohort(Object request) {
            return cohort;
        }
    }

    private static class FailingPrioritizer implements RequestPrioritizer<Object> {
        @Override
        public boolean appliesTo(Object request) {
            return true;
        }

        @Override
        public RequestPriority priority(Object request) {
            throw new IllegalStateException("broken prioritizer");
        }
    }

    private static class FailingClassifier implements RequestClassifier<Object> {
        @Override
        public boolean appliesTo(Object request) {
            return true;
        }

        @Override
        public int cohort(Object request) {
            throw new IllegalStateException("broken classifier");
        }
    }

    // `appliesTo()` accepts everything, but the implementation is only written for strings
    private static class StringOnlyPrioritizer implements RequestPrioritizer<String> {
        @Override
        public boolean appliesTo(Object request) {
            return true;
        }

        @Override
        public RequestPriority priority(String request) {
            return RequestPriority.CRITICAL;
        }
    }
}
