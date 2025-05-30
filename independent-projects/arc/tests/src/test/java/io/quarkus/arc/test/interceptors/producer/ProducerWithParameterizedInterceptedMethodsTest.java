package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithParameterizedInterceptedMethodsTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding1.class, MyInterceptor1.class,
            MyBinding2.class, MyInterceptor2.class, MyProducer.class);

    @Test
    public void test() {
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: intercepted2: hello1_1_foobar", nonbean.hello1(1));
        assertEquals("intercepted1: hello2_2_3_foobar", nonbean.hello2(2, 3));
        assertEquals("intercepted2: hello3_4_5_6_foobar", nonbean.hello3(4, 5, 6));
        assertEquals("hello4_7_foobar", nonbean.hello4(7));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding1 {
    }

    @MyBinding1
    @Priority(1)
    @Interceptor
    static class MyInterceptor1 {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted1: " + ctx.proceed();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding2 {
    }

    @MyBinding2
    @Priority(2)
    @Interceptor
    static class MyInterceptor2 {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted2: " + ctx.proceed();
        }
    }

    @MyBinding1
    static class MyNonbean {
        private final String value;

        MyNonbean() {
            this(null);
        }

        MyNonbean(String value) {
            this.value = value;
        }

        @MyBinding2
        String hello1(int i) {
            return "hello1_" + i + "_" + value;
        }

        String hello2(int i, int j) {
            return "hello2_" + i + "_" + j + "_" + value;
        }

        @NoClassInterceptors
        @MyBinding2
        String hello3(int i, int j, int k) {
            return "hello3_" + i + "_" + j + "_" + k + "_" + value;
        }

        @NoClassInterceptors
        String hello4(int i) {
            return "hello4_" + i + "_" + value;
        }
    }

    @Dependent
    static class MyProducer {
        @Produces
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean("foobar"));
        }
    }
}
