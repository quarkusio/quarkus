package org.jboss.protean.arc.test.interceptors.bindings;

import static org.junit.Assert.assertTrue;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.InvocationContextImpl;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.jboss.protean.arc.test.interceptors.Simple;
import org.junit.Rule;
import org.junit.Test;

public class InvocationContextBindingsTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(Simple.class, MyTransactional.class, SimpleBean.class, SimpleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        // [@org.jboss.protean.arc.test.interceptors.Simple(),
        // @org.jboss.protean.arc.test.interceptors.bindings.MyTransactional(value={java.lang.String.class})]::foo
        String ret = simpleBean.foo();
        assertTrue(ret.contains(Simple.class.getName()));
        assertTrue(ret.contains(MyTransactional.class.getName()));
        assertTrue(ret.contains(String.class.getName()));
    }

    @Singleton
    static class SimpleBean {

        @MyTransactional({ String.class })
        @Simple
        String foo() {
            return "foo";
        }

    }

    @Simple
    @MyTransactional
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            Object bindings = ctx.getContextData().get(InvocationContextImpl.KEY_INTERCEPTOR_BINDINGS);
            if (bindings != null) {
                return bindings.toString() + "::" + ctx.proceed();
            }
            return ctx.proceed();
        }
    }

}
