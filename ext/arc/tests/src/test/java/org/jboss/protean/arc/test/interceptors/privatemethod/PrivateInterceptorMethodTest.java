package org.jboss.protean.arc.test.interceptors.privatemethod;

import static org.junit.Assert.assertEquals;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.jboss.protean.arc.test.interceptors.Simple;
import org.junit.Rule;
import org.junit.Test;

public class PrivateInterceptorMethodTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class, SimpleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        assertEquals("privatefoo", simpleBean.foo());
    }

    @Singleton
    static class SimpleBean {

        @Simple
        String foo() {
            return "foo";
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        private Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return "private" + ctx.proceed();
        }
    }

}
