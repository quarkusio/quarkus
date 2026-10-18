package io.quarkus.arc.test.interceptors.aroundconstruct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructWithParameterChangeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(SimpleBean.class, MyDependency.class,
            MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        SimpleBean simpleBean = Arc.container().instance(SimpleBean.class).get();
        assertNotNull(simpleBean);
        assertNotNull(simpleBean.dependency);
        assertEquals("from interceptor", simpleBean.dependency.value);
    }

    @Singleton
    @MyInterceptorBinding
    static class SimpleBean {
        final MyDependency dependency;

        @Inject
        SimpleBean(MyDependency dependency) {
            this.dependency = dependency;
        }
    }

    @Singleton
    static class MyDependency {
        final String value;

        MyDependency() {
            this("default");
        }

        MyDependency(String value) {
            this.value = value;
        }
    }

    @Target({ ElementType.TYPE, ElementType.CONSTRUCTOR })
    @Retention(RetentionPolicy.RUNTIME)
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        @AroundConstruct
        void aroundConstruct(InvocationContext ctx) throws Exception {
            ctx.setParameters(new Object[] { new MyDependency("from interceptor") });
            ctx.proceed();
        }
    }
}
