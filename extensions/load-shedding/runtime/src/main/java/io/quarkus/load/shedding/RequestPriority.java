package io.quarkus.load.shedding;

/**
 * A priority that can be assigned to a request by implementing the {@link RequestPrioritizer}.
 * There is 5 statically defined priority levels:
 * <ul>
 * <li><em>critical</em>: this request should almost never be rejected</li>
 * <li><em>important</em>: this request should only be rejected under high load</li>
 * <li><em>normal</em>: this is a normal request</li>
 * <li><em>background</em>: this is a background request, it may be rejected if needed</li>
 * <li><em>degraded</em>: this request may be rejected freely</li>
 * </ul>
 *
 * @see RequestPrioritizer
 */
public enum RequestPriority {
    CRITICAL(0),
    IMPORTANT(1),
    NORMAL(2),
    BACKGROUND(3),
    DEGRADED(4),
    ;

    private final int cohortBaseline;

    RequestPriority(int factor) {
        this.cohortBaseline = factor * RequestClassifier.MAX_COHORT;
    }

    public int cohortBaseline() {
        return cohortBaseline;
    }
}
