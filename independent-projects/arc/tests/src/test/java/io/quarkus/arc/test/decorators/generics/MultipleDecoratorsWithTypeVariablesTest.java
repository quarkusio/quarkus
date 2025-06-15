package io.quarkus.arc.test.decorators.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class MultipleDecoratorsWithTypeVariablesTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(StringFunction.class,
            FunctionDecorator1.class, FunctionDecorator2.class, StringConsumer.class, ConsumerDecorator1.class,
            ConsumerDecorator2.class, StringSupplier.class, SupplierDecorator1.class, SupplierDecorator2.class);

    @Test
    public void testFunction() {
        StringFunction strFun = Arc.container().instance(StringFunction.class).get();
        assertEquals("FOOFOO", strFun.apply(" foo "));
    }

    @Test
    public void testConsumer() {
        StringConsumer strConsumer = Arc.container().instance(StringConsumer.class).get();
        strConsumer.accept("baz");
        assertEquals("barbar", StringConsumer.CONSUMED.get());
    }

    @Test
    public void testSupplier() {
        StringSupplier strSupplier = Arc.container().instance(StringSupplier.class).get();
        assertEquals("stringsupplier_Foo", strSupplier.get());
    }

    @ApplicationScoped
    static class StringSupplier implements Supplier<String> {

        @Override
        public String get() {
            return StringSupplier.class.getSimpleName();
        }

    }

    @ApplicationScoped
    static class StringConsumer implements Consumer<String> {

        static final AtomicReference<String> CONSUMED = new AtomicReference<>();

        @Override
        public void accept(String value) {
            CONSUMED.set(value);
        }

    }

    @ApplicationScoped
    static class StringFunction implements Function<String, String> {

        @Override
        public String apply(String val) {
            return val.trim();
        }

    }

    @Priority(1)
    @Decorator
    static class FunctionDecorator1<T> implements Function<String, T> {

        @Inject
        @Delegate
        Function<String, T> delegate;

        @SuppressWarnings("unchecked")
        @Override
        public T apply(String val) {
            return (T) delegate.apply(val).toString().repeat(2);
        }

    }

    @Priority(3)
    @Decorator
    static class FunctionDecorator2<T> implements Function<T, T> {

        @Inject
        @Delegate
        Function<T, T> delegate;

        @SuppressWarnings("unchecked")
        @Override
        public T apply(T val) {
            return (T) delegate.apply(val).toString().toUpperCase();
        }

    }

    @Priority(1)
    @Decorator
    static class SupplierDecorator1 implements Supplier<String> {

        @Inject
        @Delegate
        Supplier<String> delegate;

        @Override
        public String get() {
            return delegate.get() + "_Foo";
        }

    }

    @Priority(2)
    @Decorator
    static class SupplierDecorator2<T> implements Supplier<T> {

        @Inject
        @Delegate
        Supplier<T> delegate;

        @SuppressWarnings("unchecked")
        @Override
        public T get() {
            return (T) delegate.get().toString().toLowerCase();
        }

    }

    @Priority(1)
    @Decorator
    static class ConsumerDecorator1<T> implements Consumer<T> {

        @Inject
        @Delegate
        Consumer<T> delegate;

        @SuppressWarnings("unchecked")
        @Override
        public void accept(T val) {
            delegate.accept((T) "bar");
        }

    }

    @Priority(2)
    @Decorator
    static class ConsumerDecorator2 implements Consumer<String> {

        @Inject
        @Delegate
        Consumer<String> delegate;

        @Override
        public void accept(String val) {
            delegate.accept(val + val);
        }

    }

}
