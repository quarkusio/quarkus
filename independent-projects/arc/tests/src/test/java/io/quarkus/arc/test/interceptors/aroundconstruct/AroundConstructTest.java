package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.Assert.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Singleton;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class AroundConstructTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    public static AtomicBoolean INTERCEPTOR_CALLED = new AtomicBoolean(false);

    @Test
    public void testInterception() {
        SimpleBean simpleBean = Arc.container().instance(SimpleBean.class).get();
        assertNotNull(simpleBean);
        Assert.assertTrue(INTERCEPTOR_CALLED.get());
    }

    @Singleton
    @MyTransactional
    static class SimpleBean {

    }

    @MyTransactional
    @Interceptor
    public static class SimpleInterceptor {

        @AroundConstruct
        void mySuperCoolAroundConstruct(InvocationContext ctx) throws Exception {
            INTERCEPTOR_CALLED.set(true);
            ctx.proceed();
        }

    }
}
