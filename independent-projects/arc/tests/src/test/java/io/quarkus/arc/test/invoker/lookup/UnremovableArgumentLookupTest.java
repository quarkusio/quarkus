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

public class UnremovableArgumentLookupTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class, MyDependency.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method).withArgumentLookup(0).build());
            }))
            .removeUnusedBeans(true)
            .addRemovalExclusion(it -> it.hasType(DotName.createSimple(MyService.class))
                    || it.hasType(DotName.createSimple(InvokerHelper.class)))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        MyService service = Arc.container().instance(MyService.class).get();

        Invoker<MyService, String> invoker = helper.getInvoker("hello");
        assertEquals("foobar0", invoker.invoke(service, new Object[] { null }));
    }

    @Singleton
    static class MyService {
        public String hello(MyDependency dependency) {
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
