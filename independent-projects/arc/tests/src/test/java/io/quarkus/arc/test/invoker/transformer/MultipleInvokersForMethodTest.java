package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

public class MultipleInvokersForMethodTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                MethodInfo method = bean.getImplClazz().firstMethod("hello");
                invokers.put("change1", factory.createInvoker(bean, method)
                        .withReturnValueTransformer(Change1.class, "change")
                        .build());
                invokers.put("change2", factory.createInvoker(bean, method)
                        .withReturnValueTransformer(Change2.class, "change")
                        .build());
                invokers.put("change3", factory.createInvoker(bean, method)
                        .withReturnValueTransformer(Change3.class, "change")
                        .build());
            }))
            .build();

    static class Change1 {
        static String change(String result) {
            return result + "1";
        }
    }

    static class Change2 {
        static String change(String result) {
            return result + "2";
        }
    }

    static class Change3 {
        static String change(String result) {
            return result + "3";
        }
    }

    @Test
    public void test() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();

        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> invoker1 = helper.getInvoker("change1");
        assertEquals("foobar1", invoker1.invoke(service.get(), null));

        Invoker<MyService, String> invoker2 = helper.getInvoker("change2");
        assertEquals("foobar2", invoker2.invoke(service.get(), null));

        Invoker<MyService, String> invoker3 = helper.getInvoker("change3");
        assertEquals("foobar3", invoker3.invoke(service.get(), null));
    }

    @Singleton
    static class MyService {
        public String hello() {
            return "foobar";
        }
    }
}
