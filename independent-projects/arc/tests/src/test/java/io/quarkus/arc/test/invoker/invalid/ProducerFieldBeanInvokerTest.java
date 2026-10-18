package io.quarkus.arc.test.invoker.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerFieldBeanInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyProducer.class)
            .beanRegistrars(ctx -> {
                BeanInfo bean = ctx.beans().producerFields().withBeanType(MyService.class).firstResult().orElseThrow();
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                ctx.getInvokerFactory().createInvoker(bean, method);
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Cannot build invoker for target bean"));
    }

    static class MyService {
        String hello() {
            return "foobar";
        }
    }

    @Singleton
    static class MyProducer {
        @Produces
        MyService producer = new MyService();
    }
}
