package io.quarkus.test.junit.vertx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

public interface UniAsserter {

    <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter);

    <T> UniAsserter execute(Supplier<Uni<T>> uni);

    UniAsserter execute(Runnable c);

    <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertNotEquals(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertNotSame(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertNull(Supplier<Uni<T>> uni);

    <T> UniAsserter assertNotNull(Supplier<Uni<T>> uni);

    // WARNING: this method is called via reflection by io.quarkus.hibernate.reactive.panache.common.runtime.TestReactiveTransactionalInterceptor
    <T> UniAsserter surroundWith(Function<Uni<T>, Uni<T>> uni);

    UniAsserter fail();

    UniAsserter assertTrue(Supplier<Uni<Boolean>> uni);

    UniAsserter assertFalse(Supplier<Uni<Boolean>> uni);

    /**
     * Used to determine whether or not the Uni contains the expected failure.
     * The assertions fails if the Uni does not fail.
     *
     * @param c Consumer that performs custom assertions on whether or not the failure matches expectations. This allows
     *        asserting on anything present in the {@code Throwable} like, the cause or the message
     */
    <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Consumer<Throwable> c);

    /**
     * Used to determine whether or not the Uni contains the expected failure type.
     * The assertions fails if the Uni does not fail.
     */
    <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Class<? extends Throwable> c);
}
