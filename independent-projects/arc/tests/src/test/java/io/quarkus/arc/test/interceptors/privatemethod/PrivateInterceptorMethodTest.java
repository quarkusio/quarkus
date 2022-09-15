package io.quarkus.arc.test.interceptors.privatemethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PrivateInterceptorMethodTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            SimpleInterceptor.class);

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
