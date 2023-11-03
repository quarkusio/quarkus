package io.quarkus.arc.test.interceptors.targetclass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundInvokeOnTargetClassAndSuperclassesWithOverridesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();
        assertEquals("super-intercepted: intercepted: foobar", bean.doSomething());
    }

    static class Alpha {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "this should not be called as the method is overridden in MyBean";
        }
    }

    static class Bravo extends Alpha {
        @AroundInvoke
        Object superIntercept(InvocationContext ctx) throws Exception {
            return "super-intercepted: " + ctx.proceed();
        }
    }

    @Singleton
    static class MyBean extends Bravo {
        String doSomething() {
            return "foobar";
        }

        @Override
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }
}
