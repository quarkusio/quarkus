package io.quarkus.arc.test.interceptors.synthbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SynthBeanWithParameterizedConstructorsAndInterceptionTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBinding1.class, MyInterceptor1.class, MyBinding2.class, MyInterceptor2.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    context.configure(MyNonbean.class)
                            .types(MyNonbean.class)
                            .injectInterceptionProxy(MyNonbean.class)
                            .creator(MyNonbeanCreator.class)
                            .done();
                }
            })
            .build();

    static class MyNonbeanCreator implements BeanCreator<MyNonbean> {
        static int constructorId;

        @Override
        public MyNonbean create(SyntheticCreationalContext<MyNonbean> context) {
            InterceptionProxy<MyNonbean> proxy = context.getInterceptionProxy();
            if (constructorId == 0) {
                return proxy.create(new MyNonbean());
            } else if (constructorId == 1) {
                return proxy.create(new MyNonbean(1));
            } else if (constructorId == 2) {
                return proxy.create(new MyNonbean(2, 3));
            } else if (constructorId == 3) {
                return proxy.create(new MyNonbean((byte) 4, (short) 5));
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Test
    public void constructorWithNoParams() {
        MyNonbeanCreator.constructorId = 0;

        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: intercepted2: hello1_0_0", nonbean.hello1());
        assertEquals("intercepted2: hello2_0_0_6", nonbean.hello2(6));
        assertEquals("hello3_0_0", nonbean.hello3());
    }

    @Test
    public void constructorWithIntParam() {
        MyNonbeanCreator.constructorId = 1;

        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: intercepted2: hello1_1_0", nonbean.hello1());
        assertEquals("intercepted2: hello2_1_0_7", nonbean.hello2(7));
        assertEquals("hello3_1_0", nonbean.hello3());
    }

    @Test
    public void constructorWithIntIntParams() {
        MyNonbeanCreator.constructorId = 2;

        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: intercepted2: hello1_2_3", nonbean.hello1());
        assertEquals("intercepted2: hello2_2_3_8", nonbean.hello2(8));
        assertEquals("hello3_2_3", nonbean.hello3());
    }

    @Test
    public void constructorWithByteShortParams() {
        MyNonbeanCreator.constructorId = 3;

        MyNonbean nonbean = Arc.container().instance(MyNonbean.class).get();
        assertEquals("intercepted1: intercepted2: hello1_4_5", nonbean.hello1());
        assertEquals("intercepted2: hello2_4_5_9", nonbean.hello2(9));
        assertEquals("hello3_4_5", nonbean.hello3());
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
        private final int i;
        private final int j;

        MyNonbean() {
            this.i = 0;
            this.j = 0;
        }

        MyNonbean(int i) {
            this.i = i;
            this.j = 0;
        }

        MyNonbean(int i, int j) {
            this.i = i;
            this.j = j;
        }

        MyNonbean(byte i, short j) {
            this.i = i;
            this.j = j;
        }

        @MyBinding2
        String hello1() {
            return "hello1_" + i + "_" + j;
        }

        @NoClassInterceptors
        @MyBinding2
        String hello2(int k) {
            return "hello2_" + i + "_" + j + "_" + k;
        }

        @NoClassInterceptors
        String hello3() {
            return "hello3_" + i + "_" + j;
        }
    }
}
