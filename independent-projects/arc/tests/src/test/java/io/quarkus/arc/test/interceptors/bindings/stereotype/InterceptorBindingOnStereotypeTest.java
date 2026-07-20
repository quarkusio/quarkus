package io.quarkus.arc.test.interceptors.bindings.stereotype;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class InterceptorBindingOnStereotypeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyStereotype.class,
            MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void testInterception() {
        assertEquals(0, MyInterceptor.aroundConstruct);
        assertEquals(0, MyInterceptor.postConstruct);
        assertEquals(0, MyInterceptor.aroundInvoke);
        assertEquals(0, MyInterceptor.preDestroy);

        InstanceHandle<MyBean> bean = Arc.container().instance(MyBean.class);
        MyBean instance = bean.get();

        assertEquals(1, MyInterceptor.aroundConstruct);
        assertEquals(1, MyInterceptor.postConstruct);
        assertEquals(0, MyInterceptor.aroundInvoke);
        assertEquals(0, MyInterceptor.preDestroy);

        instance.doSomething();

        assertEquals(1, MyInterceptor.aroundConstruct);
        assertEquals(1, MyInterceptor.postConstruct);
        assertEquals(1, MyInterceptor.aroundInvoke);
        assertEquals(0, MyInterceptor.preDestroy);

        bean.destroy();

        assertEquals(1, MyInterceptor.aroundConstruct);
        assertEquals(1, MyInterceptor.postConstruct);
        assertEquals(1, MyInterceptor.aroundInvoke);
        assertEquals(1, MyInterceptor.preDestroy);
    }

    @Singleton
    @MyStereotype
    static class MyBean {
        void doSomething() {
        }
    }

    @MyInterceptorBinding
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyStereotype {
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        static int aroundConstruct = 0;
        static int postConstruct = 0;
        static int aroundInvoke = 0;
        static int preDestroy = 0;

        @AroundConstruct
        Object aroundConstruct(InvocationContext ctx) throws Exception {
            aroundConstruct++;
            return ctx.proceed();
        }

        @PostConstruct
        Object postConstruct(InvocationContext ctx) throws Exception {
            postConstruct++;
            return ctx.proceed();
        }

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            aroundInvoke++;
            return ctx.proceed();
        }

        @PreDestroy
        Object preDestroy(InvocationContext ctx) throws Exception {
            preDestroy++;
            return ctx.proceed();
        }
    }
}
