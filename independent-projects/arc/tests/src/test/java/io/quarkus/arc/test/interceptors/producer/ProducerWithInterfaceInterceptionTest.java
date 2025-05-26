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

public class ProducerWithInterfaceInterceptionTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding1.class, MyInterceptor1.class,
            MyBinding2.class, MyInterceptor2.class, MyProducer.class);

    @Test
    public void test() {
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: hello1_foobar", nonbean.hello1());
        assertEquals("intercepted1: intercepted2: hello2_foobar", nonbean.hello2());
        assertEquals("intercepted2: hello3", nonbean.hello3());
        assertEquals("hello4_foobar", nonbean.hello4());
        assertEquals("hello5", nonbean.hello5());
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
    interface MyNonbean {
        String hello1();

        @MyBinding2
        String hello2();

        @MyBinding2
        @NoClassInterceptors
        default String hello3() {
            return "hello3";
        }

        @NoClassInterceptors
        String hello4();

        @NoClassInterceptors
        default String hello5() {
            return "hello5";
        };
    }

    static class MyNonbeanImpl implements MyNonbean {
        private final String value;

        MyNonbeanImpl(String value) {
            this.value = value;
        }

        @Override
        public String hello1() {
            return "hello1_" + value;
        }

        @Override
        public String hello2() {
            return "hello2_" + value;
        }

        @Override
        public String hello4() {
            return "hello4_" + value;
        }
    }

    @Dependent
    static class MyProducer {
        @Produces
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbeanImpl("foobar"));
        }
    }
}
