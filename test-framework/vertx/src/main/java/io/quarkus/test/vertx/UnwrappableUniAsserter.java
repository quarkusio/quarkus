package io.quarkus.test.vertx;

import io.smallrye.mutiny.Uni;

/**
 * A {@link UniAsserter} that exposes the internal {@link Uni}.
 * <p>
 * We've added this interface so that we don't expose the method {@link #asUni()} to the user
 * </p>
 */
interface UnwrappableUniAsserter extends UniAsserter {

    /**
     * @return a {@link Uni} representing the operations pipeline up to this point
     */
    Uni<?> asUni();
}
