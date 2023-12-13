package io.quarkus.arc.test.invoker.invalid;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.test.ArcTestContainer;

public class MethodFromDifferentClassInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyOtherService.class)
            .beanRegistrars(ctx -> {
                BeanInfo bean = ctx.beans().withBeanClass(MyService.class).firstResult().orElseThrow();
                ClassInfo other = ctx.get(BuildExtension.Key.INDEX).getClassByName(MyOtherService.class);
                MethodInfo method = other.firstMethod("doSomething");
                ctx.getInvokerFactory().createInvoker(bean, method);
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Method does not belong to target bean"));
    }

    @Singleton
    static class MyService {
    }

    @Singleton
    static class MyOtherService {
        public void doSomething() {
        }
    }
}
