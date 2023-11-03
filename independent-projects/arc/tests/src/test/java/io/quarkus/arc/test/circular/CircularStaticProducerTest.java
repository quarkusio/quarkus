package io.quarkus.arc.test.circular;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class CircularStaticProducerTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyQualifier.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertEquals("foobarquux", bean.value);
    }

    @Dependent
    static class MyBean {
        @Produces
        @Dependent
        static String producerMethod() {
            return "foobar";
        }

        @Produces
        @Dependent
        @MyQualifier
        static String producerField = "quux";

        final String value;

        @Inject
        MyBean(String foobar, @MyQualifier String quux) {
            this.value = foobar + quux;
        }
    }

    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
    @interface MyQualifier {
    }
}
