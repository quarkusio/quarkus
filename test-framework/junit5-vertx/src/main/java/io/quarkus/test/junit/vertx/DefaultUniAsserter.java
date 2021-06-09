package io.quarkus.test.junit.vertx;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Uni;

class DefaultUniAsserter implements UniAsserter {

    // use a Uni to chain the various operations together and allow us to subscribe to the result
    Uni<?> execution = Uni.createFrom().item(new Object());

    @SuppressWarnings("unchecked")
    @Override
    public <T> UniAsserter assertThat(Supplier<Uni<T>> uni, Consumer<T> asserter) {
        execution = uniFromSupplier(uni)
                .onItem()
                .invoke(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        asserter.accept((T) o);
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter execute(Supplier<Uni<T>> uni) {
        execution = uniFromSupplier(uni);
        return this;
    }

    @Override
    public UniAsserter execute(Runnable c) {
        execution = execution.onItem().invoke(c);
        return this;
    }

    @Override
    public <T> UniAsserter assertEquals(Supplier<Uni<T>> uni, T t) {
        execution = uniFromSupplier(uni)
                .onItem()
                .invoke(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        Assertions.assertEquals(t, o);
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter assertSame(Supplier<Uni<T>> uni, T t) {
        execution = uniFromSupplier(uni)
                .onItem()
                .invoke(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        Assertions.assertSame(t, o);
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter assertNull(Supplier<Uni<T>> uni) {
        execution = uniFromSupplier(uni).onItem().invoke(Assertions::assertNull);
        return this;
    }

    private <T> Uni<Object> uniFromSupplier(Supplier<Uni<T>> uni) {
        return execution.onItem()
                .transformToUni(new Function<Object, Uni<?>>() {
                    @Override
                    public Uni<?> apply(Object o) {
                        return uni.get();
                    }
                });
    }
}
