package io.quarkus.load.shedding;

/**
 * Assigns a cohort number to a request. There is 128 statically defined cohorts,
 * where the minimum cohort number is 1 and maximum is 128, inclusive. All classifiers
 * are inspected and the first one that returns {@code true} for {@link #appliesTo(Object)}
 * is taken.
 * <p>
 * An implementation must be a CDI bean, otherwise it is ignored. CDI typesafe resolution
 * rules must be followed. That is, if multiple implementations are provided with different
 * {@link jakarta.annotation.Priority} values, only the implementations with the highest
 * priority are retained.
 *
 * @param <R> type of the request
 */
public interface RequestClassifier<R> {
    int MIN_COHORT = 1;

    int MAX_COHORT = 128;

    /**
     * Returns whether this request classifier applies to given {@code request}.
     *
     * @param request the request, never {@code null}
     * @return whether this request classifier applies to given {@code request}
     */
    boolean appliesTo(Object request);

    /**
     * Returns the cohort to which the given {@code request} belongs.
     *
     * @param request the request, never {@code null}
     * @return the cohort to which the given {@code request} belongs
     */
    int cohort(R request);
}
