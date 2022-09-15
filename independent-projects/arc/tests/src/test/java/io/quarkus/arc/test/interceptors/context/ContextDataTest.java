package io.quarkus.arc.test.interceptors.context;

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

public class ContextDataTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Simple.class, SimpleBean.class,
            AlphaInterceptor.class, BravoInterceptor.class);

    @Test
    public void testContextData() throws IOException {
        assertEquals("alpha:bravo:ok:true", Arc.container().instance(SimpleBean.class).get().foo());
    }

    @Simple
    @Singleton
    static class SimpleBean {

        String foo() {
            return "ok";
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    public static class AlphaInterceptor {

        @AroundInvoke
        Object around(InvocationContext ctx) throws Exception {
            Object ret = "alpha:" + ctx.proceed();
            return ret + ":" + ctx.getContextData().get("bravo");
        }
    }

    @Simple
    @Priority(2)
    @Interceptor
    public static class BravoInterceptor {

        @AroundInvoke
        Object around(InvocationContext ctx) throws Exception {
            ctx.getContextData().put("bravo", true);
            return "bravo:" + ctx.proceed();
        }
    }

}
