package io.quarkus.arc.test.interceptors.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BusinessLifecycleInterceptorTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    public static final AtomicInteger INTERCEPTORS_CALLED = new AtomicInteger();

    @Test
    public void testInterception() {
        InstanceHandle<SimpleBean> instance = Arc.container().instance(SimpleBean.class);
        SimpleBean simpleBean = instance.get();
        assertEquals(1, INTERCEPTORS_CALLED.get());
        assertEquals(2, simpleBean.ping());
        assertEquals(2, INTERCEPTORS_CALLED.get());
    }

    @Dependent
    @MyTransactional
    static class SimpleBean {

        int ping() {
            return 0;
        }

    }

    @Priority(1)
    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @PostConstruct
        @PreDestroy
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return INTERCEPTORS_CALLED.incrementAndGet();
        }

    }
}
