package io.quarkus.it.main;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.it.arc.UnusedBean;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ParameterResolverTest {

    @Inject
    UnusedBean unusedBean;

    @Test
    @ExtendWith(ParameterResolverTest.UnusedBeanDummyInputResolver.class)
    @ExtendWith(ParameterResolverTest.SupplierParameterResolver.class)
    public void testParameterResolver(UnusedBean.DummyInput dummyInput, Supplier<UnusedBean.DummyInput> supplier) {
        UnusedBean.DummyResult dummyResult = unusedBean.dummy(dummyInput);
        assertEquals("whatever/6", dummyResult.getResult());

        dummyResult = unusedBean.dummy(supplier.get());
        assertEquals("fromSupplier/0", dummyResult.getResult());
    }

    @Test
    @ExtendWith(ParameterResolverTest.SomeSerializableParameterResolver.class)
    public void testSerializableParameterResolver(SomeSerializable someSerializable) {
        assertEquals("foo", someSerializable.value);
        assertEquals("nested-foo", someSerializable.nested.value);
    }

    @Test
    @ExtendWith(ParameterResolverTest.ListWithNonSerializableParameterResolver.class)
    public void testSerializableParameterResolverFallbackToXStream(List<NonSerializable> list) {
        assertEquals("foo", list.get(0).value);
        assertEquals("bar", list.get(1).value);
    }

    public static class UnusedBeanDummyInputResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return UnusedBean.DummyInput.class.getName().equals(parameterContext.getParameter().getType().getName());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext,
                ExtensionContext extensionContext) throws ParameterResolutionException {
            // note: List.of(...) or Arrays.asList() fails on Java 16 due to: https://github.com/x-stream/xstream/issues/253
            return new UnusedBean.DummyInput("whatever", new UnusedBean.NestedDummyInput(new ArrayList<>(List.of(1, 2, 3))));
        }
    }

    public static class SupplierParameterResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return Supplier.class.getName().equals(parameterContext.getParameter().getType().getName());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return (Supplier<UnusedBean.DummyInput>) () -> new UnusedBean.DummyInput("fromSupplier",
                    // note: Collections.emptyList() fails on Java 16 due to: https://github.com/x-stream/xstream/issues/253
                    new UnusedBean.NestedDummyInput(new ArrayList<>()));
        }
    }

    public static class SomeSerializableParameterResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return SomeSerializable.class.getName().equals(parameterContext.getParameter().getType().getName());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return new SomeSerializable("foo", new SomeSerializable.SomeNestedSerializable("nested-foo"));
        }
    }

    public static class ListWithNonSerializableParameterResolver implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            return List.class.isAssignableFrom(parameterContext.getParameter().getType());
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
                throws ParameterResolutionException {
            // note: List.of(...) or Arrays.asList() fails on Java 16 due to: https://github.com/x-stream/xstream/issues/253
            return new ArrayList<>(List.of(new NonSerializable("foo"), new NonSerializable("bar")));
        }
    }

    public static class SomeSerializable implements Serializable {

        private final String value;
        private final SomeNestedSerializable nested;

        public SomeSerializable(String value, SomeNestedSerializable nested) {
            this.value = value;
            this.nested = nested;
        }

        public static class SomeNestedSerializable implements Serializable {

            private final String value;

            public SomeNestedSerializable(String value) {
                this.value = value;
            }
        }
    }

    public static class NonSerializable {

        private final String value;

        public NonSerializable(String value) {
            this.value = value;
        }
    }
}
