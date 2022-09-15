package io.quarkus.arc.test.interceptors.constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class SubclassConstructorGuardTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            SimpleInterceptor.class);

    @Test
    public void testConstructorGuard() throws IOException {
        assertEquals("foo::bar", Arc.container().instance(SimpleBean.class).get().foo());
    }

    @Simple
    @Singleton
    static class SimpleBean {

        private String val;

        public SimpleBean() {
            init();
        }

        void init() {
            this.val = "bar";
        }

        String foo() {
            return val;
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return "foo" + "::" + ctx.proceed();
        }
    }

}
