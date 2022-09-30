package io.quarkus.arc.test.interceptors.reflectionless;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.inject.Singleton;
import javax.interceptor.AroundConstruct;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.ArcInvocationContext;
import io.quarkus.arc.MethodMetadata;
import io.quarkus.arc.Reflectionless;
import io.quarkus.arc.test.ArcTestContainer;

public class AroundConstructReflectionlessInterceptorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyInterceptorBinding.class, MyInterceptor.class, MyBean.class,
            MyDependency.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();
        assertNotNull(bean);

        assertNotNull(MyInterceptor.method);
        assertTrue(MyInterceptor.method.isConstructor());
        assertFalse(MyInterceptor.method.isStatic());
        assertEquals(MyBean.class.getName(), MyInterceptor.method.getName());
        assertEquals(MyBean.class, MyInterceptor.method.getDeclaringClass());
        assertEquals(1, MyInterceptor.method.getParameterCount());
        assertEquals(1, MyInterceptor.method.getParameterTypes().length);
        assertEquals(MyDependency.class, MyInterceptor.method.getParameterTypes()[0]);
        assertEquals(1, MyInterceptor.method.getParameters().length);
        assertEquals(MyDependency.class, MyInterceptor.method.getParameters()[0].getType());
        assertEquals("dependency", MyInterceptor.method.getParameters()[0].getName());
        assertEquals(1, MyInterceptor.method.getParameters()[0].getAnnotations().length);
        assertTrue(MyInterceptor.method.getParameters()[0].isAnnotationPresent(MySimpleAnnotation.class));
        assertFalse(MyInterceptor.method.getParameters()[0].isAnnotationPresent(Override.class));
        assertNotNull(MyInterceptor.method.getParameters()[0].getAnnotation(MySimpleAnnotation.class));
        assertEquals("quux", MyInterceptor.method.getParameters()[0].getAnnotation(MySimpleAnnotation.class).value());
        assertNull(MyInterceptor.method.getParameters()[0].getAnnotation(Override.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MyInterceptorBinding.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(Override.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MyInterceptorBinding.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("foobar", MyInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertNull(MyInterceptor.method.getAnnotation(Override.class));
        assertEquals(2, MyInterceptor.method.getAnnotations().length);
    }

    @Target({ TYPE, METHOD, CONSTRUCTOR })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @Target({ TYPE, METHOD, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    @interface MySimpleAnnotation {
        String value();
    }

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    @Reflectionless
    static class MyInterceptor {
        static MethodMetadata method;

        @AroundConstruct
        Object aroundConstruct(ArcInvocationContext ctx) throws Exception {
            assertNull(ctx.getMethod());
            assertNotNull(ctx.getMethodMetadata());
            method = ctx.getMethodMetadata();

            return ctx.proceed();
        }
    }

    @Singleton
    static class MyBean {
        @MyInterceptorBinding
        @MySimpleAnnotation("foobar")
        public MyBean(@MySimpleAnnotation("quux") MyDependency dependency) {
        }
    }

    @Dependent
    static class MyDependency {
    }
}
