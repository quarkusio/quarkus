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

public class ObjectMethodButNotToStringInvokerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(ctx -> {
                BeanInfo bean = ctx.beans().withBeanClass(MyService.class).firstResult().orElseThrow();
                ClassInfo obj = ctx.get(BuildExtension.Key.INDEX).getClassByName(Object.class);
                MethodInfo method = obj.firstMethod("hashCode");
                ctx.getInvokerFactory().createInvoker(bean, method);
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DeploymentException.class, error);
        assertTrue(error.getMessage().contains("Cannot build invoker for target method"));
    }

    @Singleton
    static class MyService {
    }
}
