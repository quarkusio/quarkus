package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class ArrayBeanTypesTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(Producer.class);

    @Test
    public <T> void test() {
        InjectableBean<Object> intArray = Arc.container().instance("intArray").getBean();
        assertBeanTypes(intArray, Object.class, int[][].class);

        InjectableBean<Object> stringArray = Arc.container().instance("stringArray").getBean();
        assertBeanTypes(stringArray, Object.class, String[].class);

        InjectableBean<Object> listArray = Arc.container().instance("listArray").getBean();
        assertBeanTypes(listArray, Object.class, new TypeLiteral<List<String>[]>() {
        }.getType());
    }

    private void assertBeanTypes(Bean<?> bean, Type... expectedTypes) {
        Set<Type> types = bean.getTypes();

        assertEquals(expectedTypes.length, types.size());
        for (Type expectedType : expectedTypes) {
            assertTrue(types.contains(expectedType));
        }
    }

    @Dependent
    static class Producer<T extends Number> {
        @Produces
        @Dependent
        @Named("intArray")
        int[][] intArray() {
            return new int[0][0];
        }

        @Produces
        @Dependent
        @Named("stringArray")
        String[] stringArray() {
            return new String[0];
        }

        @Produces
        @Dependent
        @Named("listArray")
        List<String>[] listArray() {
            return (List<String>[]) new List<?>[0];
        }
    }
}
