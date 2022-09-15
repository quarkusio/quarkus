package io.quarkus.arc.test.buildextension.interceptor.bindings;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AdditionalInterceptorBindingsTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SomeBean.class, SomeOtherBean.class,
                    MyInterceptor.class, ToBeBinding.class,
                    ToBeBindingWithBindingField.class, MyInterceptorForBindingField.class,
                    ToBeBindingWithNonBindingField.class, MyInterceptorForNonBindingField.class)
            .interceptorBindingRegistrars(new MyBindingRegistrar())
            .build();

    @Test
    public void testBindingWasRegistered() {
        MyInterceptor.INTERCEPTOR_TRIGGERED = false;
        assertAfterCall(SomeBean.class, () -> MyInterceptor.INTERCEPTOR_TRIGGERED, true);
    }

    @Test
    public void testBindingWasRegisteredWithNonBindingField() {
        MyInterceptorForNonBindingField.INTERCEPTOR_TRIGGERED = false;
        assertAfterCall(SomeBean.class, () -> MyInterceptorForNonBindingField.INTERCEPTOR_TRIGGERED, true);
    }

    @Test
    public void testBindingWasNotRegisteredWithMismatchingBindingField() {
        MyInterceptorForBindingField.INTERCEPTOR_TRIGGERED = false;
        assertAfterCall(SomeBean.class, () -> MyInterceptorForBindingField.INTERCEPTOR_TRIGGERED, false);
    }

    @Test
    public void testBindingWasRegisteredWithMatchingBindingField() {
        MyInterceptorForBindingField.INTERCEPTOR_TRIGGERED = false;
        assertAfterCall(SomeOtherBean.class, () -> MyInterceptorForBindingField.INTERCEPTOR_TRIGGERED, true);
    }

    private void assertAfterCall(Class<? extends Pingable> beanClass, Supplier<Boolean> check, boolean expected) {
        Assertions.assertTrue(Arc.container().instance(beanClass).isAvailable());
        Assertions.assertFalse(check.get());
        Arc.container().instance(beanClass).get().ping();
        Assertions.assertEquals(expected, check.get());
    }

    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface ToBeBinding {

    }

    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface ToBeBindingWithNonBindingField {
        String[] value();
    }

    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface ToBeBindingWithBindingField {
        String[] value();
    }

    static class MyBindingRegistrar implements InterceptorBindingRegistrar {

        @Override
        public List<InterceptorBinding> getAdditionalBindings() {
            return List.of(
                    InterceptorBinding.of(ToBeBinding.class),
                    InterceptorBinding.of(ToBeBindingWithNonBindingField.class, Collections.singleton("value")),
                    InterceptorBinding.of(ToBeBindingWithBindingField.class));
        }
    }

    @Interceptor
    @Priority(1)
    @ToBeBinding
    static class MyInterceptor {

        public static boolean INTERCEPTOR_TRIGGERED = false;

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            INTERCEPTOR_TRIGGERED = true;
            return context.proceed();
        }

    }

    @Interceptor
    @Priority(1)
    @ToBeBindingWithBindingField("notIgnored")
    static class MyInterceptorForBindingField {

        public static boolean INTERCEPTOR_TRIGGERED = false;

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            INTERCEPTOR_TRIGGERED = true;
            return context.proceed();
        }

    }

    @Interceptor
    @Priority(1)
    @ToBeBindingWithNonBindingField("ignored")
    static class MyInterceptorForNonBindingField {

        public static boolean INTERCEPTOR_TRIGGERED = false;

        @AroundInvoke
        public Object invoke(InvocationContext context) throws Exception {
            INTERCEPTOR_TRIGGERED = true;
            return context.proceed();
        }

    }

    @ApplicationScoped
    @ToBeBinding
    @ToBeBindingWithNonBindingField("toBeIgnored")
    @ToBeBindingWithBindingField("notIgnored-mismatched")
    static class SomeBean implements Pingable {
        public void ping() {

        }
    }

    @ApplicationScoped
    @ToBeBindingWithBindingField("notIgnored")
    static class SomeOtherBean implements Pingable {
        public void ping() {

        }
    }

    interface Pingable {
        void ping();
    }
}
