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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> Uni<T> uniFromSupplier(Supplier<Uni<T>> uni) {
        return execution.onItem()
                .transformToUni((Function) new Function<Object, Uni<T>>() {
                    @Override
                    public Uni<T> apply(Object o) {
                        return uni.get();
                    }
                });
    }

    @Override
    public <T> UniAsserter assertNotEquals(Supplier<Uni<T>> uni, T t) {
        execution = uniFromSupplier(uni)
                .onItem()
                .invoke(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        Assertions.assertNotEquals(t, o);
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter assertNotSame(Supplier<Uni<T>> uni, T t) {
        execution = uniFromSupplier(uni)
                .onItem()
                .invoke(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) {
                        Assertions.assertNotSame(t, o);
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter assertNotNull(Supplier<Uni<T>> uni) {
        execution = uniFromSupplier(uni).onItem().invoke(Assertions::assertNotNull);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> UniAsserter surroundWith(Function<Uni<T>, Uni<T>> uni) {
        execution = uni.apply((Uni<T>) execution);
        return this;
    }

    @Override
    public UniAsserter assertFalse(Supplier<Uni<Boolean>> uni) {
        execution = uniFromSupplier(uni).onItem().invoke(Assertions::assertFalse);
        return this;
    }

    @Override
    public UniAsserter assertTrue(Supplier<Uni<Boolean>> uni) {
        execution = uniFromSupplier(uni).onItem().invoke(Assertions::assertTrue);
        return this;
    }

    @Override
    public UniAsserter fail() {
        execution = execution.onItem().invoke(v -> Assertions.fail());
        return this;
    }
}
