package io.quarkus.arc.test.introspection;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.beans.IntrospectionException;
import java.beans.Introspector;

import jakarta.enterprise.context.RequestScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.test.ArcTestContainer;

// https://github.com/quarkusio/quarkus/issues/52553
public class IntrospectionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() throws IntrospectionException {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertInstanceOf(ClientProxy.class, bean);
        assertDoesNotThrow(() -> {
            Introspector.getBeanInfo(bean.getClass());
        });
    }

    public static class Box<T> {
        private T value;

        public T get() {
            return value;
        }

        public void set(T value) {
            this.value = value;
        }
    }

    @RequestScoped
    public static class MyBean extends Box<String> {
    }
}
