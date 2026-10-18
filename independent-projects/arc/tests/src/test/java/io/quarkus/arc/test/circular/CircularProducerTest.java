package io.quarkus.arc.test.circular;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import io.quarkus.arc.test.ArcTestContainer;

public class CircularProducerTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer.Builder()
            .beanClasses(MyBean.class, MyQualifier.class)
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(IllegalStateException.class, error);
        assertTrue(error.getMessage().contains("Circular dependencies not supported"));
    }

    @Dependent
    static class MyBean {
        @Produces
        @Dependent
        String producerMethod() {
            return "foobar";
        }

        @Produces
        @Dependent
        @MyQualifier
        String producerField = "quux";

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
