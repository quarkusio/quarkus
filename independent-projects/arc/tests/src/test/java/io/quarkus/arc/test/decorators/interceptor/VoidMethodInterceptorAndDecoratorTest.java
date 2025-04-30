package io.quarkus.arc.test.decorators.interceptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class VoidMethodInterceptorAndDecoratorTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Performer.class, MainPerformer.class,
            PerformerDecorator.class, MyInterceptorBinding.class, MyInterceptor.class);

    @Test
    public void testDecoration() {
        MainPerformer performer = Arc.container().instance(MainPerformer.class).get();

        assertFalse(MainPerformer.DONE.get());
        assertFalse(PerformerDecorator.DONE.get());
        assertFalse(MyInterceptor.INTERCEPTED.get());

        performer.doSomething();

        assertTrue(MainPerformer.DONE.get());
        assertTrue(PerformerDecorator.DONE.get());
        assertTrue(MyInterceptor.INTERCEPTED.get());
    }

    interface Performer {
        void doSomething();
    }

    @ApplicationScoped
    @MyInterceptorBinding
    static class MainPerformer implements Performer {
        static final AtomicBoolean DONE = new AtomicBoolean();

        @Override
        public void doSomething() {
            DONE.set(true);
        }
    }

    @Dependent
    @Priority(1)
    @Decorator
    static class PerformerDecorator implements Performer {
        static final AtomicBoolean DONE = new AtomicBoolean();

        @Inject
        @Delegate
        Performer delegate;

        @Override
        public void doSomething() {
            DONE.set(true);
            delegate.doSomething();
        }
    }

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface MyInterceptorBinding {
    }

    @MyInterceptorBinding
    @Priority(1)
    @Interceptor
    static class MyInterceptor {
        static final AtomicBoolean INTERCEPTED = new AtomicBoolean();

        @AroundInvoke
        Object log(InvocationContext ctx) throws Exception {
            INTERCEPTED.set(true);
            return ctx.proceed();
        }
    }
}
