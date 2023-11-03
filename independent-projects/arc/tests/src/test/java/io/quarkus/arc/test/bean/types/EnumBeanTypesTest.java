package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class EnumBeanTypesTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(ExtendedBooleanProducer.class);

    @Test
    public void test() {
        InjectableBean<ExtendedBoolean> bean = Arc.container().instance(ExtendedBoolean.class).getBean();
        Set<Type> types = bean.getTypes();

        assertEquals(5, types.size());
        assertTrue(types.contains(Object.class));
        assertTrue(types.contains(Serializable.class));
        assertTrue(types.contains(ExtendedBoolean.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertTrue(Enum.class.equals(genericClass) || Comparable.class.equals(genericClass));

                assertEquals(1, ((ParameterizedType) type).getActualTypeArguments().length);
                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertEquals(ExtendedBoolean.class, typeArg);
            }
        }
    }

    enum ExtendedBoolean {
        TRUE,
        FALSE,
        FILE_NOT_FOUND,
    }

    @Dependent
    static class ExtendedBooleanProducer {
        @Produces
        ExtendedBoolean produce() {
            return null;
        }
    }
}
