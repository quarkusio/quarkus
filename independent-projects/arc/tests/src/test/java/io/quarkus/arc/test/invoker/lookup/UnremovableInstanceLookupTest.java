package io.quarkus.arc.test.invoker.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class UnremovableInstanceLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method).withInstanceLookup().build());
            }))
            .removeUnusedBeans(true)
            .addRemovalExclusion(it -> it.hasType(DotName.createSimple(InvokerHelper.class)))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar", invoker.invoke(null, null));
    }

    @Singleton
    static class MyService {
        public String hello() {
            return "foobar";
        }
    }
}
