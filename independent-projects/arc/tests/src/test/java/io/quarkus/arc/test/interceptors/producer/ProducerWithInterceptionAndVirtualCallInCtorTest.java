package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

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
import io.quarkus.arc.test.ArcTestContainer;

public class ProducerWithInterceptionAndVirtualCallInCtorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor.class, MyProducer.class);

    @Test
    public void test() {
        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted: hello", nonbean.hello());
        assertEquals("intercepted: MyNonbean", nonbean.getThisClass_intercepted());
        assertEquals("MyNonbean", nonbean.getThisClass_notIntercepted());

        assertEquals(List.of(
                "MyNonbean",
                nonbean.getClass().getSimpleName()),
                MyNonbean.constructedInstances_intercepted);
        assertEquals(MyNonbean.constructedInstances_intercepted, MyNonbean.constructedInstances_notIntercepted);
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
        static final List<String> constructedInstances_intercepted = new ArrayList<>();
        static final List<String> constructedInstances_notIntercepted = new ArrayList<>();

        MyNonbean() {
            constructedInstances_intercepted.add(getThisClass_intercepted());
            constructedInstances_notIntercepted.add(getThisClass_notIntercepted());
        }

        @MyBinding
        String getThisClass_intercepted() {
            return this.getClass().getSimpleName();
        }

        String getThisClass_notIntercepted() {
            return this.getClass().getSimpleName();
        }

        @MyBinding
        String hello() {
            return "hello";
        }
    }

    @Dependent
    static class MyProducer {
        @Produces
        MyNonbean produce(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean());
        }
    }
}
