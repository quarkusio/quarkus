package io.quarkus.test.vertx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

/**
 * Used in conjunction with {@link RunOnVertxContext} (or similar annotations) in order to test assertions on APIs that
 * return {@link Uni} and whose execution needs to occur on a Vert.x Event Loop Thread (such as Hibernate Reactive for
 * example). The following example shows how this class is meant to be used:
 *
 * <pre>
 * &#64;QuarkusTest
 * public class SomeTestClass {
 *
 *     &#64;RunOnVertxContext
 *     &#64;Test
 *     public void someTest(UniAsserter asserter) {
 *         asserter.assertNotNull(() -> MyAPI.someObject()).assertEquals(() -> MyAPI.count(), 0L);
 *     }
 * }
 * </pre>
 *
 * As can be seen in example, {@code UniAsserter} is used as parameter of a test method for a class annotated with
 * {@code QuarkusTest}.
 * <p>
 * As far as the API is concerned, each assertion in the pipeline is executed when the result of the corresponding Uni
 * becomes available.
 */
public interface UniAsserter {

    /**
     * Perform a custom assertion on the value of the {@code Uni} in cases where no exception was thrown.
     * <p>
     * If an exception is expected, then one of {@link #assertFailedWith} methods should be used.
     */
    <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter);

    /**
     * Execute some arbitrary operation as part of the pipeline.
     */
    <T> UniAsserter execute(Supplier<Uni<T>> uni);

    /**
     * Execute some arbitrary operation as part of the pipeline.
     */
    UniAsserter execute(Runnable c);

    /**
     * Assert that the value of the {@code Uni} is equal to the expected value. If both are {@code null}, they are
     * considered equal.
     *
     * @see Object#equals(Object)
     */
    <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t);

    /**
     * Assert that the value of the {@code Uni} is not equal to the expected value. Fails if both are {@code null}.
     *
     * @see Object#equals(Object)
     */
    <T> UniAsserter assertNotEquals(Supplier<Uni<T>> uni, T t);

    /**
     * Assert that the value of the {@code Uni} and the expected value refer to the same object.
     */
    <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t);

    /**
     * Assert that the value of the {@code Uni} and the expected value do not refer to the same object.
     */
    <T> UniAsserter assertNotSame(Supplier<Uni<T>> uni, T t);

    /**
     * Assert that the value of the {@code Uni} is {@code null}.
     * <p>
     * If an exception is expected, then one of {@link #assertFailedWith} methods should be used.
     */
    <T> UniAsserter assertNull(Supplier<Uni<T>> uni);

    /**
     * Assert that the value of the {@code Uni} is not {@code null}.
     * <p>
     * If an exception is expected, then one of {@link #assertFailedWith} methods should be used.
     */
    <T> UniAsserter assertNotNull(Supplier<Uni<T>> uni);

    // WARNING: this method is called via reflection by
    // io.quarkus.hibernate.reactive.panache.common.runtime.TestReactiveTransactionalInterceptor
    @SuppressWarnings("unused")
    <T> UniAsserter surroundWith(Function<Uni<T>, Uni<T>> uni);

    /**
     * Ensure that the whole pipeline always fails
     */
    UniAsserter fail();

    /**
     * Assert that the value of the {@code Uni} is {@code true}.
     */
    UniAsserter assertTrue(Supplier<Uni<Boolean>> uni);

    /**
     * Assert that the value of the {@code Uni} is {@code false}.
     */
    UniAsserter assertFalse(Supplier<Uni<Boolean>> uni);

    /**
     * Used to determine whether the {@code Uni} contains the expected failure. The assertions fail if the {@code Uni}
     * does not fail.
     *
     * @param c
     *        Consumer that performs custom assertions on whether the failure matches expectations. This allows
     *        asserting on anything present in the {@code Throwable} like, the cause or the message
     */
    <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Consumer<Throwable> c);

    /**
     * Used to determine whether the {@code Uni} contains the expected failure type. The assertions fail if the
     * {@code Uni} does not fail.
     */
    <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Class<? extends Throwable> c);

    /**
     * @return the test data associated with the given key, or {@code null}
     *
     * @see #putData(String, Object)
     */
    Object getData(String key);

    /**
     * Associate the value with the given key.
     *
     * @return the previous value, or {@code null} if there was no data for the given key
     */
    Object putData(String key, Object value);

    /**
     * Clear the test data.
     */
    void clearData();
}
