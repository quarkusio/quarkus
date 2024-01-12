package io.quarkus.test.vertx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;

import io.smallrye.mutiny.Uni;

public final class DefaultUniAsserter implements UnwrappableUniAsserter {

    private final ConcurrentMap<String, Object> data;

    // A Uni used to chain the various operations together and allow us to subscribe to the result
    Uni<?> execution;

    public DefaultUniAsserter() {
        this.execution = Uni.createFrom().item(new Object());
        this.data = new ConcurrentHashMap<>();
    }

    @Override
    public Uni<?> asUni() {
        return execution;
    }

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Consumer<Throwable> c) {
        execution = execution.onItem()
                .transformToUni((Function) new Function<Object, Uni<T>>() {
                    @Override
                    public Uni<T> apply(Object obj) {
                        return uni.get().onItemOrFailure().transformToUni((o, t) -> {
                            if (t == null) {
                                return Uni.createFrom().failure(() -> Assertions.fail("Uni did not contain a failure."));
                            } else {
                                return Uni.createFrom().item(() -> {
                                    c.accept(t);
                                    return null;
                                });
                            }
                        });
                    }
                });
        return this;
    }

    @Override
    public <T> UniAsserter assertFailedWith(Supplier<Uni<T>> uni, Class<? extends Throwable> c) {
        return assertFailedWith(uni, t -> {
            if (!c.isInstance(t)) {
                Assertions.fail(String.format("Uni failure type '%s' was not of the expected type '%s'.",
                        t.getClass().getName(), c.getName()));
            }
        });
    }

    @Override
    public Object getData(String key) {
        return data.get(key);
    }

    @Override
    public Object putData(String key, Object value) {
        return data.put(key, value);
    }

    @Override
    public void clearData() {
        data.clear();
    }

}
