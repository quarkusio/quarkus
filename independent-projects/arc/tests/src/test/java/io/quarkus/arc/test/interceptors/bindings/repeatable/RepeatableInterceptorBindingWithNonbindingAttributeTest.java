package io.quarkus.arc.test.interceptors.bindings.repeatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.AroundConstruct;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests repeating interceptor binding usage, with a nonbinding annotation attribute.
 */
public class RepeatableInterceptorBindingWithNonbindingAttributeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyBinding.List.class,
            MethodInterceptedBean.class, IncrementingInterceptor.class);

    @BeforeEach
    public void setUp() {
        IncrementingInterceptor.AROUND_CONSTRUCT.set(false);
        IncrementingInterceptor.POST_CONSTRUCT.set(false);
        IncrementingInterceptor.PRE_DESTROY.set(false);
    }

    @Test
    public void methodLevelInterceptor() {
        MethodInterceptedBean bean = Arc.container().instance(MethodInterceptedBean.class).get();

        assertEquals(11, bean.foo());
        assertEquals(21, bean.foobar());
        assertEquals(31, bean.foobaz());
        assertEquals(41, bean.foobarbaz());
        assertEquals(50, bean.nonannotated());

        // the instance is created lazily (the bean is @ApplicationScoped),
        // so the around construct interceptor is also called lazily,
        // when the first method call is made on the client proxy
        assertTrue(IncrementingInterceptor.AROUND_CONSTRUCT.get());

        // post-construct and pre-destroy interceptors aren't called,
        // because there are no class-level interceptor bindings
        assertFalse(IncrementingInterceptor.POST_CONSTRUCT.get());
        assertFalse(IncrementingInterceptor.PRE_DESTROY.get());
    }

    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(MyBinding.List.class)
    @InterceptorBinding
    @interface MyBinding {
        @Nonbinding
        String value();

        @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
        @Retention(RetentionPolicy.RUNTIME)
        @interface List {
            MyBinding[] value();
        }
    }

    @ApplicationScoped
    static class MethodInterceptedBean {
        @MyBinding("foo")
        @MyBinding("bar")
        public MethodInterceptedBean() {
        }

        @MyBinding("foo")
        public int foo() {
            return 10;
        }

        @MyBinding("foo")
        @MyBinding("bar")
        public int foobar() {
            return 20;
        }

        @MyBinding("foo")
        @MyBinding("baz")
        public int foobaz() {
            return 30;
        }

        @MyBinding("foo")
        @MyBinding("bar")
        @MyBinding("baz")
        public int foobarbaz() {
            return 40;
        }

        public int nonannotated() {
            return 50;
        }
    }

    @Interceptor
    @MyBinding("foo")
    @MyBinding("bar")
    static class IncrementingInterceptor {
        static final AtomicBoolean AROUND_CONSTRUCT = new AtomicBoolean(false);
        static final AtomicBoolean POST_CONSTRUCT = new AtomicBoolean(false);
        static final AtomicBoolean PRE_DESTROY = new AtomicBoolean(false);

        @AroundConstruct
        public void aroundConstruct(InvocationContext ctx) throws Exception {
            AROUND_CONSTRUCT.set(true);
            ctx.proceed();
        }

        @PostConstruct
        public void postConstruct(InvocationContext ctx) {
            POST_CONSTRUCT.set(true);
        }

        @PreDestroy
        public void preDestroy(InvocationContext ctx) {
            PRE_DESTROY.set(true);
        }

        @AroundInvoke
        public Object intercept(InvocationContext ctx) throws Exception {
            return ((Integer) ctx.proceed()) + 1;
        }
    }
}
