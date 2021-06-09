package io.quarkus.test.junit.vertx;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

public interface UniAsserter {

    <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter);

    <T> UniAsserter execute(Supplier<Uni<T>> uni);

    UniAsserter execute(Runnable c);

    <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t);

    <T> UniAsserter assertNull(Supplier<Uni<T>> uni);
}
