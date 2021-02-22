package io.quarkus.arc.test.interceptors.finalmethod;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.test.ArcTestContainer;
import io.quarkus.arc.test.interceptors.Simple;
import javax.annotation.Priority;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FinalInterceptedMethodTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().beanClasses(Simple.class, SimpleBean.class,
            SimpleInterceptor.class).shouldFail().build();

    @Test
    public void testFailure() {
        Throwable t = container.getFailure();
        assertNotNull(t);
        assertTrue(t instanceof DeploymentException);
        assertTrue(t.getMessage().contains("foo"));
        assertTrue(t.getMessage().contains("bar"));
    }

    @Simple
    @Singleton
    static class SimpleBean {

        final String foo() {
            return "foo";
        }

        final void bar() {
        }

    }

    @Simple
    @Priority(1)
    @Interceptor
    static class SimpleInterceptor {

        @AroundInvoke
        Object mySuperCoolAroundInvoke(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

}
