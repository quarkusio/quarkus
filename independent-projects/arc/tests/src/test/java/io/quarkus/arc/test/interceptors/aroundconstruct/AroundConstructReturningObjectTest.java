package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AroundConstructReturningObjectTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    public static AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    @Test
    public void testInterception() {
        SimpleBean simpleBean = Arc.container().instance(SimpleBean.class).get();
        assertNotNull(simpleBean);
        assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Singleton
    @MyTransactional
    static class SimpleBean {

    }

    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @AroundConstruct
        Object mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            INTERCEPTOR_CALLED.set(true);
            return ctx.proceed();
        }

    }
}
