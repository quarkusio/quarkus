package io.quarkus.arc.test.interceptors.targetclass.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

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

public class AroundInvokeInterceptorNestingTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();
        assertEquals("expected-exception", bean.doSomething());
        assertEquals(List.of("MyInterceptor", "MyBean"), MyBean.invocations);
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean {
        static final List<String> invocations = new ArrayList<>();

        String doSomething() {
            throw new IllegalStateException("should not be called");
        }

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) {
            invocations.add(MyBean.class.getSimpleName());
            throw new IllegalArgumentException("intentional");
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor {
        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            try {
                MyBean.invocations.add(MyInterceptor.class.getSimpleName());
                return ctx.proceed();
            } catch (IllegalArgumentException e) {
                return "expected-exception";
            }
        }
    }
}
