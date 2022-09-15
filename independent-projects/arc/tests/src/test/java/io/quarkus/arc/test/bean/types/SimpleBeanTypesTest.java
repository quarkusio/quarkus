package io.quarkus.arc.test.bean.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.Dependent;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SimpleBeanTypesTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() {
        InjectableBean<MyBean> bean = Arc.container().instance(MyBean.class).getBean();
        Set<Type> types = bean.getTypes();

        assertEquals(3, types.size());
        assertTrue(types.contains(Object.class));
        assertTrue(types.contains(MyBean.class));
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                Type genericClass = ((ParameterizedType) type).getRawType();
                assertEquals(MyInterface.class, genericClass);

                assertEquals(1, ((ParameterizedType) type).getActualTypeArguments().length);
                Type typeArg = ((ParameterizedType) type).getActualTypeArguments()[0];
                assertEquals(String.class, typeArg);
            }
        }
    }

    interface MyInterface<T> {
    }

    @Dependent
    static class MyBean implements MyInterface<String> {
    }
}
