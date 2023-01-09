package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Iterator;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;

public class GenericBeanTypesTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void customGenericBean() {
        InjectableBean<Object> bean = Arc.container().instance("myBean").getBean();
        Set<Type> types = bean.getTypes();

        assertEquals(3, types.size());
        assertTrue(types.contains(Object.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertTrue(MyBean.class.equals(genericClass) || Iterable.class.equals(genericClass));

                assertEquals(1, ((ParameterizedType) type).getActualTypeArguments().length);
                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertTrue(typeArg instanceof TypeVariable);
                assertEquals("T", ((TypeVariable) typeArg).getName());

                assertEquals(1, ((TypeVariable) typeArg).getBounds().length);
                Type bound = ((TypeVariable) typeArg).getBounds()[0];
                assertTrue(bound instanceof ParameterizedType);
                assertEquals(Comparable.class, ((ParameterizedType) bound).getRawType());

                assertEquals(1, ((ParameterizedType) bound).getActualTypeArguments().length);
                Type boundTypeArg = ((ParameterizedType) bound).getActualTypeArguments()[0];
                assertTrue(boundTypeArg instanceof TypeVariable);
                assertEquals("T", ((TypeVariable) boundTypeArg).getName());
                // recursive
            }
        }
    }

    @Dependent
    @Named("myBean")
    static class MyBean<T extends Comparable<T>> implements Iterable<T> {
        @Override
        public Iterator<T> iterator() {
            return null;
        }
    }
}
