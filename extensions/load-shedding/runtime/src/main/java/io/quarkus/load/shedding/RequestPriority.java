package io.quarkus.load.shedding;

import java.util.List;

/**
 * A priority that can be assigned to a request by implementing the {@link RequestPrioritizer}.
 * There is 5 statically defined priority levels plus 1 level that is special:
 * <ul>
 * <li><em>never</em>: this request is never rejected</li>
 * <li><em>critical</em>: this request should not be rejected unless absolutely necessary</li>
 * <li><em>important</em>: this request should only be rejected under high load</li>
 * <li><em>normal</em>: this is a normal request</li>
 * <li><em>background</em>: this is a background request, it may be rejected if needed</li>
 * <li><em>degraded</em>: this request may be rejected freely</li>
 * </ul>
 *
 * Note that {@link #NEVER} is an exemption rather than the top of a priority ranking. While
 * priority load shedding is enabled, requests that are assigned {@code NEVER} are never rejected,
 * no matter how deep the overload.
 * <p>
 * It must be used sparingly. A request should only be assigned {@code NEVER} if it is part of
 * the operational control plane (health checks, in-memory metrics scraping, etc.), where failures
 * would trigger cascading failures, AND if it satisfies strict execution bounds. The request
 * should execute in sub-millisecond time, only read pre-aggregated values from memory, perform
 * no disk or network I/O, and require no thread offload.
 *
 * @see RequestPrioritizer
 */
public enum RequestPriority {
    NEVER(-1),
    CRITICAL(0),
    IMPORTANT(1),
    NORMAL(2),
    BACKGROUND(3),
    DEGRADED(4),
    ;

    private final int cohortBaseline;

    RequestPriority(int factor) {
        this.cohortBaseline = factor * RequestClassifier.numCohorts();
    }

    public int cohortBaseline() {
        return cohortBaseline;
    }

    /**
     * {@return the list of normal priority levels, that is, all but {@link #NEVER}}
     */
    public static List<RequestPriority> levels() {
        return List.of(CRITICAL, IMPORTANT, NORMAL, BACKGROUND, DEGRADED);
    }
}
