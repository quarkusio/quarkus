package io.quarkus.arc.test.interceptors.bindingdefaultvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BindingDefaultValueTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class,
            AlphaInterceptor.class,
            BravoInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        assertEquals("foo::alpha", simpleBean.ping());
    }

    @Singleton
    static class SimpleBean {

        @MyTransactional // This should only match AlphaInterceptor
        String ping() {
            return "foo";
        }

    }

    @MyTransactional
    @Priority(1)
    @Interceptor
    public static class AlphaInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed() + "::alpha";
        }
    }

    @MyTransactional("bravo")
    @Priority(2)
    @Interceptor
    public static class BravoInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed() + "::bravo";
        }
    }

}
