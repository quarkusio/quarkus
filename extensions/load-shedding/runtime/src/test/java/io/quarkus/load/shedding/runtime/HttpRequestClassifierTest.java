package io.quarkus.load.shedding.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.load.shedding.RequestClassifier;

public class HttpRequestClassifierTest {
    private static final int FIRST_HOUR = 400_000;
    private static final int NUM_HOURS = 2_000;

    private static List<String> hosts() {
        List<String> result = new ArrayList<>();
        for (int subnet = 1; subnet <= 3; subnet++) {
            for (int host = 1; host <= 254; host++) {
                result.add("10.0." + subnet + "." + host);
            }
        }
        return result;
    }

    @Test
    public void cohortIsInRange() {
        for (String host : hosts()) {
            for (int hour = FIRST_HOUR; hour < FIRST_HOUR + NUM_HOURS; hour++) {
                assertThat(HttpRequestClassifier.cohort(host, hour))
                        .isBetween(0, RequestClassifier.numCohorts() - 1);
            }
        }
    }

    @Test
    public void cohortIsStableWithinHour() {
        for (String host : hosts()) {
            int cohort = HttpRequestClassifier.cohort(host, FIRST_HOUR);
            for (int i = 0; i < 10; i++) {
                assertThat(HttpRequestClassifier.cohort(host, FIRST_HOUR)).isEqualTo(cohort);
            }
        }
    }

    @Test
    public void unknownHostIsAccepted() {
        for (int hour = FIRST_HOUR; hour < FIRST_HOUR + NUM_HOURS; hour++) {
            assertThat(HttpRequestClassifier.cohort("", hour))
                    .isBetween(RequestClassifier.minCohort(), RequestClassifier.maxCohort());
        }
    }

    // Clients must be spread over all cohorts, otherwise the amount of load that priority shedding
    // rejects doesn't follow the threshold. With 762 clients, the ideal is 6 per cohort.
    @Test
    public void cohortsAreEvenlyPopulated() {
        List<String> hosts = hosts();
        for (int hour = FIRST_HOUR; hour < FIRST_HOUR + NUM_HOURS; hour++) {
            int[] counts = new int[RequestClassifier.numCohorts()];
            for (String host : hosts) {
                counts[HttpRequestClassifier.cohort(host, hour)]++;
            }

            int occupied = 0;
            int busiest = 0;
            for (int count : counts) {
                if (count > 0) {
                    occupied++;
                }
                busiest = Math.max(busiest, count);
            }

            // The bounds are deliberately loose: assigning 762 clients to 128 cohorts based
            // on a hash sometimes leaves a few cohorts empty and others overfilled. We only
            // attempt to catch big irregularities.
            assertThat(occupied).isGreaterThanOrEqualTo(100);
            assertThat(busiest).isLessThanOrEqualTo(50);
        }
    }

    // Each hour must reshuffle the clients, not shift them all by the same amount.
    @Test
    public void cohortsAreReshuffledEachHour() {
        List<String> hosts = hosts();
        for (int hour = FIRST_HOUR; hour < FIRST_HOUR + NUM_HOURS; hour++) {
            Set<Integer> shifts = new HashSet<>();
            for (String host : hosts) {
                shifts.add(HttpRequestClassifier.cohort(host, hour + 1) - HttpRequestClassifier.cohort(host, hour));
            }

            assertThat(shifts).hasSizeGreaterThanOrEqualTo(100);
        }
    }
}
