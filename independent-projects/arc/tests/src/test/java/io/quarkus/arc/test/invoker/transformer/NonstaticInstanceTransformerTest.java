package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class NonstaticInstanceTransformerTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put(method.name(), factory.createInvoker(bean, method)
                        .withInstanceTransformer(MyService.class, "transform")
                        .build());
            }))
            .build();

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> hello = helper.getInvoker("hello");
        assertEquals("transformedfoobar0[]", hello.invoke(service.get(), new Object[] { 0, List.of() }));
    }

    @Singleton
    static class MyService {
        private final String id;

        public MyService() {
            this("default");
        }

        public MyService(String id) {
            this.id = id;
        }

        public MyService transform() {
            return new MyService("transformed");
        }

        public String hello(int param1, List<String> param2) {
            return id + "foobar" + param1 + param2;
        }
    }
}
