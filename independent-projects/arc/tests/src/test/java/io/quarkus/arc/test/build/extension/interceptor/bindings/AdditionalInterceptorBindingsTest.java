package io.quarkus.arc.test.build.extension.interceptor.bindings;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jboss.jandex.DotName;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class AdditionalInterceptorBindingsTest {

    @Rule
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SomeBean.class, MyInterceptor.class, ToBeBinding.class)
            .interceptorBindingRegistrars(new MyBindingRegistrar())
            .build();

    @Test
    public void testBindingWasRegistered() {
        Assert.assertTrue(Arc.container().instance(SomeBean.class).isAvailable());
        Assert.assertFalse(MyInterceptor.INTERCEPTOR_TRIGGERED);
        Arc.container().instance(SomeBean.class).get().ping();
        Assert.assertTrue(MyInterceptor.INTERCEPTOR_TRIGGERED);
    }

    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface ToBeBinding {
    }

    static class MyBindingRegistrar implements InterceptorBindingRegistrar {

        @Override
        public Collection<DotName> registerAdditionalBindings() {
            List<DotName> newBindings = new ArrayList<>();
            newBindings.add(DotName.createSimple(ToBeBinding.class.getName()));
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

    @ApplicationScoped
    @ToBeBinding
    static class SomeBean {
        public void ping() {

        }
    }
}
