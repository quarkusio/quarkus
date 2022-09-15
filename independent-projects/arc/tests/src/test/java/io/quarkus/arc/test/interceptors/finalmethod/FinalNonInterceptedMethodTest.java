package io.quarkus.arc.test.interceptors.finalmethod;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FinalNonInterceptedMethodTest {

    static final String VAL = "ping";

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testInvocation() {
        SimpleBean bean = Arc.container().instance(SimpleBean.class).get();
        assertEquals(VAL, bean.foo());
        assertEquals("a" + VAL, bean.bar());
    }

    @Singleton
    static class SimpleBean {

        private String val;

        @PostConstruct
        void init() {
            val = VAL;
        }

        // This method is final but not intercepted = OK
        final String foo() {
            return val;
        }

        @Simple
        String bar() {
            return val;
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return "a" + ctx.proceed();
        }
    }

}
