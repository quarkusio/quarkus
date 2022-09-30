package io.quarkus.arc.test.interceptors.reflectionless;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
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

public class AroundInvokeReflectionlessInterceptorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyInterceptorBinding.class, MyInterceptor.class, MyBean.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        MyBean bean = arc.instance(MyBean.class).get();

        assertEquals(42, bean.primitive());
        assertNotNull(MyInterceptor.method);
        assertFalse(MyInterceptor.method.isConstructor());
        assertFalse(MyInterceptor.method.isStatic());
        assertEquals("primitive", MyInterceptor.method.getName());
        assertEquals(MyBean.class, MyInterceptor.method.getDeclaringClass());
        assertEquals(0, MyInterceptor.method.getParameterCount());
        assertEquals(0, MyInterceptor.method.getParameters().length);
        assertEquals(0, MyInterceptor.method.getParameterTypes().length);
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyInterceptorBinding.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyFirstAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MySecondAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MyInterceptorBinding.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("foo", MyInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertNull(MyInterceptor.method.getAnnotation(MyFirstAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MySecondAnnotation.class));
        assertEquals(1, MyInterceptor.method.getAnnotations().length);

        assertEquals("hello", bean.clazz());
        assertNotNull(MyInterceptor.method);
        assertFalse(MyInterceptor.method.isConstructor());
        assertFalse(MyInterceptor.method.isStatic());
        assertEquals("clazz", MyInterceptor.method.getName());
        assertEquals(MyBean.class, MyInterceptor.method.getDeclaringClass());
        assertEquals(0, MyInterceptor.method.getParameterCount());
        assertEquals(0, MyInterceptor.method.getParameters().length);
        assertEquals(0, MyInterceptor.method.getParameterTypes().length);
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyInterceptorBinding.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MyFirstAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MySecondAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MyInterceptorBinding.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("bar", MyInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertNotNull(MyInterceptor.method.getAnnotation(MyFirstAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MySecondAnnotation.class));
        assertEquals(2, MyInterceptor.method.getAnnotations().length);

        assertEquals(Collections.emptyList(), bean.parameterized(null, 0));
        assertNotNull(MyInterceptor.method);
        assertFalse(MyInterceptor.method.isConstructor());
        assertFalse(MyInterceptor.method.isStatic());
        assertEquals("parameterized", MyInterceptor.method.getName());
        assertEquals(MyBean.class, MyInterceptor.method.getDeclaringClass());
        assertEquals(2, MyInterceptor.method.getParameterCount());
        assertEquals(2, MyInterceptor.method.getParameterTypes().length);
        assertEquals(List.class, MyInterceptor.method.getParameterTypes()[0]);
        assertEquals(int.class, MyInterceptor.method.getParameterTypes()[1]);
        assertEquals(2, MyInterceptor.method.getParameters().length);
        assertEquals(List.class, MyInterceptor.method.getParameters()[0].getType());
        assertEquals("param1", MyInterceptor.method.getParameters()[0].getName());
        assertEquals(0, MyInterceptor.method.getParameters()[0].getAnnotations().length);
        assertEquals(int.class, MyInterceptor.method.getParameters()[1].getType());
        assertEquals("param2", MyInterceptor.method.getParameters()[1].getName());
        assertEquals(0, MyInterceptor.method.getParameters()[1].getAnnotations().length);
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyInterceptorBinding.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MyFirstAnnotation.class));
        assertTrue(MyInterceptor.method.isAnnotationPresent(MySecondAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MyInterceptorBinding.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertEquals("baz", MyInterceptor.method.getAnnotation(MySimpleAnnotation.class).value());
        assertNotNull(MyInterceptor.method.getAnnotation(MyFirstAnnotation.class));
        assertNotNull(MyInterceptor.method.getAnnotation(MySecondAnnotation.class));
        assertEquals(3, MyInterceptor.method.getAnnotations().length);

        assertArrayEquals(new String[0], bean.array("", new String[0]));
        assertNotNull(MyInterceptor.method);
        assertFalse(MyInterceptor.method.isConstructor());
        assertFalse(MyInterceptor.method.isStatic());
        assertEquals("array", MyInterceptor.method.getName());
        assertEquals(MyBean.class, MyInterceptor.method.getDeclaringClass());
        assertEquals(2, MyInterceptor.method.getParameterCount());
        assertEquals(2, MyInterceptor.method.getParameterTypes().length);
        assertEquals(CharSequence.class, MyInterceptor.method.getParameterTypes()[0]);
        assertEquals(CharSequence[].class, MyInterceptor.method.getParameterTypes()[1]);
        assertEquals(2, MyInterceptor.method.getParameters().length);
        assertEquals(CharSequence.class, MyInterceptor.method.getParameters()[0].getType());
        assertEquals("typeVar", MyInterceptor.method.getParameters()[0].getName());
        assertEquals(1, MyInterceptor.method.getParameters()[0].getAnnotations().length);
        assertTrue(MyInterceptor.method.getParameters()[0].isAnnotationPresent(MyFirstAnnotation.class));
        assertFalse(MyInterceptor.method.getParameters()[0].isAnnotationPresent(MySecondAnnotation.class));
        assertNotNull(MyInterceptor.method.getParameters()[0].getAnnotation(MyFirstAnnotation.class));
        assertNull(MyInterceptor.method.getParameters()[0].getAnnotation(MySecondAnnotation.class));
        assertEquals(CharSequence[].class, MyInterceptor.method.getParameters()[1].getType());
        assertEquals("typeVarArray", MyInterceptor.method.getParameters()[1].getName());
        assertEquals(1, MyInterceptor.method.getParameters()[1].getAnnotations().length);
        assertFalse(MyInterceptor.method.getParameters()[1].isAnnotationPresent(MyFirstAnnotation.class));
        assertTrue(MyInterceptor.method.getParameters()[1].isAnnotationPresent(MySecondAnnotation.class));
        assertNull(MyInterceptor.method.getParameters()[1].getAnnotation(MyFirstAnnotation.class));
        assertNotNull(MyInterceptor.method.getParameters()[1].getAnnotation(MySecondAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyInterceptorBinding.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MySimpleAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MyFirstAnnotation.class));
        assertFalse(MyInterceptor.method.isAnnotationPresent(MySecondAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MyInterceptorBinding.class));
        assertNull(MyInterceptor.method.getAnnotation(MySimpleAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MyFirstAnnotation.class));
        assertNull(MyInterceptor.method.getAnnotation(MySecondAnnotation.class));
        assertEquals(0, MyInterceptor.method.getAnnotations().length);
    }

    @Target({ TYPE, METHOD, CONSTRUCTOR })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @Target({ TYPE, METHOD, CONSTRUCTOR })
    @Retention(RUNTIME)
    @interface MySimpleAnnotation {
        String value();
    }

    @Target({ TYPE, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface MyFirstAnnotation {
    }

    @Target({ TYPE, METHOD, PARAMETER })
    @Retention(RUNTIME)
    @interface MySecondAnnotation {
    }

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    @Reflectionless
    static class MyInterceptor {
        static MethodMetadata method;

        @AroundInvoke
        Object aroundInvoke(ArcInvocationContext ctx) throws Exception {
            assertNull(ctx.getMethod());
            assertNotNull(ctx.getMethodMetadata());
            method = ctx.getMethodMetadata();

            return ctx.proceed();
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean {
        @MySimpleAnnotation("foo")
        int primitive() {
            return 42;
        }

        @MySimpleAnnotation("bar")
        @MyFirstAnnotation
        String clazz() {
            return "hello";
        }

        @MySimpleAnnotation("baz")
        @MyFirstAnnotation
        @MySecondAnnotation
        List<String> parameterized(List<Integer> param1, int param2) {
            return Collections.emptyList();
        }

        <T extends CharSequence> String[] array(@MyFirstAnnotation T typeVar, @MySecondAnnotation T[] typeVarArray) {
            return new String[0];
        }
    }
}
