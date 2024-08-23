package io.quarkus.arc.test.buildextension.beans;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.inject.Intercepted;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;
import jakarta.interceptor.InterceptorBinding;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.InterceptorCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInterceptorTest {

    static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyBean.class, SimpleBinding.class, TestAroundInvoke.class)
            .beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testSyntheticInterceptor() {
        EVENTS.clear();
        InstanceHandle<MyBean> handle = Arc.container().instance(MyBean.class);
        assertEquals("ok", handle.get().ping());
        handle.destroy();
        assertEquals(5, EVENTS.size());
        assertEquals(TestAroundConstruct.class.getName(), EVENTS.get(0));
        assertEquals(TestPostConstruct.class.getName(), EVENTS.get(1));
        assertEquals(TestPostConstruct.class.getName(), EVENTS.get(2));
        assertEquals(TestAroundInvoke.class.getName(), EVENTS.get(3));
        assertEquals(TestPreDestroy.class.getName(), EVENTS.get(4));
    }

    @SimpleBinding
    @Singleton
    static class MyBean {

        String ping() {
            return "true";
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @InterceptorBinding
    public @interface SimpleBinding {

    }

    static class TestAroundInvoke implements InterceptorCreator {

        @Override
        public InterceptFunction create(SyntheticCreationalContext<Object> context) {
            assertInterceptedBean(context);
            return ic -> {
                EVENTS.add(TestAroundInvoke.class.getName());
                return Boolean.parseBoolean(ic.proceed().toString()) ? "ok" : "nok";
            };
        }

    }

    static class TestPostConstruct implements InterceptorCreator {

        @Override
        public InterceptFunction create(SyntheticCreationalContext<Object> context) {
            assertInterceptedBean(context);
            return ic -> {
                EVENTS.add(TestPostConstruct.class.getName());
                return ic.proceed();
            };
        }

    }

    static class TestPreDestroy implements InterceptorCreator {

        @Override
        public InterceptFunction create(SyntheticCreationalContext<Object> context) {
            assertInterceptedBean(context);
            return ic -> {
                EVENTS.add(TestPreDestroy.class.getName());
                return ic.proceed();
            };
        }

    }

    static class TestAroundConstruct implements InterceptorCreator {

        @Override
        public InterceptFunction create(SyntheticCreationalContext<Object> context) {
            assertInterceptedBean(context);
            return ic -> {
                EVENTS.add(TestAroundConstruct.class.getName());
                return ic.proceed();
            };
        }

    }

    static void assertInterceptedBean(SyntheticCreationalContext<Object> context) {
        @SuppressWarnings("serial")
        Bean<?> bean = context.getInjectedReference(new TypeLiteral<Bean<?>>() {
        }, InterceptedLiteral.INSTANCE);
        assertNotNull(bean);
        assertEquals(Singleton.class, bean.getScope());
    }

    static final class InterceptedLiteral extends AnnotationLiteral<Intercepted> implements Intercepted {

        public static final InterceptedLiteral INSTANCE = new InterceptedLiteral();

        private static final long serialVersionUID = 1L;

    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configureInterceptor(InterceptionType.AROUND_INVOKE)
                    .bindings(AnnotationInstance.builder(SimpleBinding.class).build())
                    .addInjectionPoint(ParameterizedType.create(Bean.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(Intercepted.class).build())
                    .creator(TestAroundInvoke.class);

            context.configureInterceptor(InterceptionType.POST_CONSTRUCT)
                    .bindings(AnnotationInstance.builder(SimpleBinding.class).build())
                    .addInjectionPoint(ParameterizedType.create(Bean.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(Intercepted.class).build())
                    .creator(TestPostConstruct.class);

            context.configureInterceptor(InterceptionType.POST_CONSTRUCT)
                    .bindings(AnnotationInstance.builder(SimpleBinding.class).build())
                    .addInjectionPoint(ParameterizedType.create(Bean.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(Intercepted.class).build())
                    .identifier("foo")
                    .creator(TestPostConstruct.class);

            context.configureInterceptor(InterceptionType.PRE_DESTROY)
                    .bindings(AnnotationInstance.builder(SimpleBinding.class).build())
                    .addInjectionPoint(ParameterizedType.create(Bean.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(Intercepted.class).build())
                    .creator(TestPreDestroy.class);

            context.configureInterceptor(InterceptionType.AROUND_CONSTRUCT)
                    .bindings(AnnotationInstance.builder(SimpleBinding.class).build())
                    .addInjectionPoint(ParameterizedType.create(Bean.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(Intercepted.class).build())
                    .creator(TestAroundConstruct.class);
        }

    }

}
