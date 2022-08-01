package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MemberLevelInheritanceTest {

    @RegisterExtension
    ArcTestContainer testContainer = new ArcTestContainer(Foo.class, FooExtended.class, DummyBean.class,
            MyBinding.class, MyInterceptor.class);

    @Test
    public void testMemberLevelInheritance() {
        ArcContainer container = Arc.container();
        // IP inheritance
        FooExtended fooExtended = container.instance(FooExtended.class).get();
        assertNotNull(fooExtended);
        assertNotNull(fooExtended.getBar());

        // producer inheritance is tested simply by not getting ambiguous dependency while running the test

        // initializer inheritance
        assertNotNull(fooExtended.getBeanFromInitMethod());

        // interceptor binding on a method inheritance
        assertEquals(0, MyInterceptor.timesInvoked);
        fooExtended.interceptedMethod();
        assertEquals(1, MyInterceptor.timesInvoked);
    }

    @ApplicationScoped
    static class Foo {
        @Inject
        DummyBean bar;

        public DummyBean getBar() {
            return bar;
        }

        @Produces
        String string = "42";

        @Produces
        public Integer answerToLifeUniverseAndAll() {
            return 42;
        }

        DummyBean beanFromInitMethod = null;

        public DummyBean getBeanFromInitMethod() {
            return beanFromInitMethod;
        }

        @Inject
        public void initializerMethod(DummyBean bean) {
            this.beanFromInitMethod = bean;
        }

        @MyBinding
        public void interceptedMethod() {
        }
    }

    @ApplicationScoped
    @Typed(FooExtended.class)
    static class FooExtended extends Foo {
        // does inherit injected field
        // does not inherit producers
        // does inherit initializer
        // does inherit intercepted interceptedMethod
    }

    @ApplicationScoped
    static class DummyBean {

    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Inherited
    @InterceptorBinding
    @interface MyBinding {
    }

    @MyBinding
    @Interceptor
    @Priority(1)
    static class MyInterceptor {
        public static int timesInvoked = 0;

        @AroundInvoke
        Object aroundInvoke(InvocationContext ctx) throws Exception {
            timesInvoked++;
            return ctx.proceed();
        }
    }
}
