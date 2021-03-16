package io.quarkus.arc.test.beanmanager;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ManagedContext;
import io.quarkus.arc.test.ArcTestContainer;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.util.AnnotationLiteral;
import javax.enterprise.util.Nonbinding;
import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InterceptorBinding;
import javax.interceptor.InvocationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class BeanManagerTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(Legacy.class, AlternativeLegacy.class, Fool.class,
            DummyInterceptor.class, DummyBinding.class, LowPriorityInterceptor.class, WithInjectionPointMetadata.class,
            High.class, Observers.class);

    @Test
    public void testGetBeans() {
        BeanManager beanManager = Arc.container().instance(Legacy.class).get().getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(Legacy.class);
        assertEquals(2, beans.size());
        assertEquals(AlternativeLegacy.class, beanManager.resolve(beans).getBeanClass());
    }

    @Test
    public void testGetReference() {
        Fool.DESTROYED.set(false);
        Legacy.DESTROYED.set(false);

        BeanManager beanManager = Arc.container().instance(Legacy.class).get().getBeanManager();

        Set<Bean<?>> foolBeans = beanManager.getBeans(Fool.class);
        assertEquals(1, foolBeans.size());
        @SuppressWarnings("unchecked")
        Bean<Fool> foolBean = (Bean<Fool>) foolBeans.iterator().next();
        Fool fool1 = (Fool) beanManager.getReference(foolBean, Fool.class, beanManager.createCreationalContext(foolBean));

        ManagedContext requestContext = Arc.container().requestContext();
        requestContext.activate();
        assertEquals(fool1.getId(),
                ((Fool) beanManager.getReference(foolBean, Fool.class, beanManager.createCreationalContext(foolBean))).getId());
        requestContext.terminate();
        assertTrue(Fool.DESTROYED.get());

        Set<Bean<?>> legacyBeans = beanManager.getBeans(AlternativeLegacy.class);
        assertEquals(1, legacyBeans.size());
        @SuppressWarnings("unchecked")
        Bean<AlternativeLegacy> legacyBean = (Bean<AlternativeLegacy>) legacyBeans.iterator().next();
        CreationalContext<AlternativeLegacy> ctx = beanManager.createCreationalContext(legacyBean);
        Legacy legacy = (Legacy) beanManager.getReference(legacyBean, Legacy.class, ctx);
        assertNotNull(legacy.getBeanManager());
        ctx.release();
        assertTrue(Legacy.DESTROYED.get());
    }

    @Test
    public void testGetInjectableReference() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<Bean<?>> beans = beanManager.getBeans(WithInjectionPointMetadata.class);
        assertEquals(1, beans.size());
        @SuppressWarnings("unchecked")
        Bean<WithInjectionPointMetadata> bean = (Bean<WithInjectionPointMetadata>) beans.iterator().next();
        WithInjectionPointMetadata injectableReference = (WithInjectionPointMetadata) beanManager
                .getInjectableReference(new InjectionPoint() {

                    @Override
                    public boolean isTransient() {
                        return false;
                    }

                    @Override
                    public boolean isDelegate() {
                        return false;
                    }

                    @Override
                    public Type getType() {
                        return WithInjectionPointMetadata.class;
                    }

                    @Override
                    public Set<Annotation> getQualifiers() {
                        return Collections.singleton(Any.Literal.INSTANCE);
                    }

                    @Override
                    public Member getMember() {
                        return null;
                    }

                    @Override
                    public Bean<?> getBean() {
                        return null;
                    }

                    @Override
                    public Annotated getAnnotated() {
                        return null;
                    }
                }, beanManager.createCreationalContext(bean));
        assertNotNull(injectableReference.injectionPoint);
        assertEquals(WithInjectionPointMetadata.class, injectableReference.injectionPoint.getType());
        assertNull(injectableReference.injectionPoint.getBean());
    }

    @SuppressWarnings("serial")
    @Test
    public void testResolveInterceptors() {
        BeanManager beanManager = Arc.container().beanManager();
        List<javax.enterprise.inject.spi.Interceptor<?>> interceptors;
        // InterceptionType does not match
        interceptors = beanManager.resolveInterceptors(InterceptionType.AROUND_CONSTRUCT, new DummyBinding.Literal(true, true));
        assertTrue(interceptors.isEmpty());
        // alpha is @Nonbinding
        interceptors = beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE, new DummyBinding.Literal(false, true),
                new AnnotationLiteral<UselessBinding>() {
                });
        assertEquals(2, interceptors.size());
        assertEquals(DummyInterceptor.class, interceptors.get(0).getBeanClass());
        assertEquals(LowPriorityInterceptor.class, interceptors.get(1).getBeanClass());
    }

    @Test
    public void testIsQualifier() {
        BeanManager beanManager = Arc.container().beanManager();
        assertTrue(beanManager.isQualifier(Default.class));
        assertTrue(beanManager.isQualifier(High.class));
        assertFalse(beanManager.isQualifier(ApplicationScoped.class));
    }

    @Test
    public void testIsInterceptorBinding() {
        BeanManager beanManager = Arc.container().beanManager();
        assertTrue(beanManager.isInterceptorBinding(DummyBinding.class));
        assertFalse(beanManager.isInterceptorBinding(Default.class));
    }

    @Test
    public void testIsScope() {
        BeanManager beanManager = Arc.container().beanManager();
        assertTrue(beanManager.isScope(Singleton.class));
        assertTrue(beanManager.isNormalScope(RequestScoped.class));
        assertFalse(beanManager.isNormalScope(Dependent.class));
    }

    @Test
    public void testResolveObservers() {
        BeanManager beanManager = Arc.container().beanManager();
        Set<ObserverMethod<? super Long>> observers = beanManager.resolveObserverMethods(Long.valueOf(1),
                new AnnotationLiteral<High>() {
                });
        assertEquals(1, observers.size());
        assertEquals(Number.class, observers.iterator().next().getObservedType());
    }

    @ApplicationScoped
    static class Observers {

        void observe(@Observes @High Number number) {
        }
    }

    @Target({ TYPE, METHOD, PARAMETER, FIELD })
    @Retention(RUNTIME)
    @Documented
    @Qualifier
    public @interface High {

    }

    @Dependent
    static class Legacy {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        @Inject
        BeanManager beanManager;

        public BeanManager getBeanManager() {
            return beanManager;
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

    }

    @Priority(1)
    @Alternative
    @Dependent
    static class AlternativeLegacy extends Legacy {

    }

    @RequestScoped
    static class Fool {

        static final AtomicBoolean DESTROYED = new AtomicBoolean();

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }

        String getId() {
            return id;
        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface DummyBinding {

        @Nonbinding
        boolean alpha();

        boolean bravo();

        @SuppressWarnings("serial")
        static class Literal extends AnnotationLiteral<DummyBinding> implements DummyBinding {

            private final boolean alpha;
            private final boolean bravo;

            public Literal(boolean alpha, boolean bravo) {
                this.alpha = alpha;
                this.bravo = bravo;
            }

            @Override
            public boolean alpha() {
                return alpha;
            }

            @Override
            public boolean bravo() {
                return bravo;
            }

        }

    }

    @Target({ TYPE, METHOD })
    @Retention(RUNTIME)
    @Documented
    @InterceptorBinding
    public @interface UselessBinding {

    }

    @DummyBinding(alpha = true, bravo = true)
    @Priority(10)
    @Interceptor
    static class DummyInterceptor {

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    @DummyBinding(alpha = true, bravo = true)
    @Priority(1)
    @Interceptor
    static class LowPriorityInterceptor {

        @AroundInvoke
        Object intercept(InvocationContext ctx) throws Exception {
            return ctx.proceed();
        }
    }

    @Dependent
    static class WithInjectionPointMetadata {

        @Inject
        InjectionPoint injectionPoint;

    }

}
