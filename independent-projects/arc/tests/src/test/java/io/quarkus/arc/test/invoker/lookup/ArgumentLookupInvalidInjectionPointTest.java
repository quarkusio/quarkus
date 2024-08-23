package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

// multiple kinds of invalid injection points exist for invoker lookup:
// - Bean
// - @Intercepted Bean
// - EventMetadata
// - InjectionPoint
// - @Named without value
// - maybe more
// in this test, we use `@Named` without value, for no particular reason
public class ArgumentLookupInvalidInjectionPointTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withArgumentLookup(0)
                        .build());
            }))
            .shouldFail()
            .build();

    @Test
    public void trigger() throws Exception {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains("@Named without value may not be used on method parameter"));
    }

    @Singleton
    static class MyService {
        public String hello(@Named MyDependency dependency) {
            return "foobar" + dependency.getId();
        }
    }

    @Singleton
    static class MyDependency {
        public int getId() {
            return 0;
        }
    }
}
