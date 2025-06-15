package io.quarkus.arc.test.decorators.generics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SlightlyMoreComplexDecoratorWithTypeVariableTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Fun.class,
            FunctionDecorator1.class, FunctionDecorator2.class, FunctionDecorator3.class);

    @Test
    public void testFunction() {
        Fun strFun = Arc.container().instance(Fun.class).get();
        assertEquals(List.of("1", "foo", "bar", "baz"), strFun.apply(1));
    }

    @ApplicationScoped
    static class Fun implements Function<Integer, List<String>> {

        @Override
        public List<String> apply(Integer val) {
            return List.of(val.toString());
        }

    }

    @Priority(1)
    @Decorator
    static class FunctionDecorator1<T extends Number> implements Function<T, List<String>> {

        @Inject
        @Delegate
        Function<T, List<String>> delegate;

        @Override
        public List<String> apply(T val) {
            List<String> list = new ArrayList<>((List<String>) delegate.apply(val));
            list.add("baz");
            return list;
        }

    }

    @Priority(2)
    @Decorator
    static class FunctionDecorator2<T extends Number, L> implements Function<T, L> {

        @Inject
        @Delegate
        Function<T, L> delegate;

        @SuppressWarnings("unchecked")
        @Override
        public L apply(T val) {
            List<String> list = new ArrayList<>((List<String>) delegate.apply(val));
            list.add("bar");
            return (L) list;
        }

    }

    @Priority(3)
    @Decorator
    static class FunctionDecorator3 implements Function<Integer, List<String>> {

        @Inject
        @Delegate
        Function<Integer, List<String>> delegate;

        @Override
        public List<String> apply(Integer val) {
            // this should call Fun#apply()
            List<String> list = delegate.apply(val);
            return List.of(list.get(0), "foo");
        }

    }

}
