package io.quarkus.arc.test.interceptor.producer;

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
import io.quarkus.arc.Unremovable;
import io.quarkus.test.QuarkusUnitTest;

public class ProducerWithoutZeroParamCtorAndInterceptionTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBinding.class, MyInterceptor.class, MyNonbean.class, MyProducer.class));

    @Test
    public void test() {
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted: hello1_foobar", nonbean.hello1());
        assertEquals("hello2_foobar", nonbean.hello2());
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding {
    }

    @MyBinding
    @Priority(1)
    @Interceptor
    static class MyInterceptor {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }

    static class MyNonbean {
        private final String value;

        private MyNonbean(String value) {
            this.value = value;
        }

        @MyBinding
        String hello1() {
            return "hello1_" + value;
        }

        String hello2() {
            return "hello2_" + value;
        }
    }

    @Dependent
    static class MyProducer {
        @Produces
        @Unremovable
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean("foobar"));
        }
    }
}
