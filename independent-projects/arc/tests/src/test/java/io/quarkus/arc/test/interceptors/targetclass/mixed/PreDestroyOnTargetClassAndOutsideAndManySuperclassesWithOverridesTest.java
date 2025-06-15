package io.quarkus.arc.test.interceptors.targetclass.mixed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;
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

public class PreDestroyOnTargetClassAndOutsideAndManySuperclassesWithOverridesTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBean.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void test() {
        ArcContainer arc = Arc.container();
        arc.instance(MyBean.class).destroy();
        assertEquals(List.of("Foxtrot", "MyInterceptor", "Charlie", "MyBean"), MyBean.invocations);
    }

    static class Alpha {
        @PreDestroy
        void intercept() throws Exception {
            MyBean.invocations.add("this should not be called as the method is overridden in MyBean");
        }
    }

    static class Bravo extends Alpha {
        @PreDestroy
        void specialIntercept() {
            MyBean.invocations.add("this should not be called as the method is overridden in Charlie");
        }
    }

    static class Charlie extends Bravo {
        @PreDestroy
        void superIntercept() throws Exception {
            MyBean.invocations.add(Charlie.class.getSimpleName());
        }

        @Override
        void specialIntercept() {
            MyBean.invocations.add("this is not an interceptor method");
        }
    }

    @Singleton
    @MyInterceptorBinding
    static class MyBean extends Charlie {
        static final List<String> invocations = new ArrayList<>();

        @Override
        @PreDestroy
        void intercept() throws Exception {
            invocations.add(MyBean.class.getSimpleName());
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    @interface MyInterceptorBinding {
    }

    static class Delta {
        @PreDestroy
        Object intercept(InvocationContext ctx) throws Exception {
            MyBean.invocations.add("this should not be called as the method is overridden in MyInterceptor");
            return ctx.proceed();
        }
    }

    static class Echo extends Delta {
        @PreDestroy
        void specialIntercept(InvocationContext ctx) throws Exception {
            MyBean.invocations.add("this should not be called as the method is overridden in Foxtrot");
        }
    }

    static class Foxtrot extends Echo {
        @PreDestroy
        Object superIntercept(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(Foxtrot.class.getSimpleName());
            return ctx.proceed();
        }

        @Override
        void specialIntercept(InvocationContext ctx) {
            MyBean.invocations.add("this is not an interceptor method");
        }
    }

    @MyInterceptorBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor extends Foxtrot {
        @Override
        @PreDestroy
        Object intercept(InvocationContext ctx) throws Exception {
            MyBean.invocations.add(MyInterceptor.class.getSimpleName());
            return ctx.proceed();
        }
    }
}
