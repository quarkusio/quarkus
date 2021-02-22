package io.quarkus.arc.test.buildextension.interceptor.bindings;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.DotName;
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
        public Map<DotName, Set<String>> registerAdditionalBindings() {
            Map<DotName, Set<String>> newBindings = new HashMap<>();
            newBindings.put(DotName.createSimple(ToBeBinding.class.getName()), Collections.emptySet());
            HashSet<String> value = new HashSet<>();
            value.add("value");
            newBindings.put(DotName.createSimple(ToBeBindingWithNonBindingField.class.getName()), value);
            newBindings.put(DotName.createSimple(ToBeBindingWithBindingField.class.getName()), Collections.emptySet());
            return newBindings;
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
