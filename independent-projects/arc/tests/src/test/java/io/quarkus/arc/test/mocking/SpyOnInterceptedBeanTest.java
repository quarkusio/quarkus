package io.quarkus.arc.test.mocking;

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
import org.mockito.Mockito;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class SpyOnInterceptedBeanTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();

        MyBean spy = Mockito.spy(bean);
        Mockito.when(spy.getValue()).thenReturn("quux");

        assertEquals("intercepted: intercepted: quux42", spy.doSomething(42));
        Mockito.verify(spy).doSomething(Mockito.anyInt());
        Mockito.verify(spy).doSomethingElse();
        Mockito.verify(spy).getValue();
    }

    @Singleton
    static class MyBean {
        @MyInterceptorBinding
        String doSomething(int param) {
            return doSomethingElse() + param;
        }

        @MyInterceptorBinding
        String doSomethingElse() {
            return getValue();
        }

        String getValue() {
            return "foobar";
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
            return "intercepted: " + ctx.proceed();
        }
    }
}
