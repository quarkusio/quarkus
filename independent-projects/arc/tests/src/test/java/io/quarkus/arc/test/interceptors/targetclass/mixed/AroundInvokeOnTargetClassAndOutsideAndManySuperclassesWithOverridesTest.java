package io.quarkus.arc.test.interceptors.targetclass.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundInvokeOnTargetClassAndOutsideAndManySuperclassesWithOverridesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();
        assertEquals("super-outside: outside: super-target: target: foobar", bean.doSomething());
    }

    static class Alpha {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "this should not be called as the method is overridden in MyBean";
        }
    }

    static class Bravo extends Alpha {
        @AroundInvoke
        Object specialIntercept(InvocationContext ctx) {
            return "this should not be called as the method is overridden in Charlie";
        }
    }

    static class Charlie extends Bravo {
        @AroundInvoke
        Object superIntercept(InvocationContext ctx) throws Exception {
            return "super-target: " + ctx.proceed();
        }

        @Override
        Object specialIntercept(InvocationContext ctx) {
            return "this is not an interceptor method";
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean extends Charlie {
        String doSomething() {
            return "foobar";
        }

        @Override
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "target: " + ctx.proceed();
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    static class Delta {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "this should not be called as the method is overridden in MyInterceptor";
        }
    }

    static class Echo extends Delta {
        @AroundInvoke
        Object specialIntercept(InvocationContext ctx) throws Exception {
            return "this should not be called as the method is overridden in Charlie";
        }
    }

    static class Foxtrot extends Echo {
        @AroundInvoke
        Object superIntercept(InvocationContext ctx) throws Exception {
            return "super-outside: " + ctx.proceed();
        }

        @Override
        Object specialIntercept(InvocationContext ctx) {
            return "this is not an interceptor method";
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor extends Foxtrot {
        @Override
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "outside: " + ctx.proceed();
        }
    }
}
