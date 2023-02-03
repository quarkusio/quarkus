package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class, MyDependency.class, SomeAroundInvokeInterceptor.class);

    public static AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    @Test
    public void testInterception() {
        SimpleBean simpleBean = Arc.container().instance(SimpleBean.class).get();
        assertNotNull(simpleBean);
        Assertions.assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Singleton
    @MyTransactional
    static class SimpleBean {

        @Inject
        SimpleBean(MyDependency foo) {

        }

        // the method has to remain here in order to trigger subclass creation due to at least one around invoke
        // intercepted method
        public void ping() {

        }

    }

    @Singleton
    static class MyDependency {

        long created;

        MyDependency() {
            created = System.currentTimeMillis();
        }

        public long getCreated() {
            return created;
        }
    }

    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @AroundConstruct
        void mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            INTERCEPTOR_CALLED.set(true);
            assertTrue(ctx.getParameters().length == 1);
            Object param = ctx.getParameters()[0];
            assertTrue(param instanceof MyDependency);
            assertEquals(Arc.container().instance(MyDependency.class).get().getCreated(), ((MyDependency) param).getCreated());
            ctx.proceed();
        }

    }

    @MyTransactional
    @Interceptor
    public static class SomeAroundInvokeInterceptor {

        @AroundInvoke
        public Object someAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }

    }
}
