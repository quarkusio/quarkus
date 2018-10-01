package org.jboss.protean.arc.test.interceptors.bindingdefaultvalue;

import static org.junit.Assert.assertEquals;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class BindingDefaultValueTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(MyTransactional.class, SimpleBean.class, AlphaInterceptor.class, BravoInterceptor.class);

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

    @MyTransactional("alpha")
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
