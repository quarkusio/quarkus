package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DefinitionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithMultipleInterceptionProxyParamsTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyProducer.class)
            .shouldFail()
            .build();

    @Test
    public void test() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertTrue(error instanceof DefinitionException);
        assertTrue(error.getMessage().contains("more than one InterceptionProxy parameter"));
    }

    static class MyNonbean1 {
    }

    static class MyNonbean2 {
    }

    static class MyNonbean3 {
    }

    @Dependent
    static class MyProducer {
        @Produces
        MyNonbean1 nonbean1(InterceptionProxy<MyNonbean2> nonbean2, InterceptionProxy<MyNonbean3> nonbean3) {
            return null;
        }
    }
}
