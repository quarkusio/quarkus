package io.quarkus.arc.test.interceptors.synthbean;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.annotation.Priority;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InterceptorBinding;
import jakarta.interceptor.InvocationContext;

import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.InterceptionProxy;
import io.quarkus.arc.NoClassInterceptors;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SynthBeanWithGenericClassInterceptionAndBindingsSourceTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBinding1.class, MyInterceptor1.class, MyBinding2.class, MyInterceptor2.class)
            .beanRegistrars(new BeanRegistrar() {
                @Override
                public void register(RegistrationContext context) {
                    ParameterizedType type = ParameterizedType.builder(MyNonbean.class)
                            .addArgument(String.class)
                            .build();

                    context.configure(MyNonbean.class)
                            .types(type)
                            .providerType(type)
                            .injectInterceptionProxy(DotName.createSimple(MyNonbeanBindings.class))
                            .creator(MyNonbeanCreator.class)
                            .done();

                }
            })
            .build();

    static class MyNonbeanCreator implements BeanCreator<MyNonbean<String>> {
        @Override
        public MyNonbean<String> create(SyntheticCreationalContext<MyNonbean<String>> context) {
            InterceptionProxy<MyNonbean<String>> proxy = context.getInterceptionProxy();
            return proxy.create(new MyNonbean<>());
        }
    }

    @Test
    public void test() {
        MyNonbean<String> nonbean = Arc.container().instance(new TypeLiteral<MyNonbean<String>>() {
        }).get();
        assertEquals("intercepted1: hello1", nonbean.hello1("hello1"));
        assertEquals("intercepted1: intercepted2: hello2", nonbean.hello2("hello2"));
        assertEquals("intercepted2: hello3", nonbean.hello3("hello3"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding1 {
    }

    @MyBinding1
    @Priority(1)
    @Interceptor
    static class MyInterceptor1 {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted1: " + ctx.proceed();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR })
    @InterceptorBinding
    @interface MyBinding2 {
    }

    @MyBinding2
    @Priority(2)
    @Interceptor
    static class MyInterceptor2 {
        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return "intercepted2: " + ctx.proceed();
        }
    }

    static class MyNonbean<T> {
        public T hello1(T param) {
            return param;
        }

        public T hello2(T param) {
            return param;
        }

        public T hello3(T param) {
            return param;
        }
    }

    @MyBinding1
    static abstract class MyNonbeanBindings<T> {
        @MyBinding2
        abstract T hello2(T param);

        @MyBinding2
        @NoClassInterceptors
        abstract T hello3(T param);
    }
}
