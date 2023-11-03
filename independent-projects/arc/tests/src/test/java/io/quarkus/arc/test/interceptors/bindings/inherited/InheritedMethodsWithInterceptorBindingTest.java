package io.quarkus.arc.test.interceptors.bindings.inherited;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class InheritedMethodsWithInterceptorBindingTest {
    @RegisterExtension
    ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        MyBean bean = Arc.container().instance(MyBean.class).get();
        assertEquals("foobar", bean.foobar());
        assertEquals("intercepted: foobar", bean.foobarNotInherited());
    }

    static class MySuperclass {
        @MyInterceptorBinding
        String foobar() {
            return "this should be ignored";
        }

        @MyInterceptorBinding
        String foobarNotInherited() {
            return "foobar";
        }
    }

    @Dependent
    static class MyBean extends MySuperclass {
        @Override
        String foobar() {
            return "foobar";
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    static class MyInterceptor {
        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }

}
