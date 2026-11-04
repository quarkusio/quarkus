package io.quarkus.load.shedding;

/**
 * Assigns a cohort number to a request. There are 128 statically defined cohorts,
 * where the minimum cohort number is 0 and the maximum is 127, inclusive. All classifiers
 * are inspected and the first one that returns {@code true} for {@link #appliesTo(Object)}
 * is taken.
 * <p>
 * An implementation must be a CDI bean, otherwise it is ignored. CDI typesafe resolution
 * rules must be followed. That is, if multiple implementations exist with a different
 * {@link jakarta.annotation.Priority} value and some of them are {@code @Alternative}s,
 * only the alternatives with the highest priority value are retained. If no implementation
 * is an alternative, all implementations are retained and are sorted in descending
 * {@code @Priority} order (highest priority value comes first).
 * <p>
 * Note that the built-in classifier for HTTP requests declares no {@code @Priority} and
 * applies to every {@link io.vertx.ext.web.RoutingContext}, so an implementation that must
 * be inspected before it has to declare a positive {@code @Priority} value.
 * <p>
 * For HTTP requests, the type of the request ({@code R}) is {@link io.vertx.ext.web.RoutingContext}.
 *
 * @param <R> type of the request
 */
public interface RequestClassifier<R> {
    /**
     * @deprecated use {@link #minCohort()}
     */
    @Deprecated(forRemoval = true, since = "4.0")
    int MIN_COHORT = 0;

    /**
     * @deprecated use {@link #maxCohort()}
     */
    @Deprecated(forRemoval = true, since = "4.0")
    int MAX_COHORT = 127;

    /**
     * {@return the minimum cohort number}
     */
    static int minCohort() {
        return 0;
    }

    /**
     * {@return the maximum cohort number}
     * This is always one less than {@link #numCohorts()}.
     */
    static int maxCohort() {
        return 127;
    }

    /**
     * {@return the number of cohorts}
     * Guaranteed to be a power of 2.
     */
    static int numCohorts() {
        return 128;
    }

    /**
     * Returns whether this request classifier applies to given {@code request}.
     *
     * @param request the request, never {@code null}
     * @return whether this request classifier applies to given {@code request}
     */
    boolean appliesTo(Object request);

    /**
     * Returns the cohort to which the given {@code request} belongs. The result must
     * fall into {@code [minCohort, maxCohort]}.
     * <p>
     * The result should be stable for a given client for a relatively long time.
     * It may rotate slowly (e.g. every hour), so that the burden of being shed does not
     * always fall on the same clients.
     * <p>
     * If the result is outside {@code [minCohort, maxCohort]}, it is clamped by a bit mask
     * and so only lowest bits are used. They must be distributed evenly across clients,
     * otherwise the clients pile up in a small number of cohorts and load shedding may
     * exhibit pathological behavior. Note that classic Java hash codes rarely have this
     * property and require postprocessing (e.g. a Murmur avalanche mixer).
     *
     * @param request the request, never {@code null}
     * @return the cohort to which the given {@code request} belongs
     */
    int cohort(R request);
}
