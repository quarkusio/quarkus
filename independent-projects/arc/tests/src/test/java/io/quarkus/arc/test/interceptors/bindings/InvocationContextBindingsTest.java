package io.quarkus.arc.test.interceptors.bindings;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class InvocationContextBindingsTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, MyTransactional.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testInterception() {
        ArcContainer arc = Arc.container();
        SimpleBean simpleBean = arc.instance(SimpleBean.class).get();
        // [@io.quarkus.arc.test.interceptors.Simple(),
        // @io.quarkus.arc.test.interceptors.bindings.MyTransactional(value={java.lang.String.class})]::foo
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
            Object bindings = ctx.getContextData().get(ArcInvocationContext.KEY_INTERCEPTOR_BINDINGS);
            if (bindings != null) {
                return bindings.toString() + "::" + ctx.proceed();
            }
            return ctx.proceed();
        }
    }

}
