package io.quarkus.arc.test.invoker.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import jakarta.enterprise.invoke.Invoker;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.invoker.InvokerHelper;
import io.quarkus.arc.test.invoker.InvokerHelperRegistrar;

public class ReturnValueTransformerBoxingTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyService.class)
            .beanRegistrars(new InvokerHelperRegistrar(MyService.class, (bean, factory, invokers) -> {
                ClassInfo clazz = bean.getImplClazz();
                MethodInfo ping = clazz.firstMethod("ping");
                MethodInfo flag = clazz.firstMethod("flag");
                MethodInfo big = clazz.firstMethod("big");
                MethodInfo fraction = clazz.firstMethod("fraction");
                MethodInfo hello = clazz.firstMethod("hello");

                invokers.put("wrapPing", factory.createInvoker(bean, ping)
                        .withReturnValueTransformer(Transformers.class, "wrap")
                        .build());
                invokers.put("wrapFlag", factory.createInvoker(bean, flag)
                        .withReturnValueTransformer(Transformers.class, "wrap")
                        .build());
                invokers.put("wrapBig", factory.createInvoker(bean, big)
                        .withReturnValueTransformer(Transformers.class, "wrap")
                        .build());
                invokers.put("wrapFraction", factory.createInvoker(bean, fraction)
                        .withReturnValueTransformer(Transformers.class, "wrap")
                        .build());
                invokers.put("stringifyPing", factory.createInvoker(bean, ping)
                        .withReturnValueTransformer(Transformers.class, "stringify")
                        .build());
                invokers.put("primitivePing", factory.createInvoker(bean, ping)
                        .withReturnValueTransformer(Transformers.class, "primitive")
                        .build());
                invokers.put("lengthHello", factory.createInvoker(bean, hello)
                        .withReturnValueTransformer(Transformers.class, "length")
                        .build());
                invokers.put("widenHello", factory.createInvoker(bean, hello)
                        .withReturnValueTransformer(Transformers.class, "widen")
                        .build());
            }))
            .build();

    static final Object MARKER = new Object();

    static class Transformers {
        static <T> List<T> wrap(T result) {
            return List.of(result);
        }

        static String stringify(Object result) {
            return "" + result;
        }

        static String primitive(int result) {
            return "i" + result;
        }

        static int length(String result) {
            return result.length();
        }

        static Object widen(String result) {
            return MARKER;
        }
    }

    @Test
    public void primitiveResultIntoAnyTypeTransformer() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, List<Integer>> wrapPing = helper.getInvoker("wrapPing");
        assertEquals(List.of(42), wrapPing.invoke(service.get(), null));

        Invoker<MyService, List<Boolean>> wrapFlag = helper.getInvoker("wrapFlag");
        assertEquals(List.of(true), wrapFlag.invoke(service.get(), null));

        Invoker<MyService, List<Long>> wrapBig = helper.getInvoker("wrapBig");
        assertEquals(List.of(1L << 40), wrapBig.invoke(service.get(), null));

        Invoker<MyService, List<Double>> wrapFraction = helper.getInvoker("wrapFraction");
        assertEquals(List.of(0.5), wrapFraction.invoke(service.get(), null));
    }

    @Test
    public void primitiveResultIntoObjectTransformer() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> stringifyPing = helper.getInvoker("stringifyPing");
        assertEquals("42", stringifyPing.invoke(service.get(), null));
    }

    @Test
    public void primitiveResultIntoPrimitiveTransformer() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, String> primitivePing = helper.getInvoker("primitivePing");
        assertEquals("i42", primitivePing.invoke(service.get(), null));
    }

    @Test
    public void transformerReturningPrimitive() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, Integer> lengthHello = helper.getInvoker("lengthHello");
        assertEquals(5, lengthHello.invoke(service.get(), null));
    }

    @Test
    public void transformerReturningObjectOfAnotherType() throws Exception {
        InvokerHelper helper = Arc.container().instance(InvokerHelper.class).get();
        InstanceHandle<MyService> service = Arc.container().instance(MyService.class);

        Invoker<MyService, Object> widenHello = helper.getInvoker("widenHello");
        assertSame(MARKER, widenHello.invoke(service.get(), null));
    }

    @Singleton
    static class MyService {
        public int ping() {
            return 42;
        }

        public boolean flag() {
            return true;
        }

        public long big() {
            return 1L << 40;
        }

        public double fraction() {
            return 0.5;
        }

        public String hello() {
            return "hello";
        }
    }
}
