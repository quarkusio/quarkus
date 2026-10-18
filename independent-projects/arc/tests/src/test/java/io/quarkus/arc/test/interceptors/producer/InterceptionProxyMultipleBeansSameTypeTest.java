package io.quarkus.arc.test.interceptors.producer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.BindingsSource;
import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.InterceptionProxySubclass;
import io.quarkus.arc.test.ArcTestContainer;

public class InterceptionProxyMultipleBeansSameTypeTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(MyBinding.class, MyInterceptor.class, MyQualifier.class,
            MyProducer.class, MyNonBeanBindings.class);

    @Test
    public void test() {
        ArcContainer container = Arc.container();
        MyNonbean nonbeanA = container.select(MyNonbean.class, new MyQualifier.Literal("A")).get();
        assertNonbean(nonbeanA, "intercepted: A");
        MyNonbean nonbeanB = container.select(MyNonbean.class, new MyQualifier.Literal("B")).get();
        assertNonbean(nonbeanB, "B");
    }

    private void assertNonbean(MyNonbean nonbean, String expected) {
        assertEquals(expected, nonbean.hello());

        assertInstanceOf(ClientProxy.class, nonbean);
        MyNonbean unwrappedProxy = ClientProxy.unwrap(nonbean);
        assertNotNull(unwrappedProxy);
        assertNotSame(nonbean, unwrappedProxy);

        assertInstanceOf(InterceptionProxySubclass.class, unwrappedProxy);
        assertNotNull(InterceptionProxySubclass.unwrap(unwrappedProxy));
        assertNotSame(unwrappedProxy, InterceptionProxySubclass.unwrap(unwrappedProxy));
        assertNotSame(nonbean, InterceptionProxySubclass.unwrap(unwrappedProxy));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface MyQualifier {

        String value();

        public final static class Literal extends AnnotationLiteral<MyQualifier> implements MyQualifier {

            private final String value;

            Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }

        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding {
    }

    @MyBinding
    @Priority(1)
    @Interceptor
    static class MyInterceptor {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted: " + ctx.proceed();
        }
    }

    static class MyNonbean {

        private final String msg;

        MyNonbean() {
            this.msg = null;
        }

        MyNonbean(String msg) {
            this.msg = msg;
        }

        String hello() {
            return msg;
        }
    }

    static abstract class MyNonBeanBindings {

        @MyBinding
        abstract String hello();

    }

    @Dependent
    static class MyProducer {

        @Produces
        @MyQualifier("A")
        @ApplicationScoped
        MyNonbean produceA(@BindingsSource(MyNonBeanBindings.class) InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean("A"));
        }

        @Produces
        @MyQualifier("B")
        @ApplicationScoped
        MyNonbean produceB(InterceptionProxy<MyNonbean> proxy) {
            return proxy.create(new MyNonbean("B"));
        }
    }
}
