package io.quarkus.arc.test.interceptors.targetclass.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class PostConstructOnTargetClassAndOutsideAndSuperclassesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        arc.instance(MyBean.class);
        assertEquals(List.of("MyInterceptorSuperclass", "MyInterceptor", "MyBeanSuperclass", "MyBean"), MyBean.invocations);
    }

    static class MyBeanSuperclass {
        @PostConstruct
        void superPostConstruct() {
            MyBean.invocations.add(MyBeanSuperclass.class.getSimpleName());
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean extends MyBeanSuperclass {
        static final List<String> invocations = new ArrayList<>();

        @PostConstruct
        void postConstruct() {
            invocations.add(MyBean.class.getSimpleName());
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    static class MyInterceptorSuperclass {
        @PostConstruct
        void superPostConstruct(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(MyInterceptorSuperclass.class.getSimpleName());
            ctx.proceed();
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    public static class MyInterceptor extends MyInterceptorSuperclass {
        @PostConstruct
        Object postConstruct(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(MyInterceptor.class.getSimpleName());
            return ctx.proceed();
        }
    }
}
