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

public class AroundInvokeOnTargetClassAndOutsideAndSuperclassesWithOverridesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();
        assertEquals("super-outside: outside: target: foobar", bean.doSomething());
    }

    static class MyBeanSuperclass {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "this should not be called as the method is overridden in MyBean";
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean extends MyBeanSuperclass {
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

    static class MyInterceptorSuperclass {
        @AroundInvoke
        Object superIntercept(InvocationContext ctx) throws Exception {
            return "super-outside: " + ctx.proceed();
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor extends MyInterceptorSuperclass {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "outside: " + ctx.proceed();
        }
    }
}
