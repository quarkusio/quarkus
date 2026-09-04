package io.quarkus.runtime.logging;

import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;

/**
 * A wrapper carrying the console banner supplier as a service value.
 * <p>
 * This exists so that the banner can flow through the logging service dependency graph as a service of a
 * dedicated type, rather than as a service typed directly on {@link Supplier} (which is too generic to use as
 * a service key).
 *
 * @param supplier the banner text supplier (must not be {@code null})
 */
public record LogBanner(Supplier<String> supplier) {

    /**
     * Construct a new instance.
     *
     * @param supplier the banner text supplier (must not be {@code null})
     */
    public LogBanner {
        Assert.checkNotNullParam("supplier", supplier);
    }
}
