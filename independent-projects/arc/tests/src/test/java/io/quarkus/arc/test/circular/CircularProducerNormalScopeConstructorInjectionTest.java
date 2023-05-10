package io.quarkus.arc.test.circular;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class CircularProducerNormalScopeConstructorInjectionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyQualifier.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertEquals("foobarquux", bean.get());
    }

    static class MyValue {
        private final String value;

        // for client proxy
        MyValue() {
            this.value = null;
        }

        MyValue(String value) {
            this.value = value;
        }

        String get() {
            return value;
        }
    }

    @Dependent
    static class MyBean {
        @Produces
        @ApplicationScoped
        MyValue producerMethod() {
            return new MyValue("foobar");
        }

        @Produces
        @ApplicationScoped
        @MyQualifier
        MyValue producerField = new MyValue("quux");

        MyValue foobar;

        MyValue quux;

        @Inject
        MyBean(MyValue foobar, @MyQualifier MyValue quux) {
            this.foobar = foobar;
            this.quux = quux;
        }

        String get() {
            return foobar.get() + quux.get();
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @interface MyQualifier {
    }
}
