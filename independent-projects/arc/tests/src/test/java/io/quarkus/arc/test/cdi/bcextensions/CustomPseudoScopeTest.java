package io.quarkus.arc.test.cdi.bcextensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Discovery;
import jakarta.enterprise.inject.build.compatible.spi.MetaAnnotations;
import jakarta.inject.Inject;
import jakarta.inject.Scope;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

public class CustomPseudoScopeTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SingletonBean.class, ApplicationScopedBean.class, RequestScopedBean.class, DependentBean.class,
                    PrototypeBean.class)
            .buildCompatibleExtensions(new MyExtension())
            .build();

    @Test
    public void test() {
        ArcContainer container = Arc.container();
        container.requestContext().activate();

        PrototypeBean prototypeBean = container.select(PrototypeBean.class).get();

        assertNotEquals(prototypeBean.getId(),
                container.select(PrototypeBean.class).get().getId());

        SingletonBean singletonBean = Arc.container().select(SingletonBean.class).get();
        assertEquals(singletonBean.getPrototypeId(),
                Arc.container().select(SingletonBean.class).get().getPrototypeId());
        assertNotEquals(prototypeBean.getId(), singletonBean.getPrototypeId());

        ApplicationScopedBean applicationScopedBean = Arc.container().select(ApplicationScopedBean.class).get();
        assertEquals(applicationScopedBean.getPrototypeId(),
                Arc.container().select(ApplicationScopedBean.class).get().getPrototypeId());
        assertNotEquals(prototypeBean.getId(), applicationScopedBean.getPrototypeId());

        RequestScopedBean requestScopedBean = Arc.container().select(RequestScopedBean.class).get();
        assertEquals(requestScopedBean.getPrototypeId(),
                Arc.container().select(RequestScopedBean.class).get().getPrototypeId());
        assertNotEquals(prototypeBean.getId(), requestScopedBean.getPrototypeId());

        DependentBean dependentBean = Arc.container().select(DependentBean.class).get();
        assertNotEquals(dependentBean.getPrototypeId(),
                Arc.container().select(DependentBean.class).get().getPrototypeId());
        assertNotEquals(prototypeBean.getId(), dependentBean.getPrototypeId());
    }

    @Singleton
    static class SingletonBean {
        @Inject
        PrototypeBean prototype;

        public String getPrototypeId() {
            return prototype.getId();
        }
    }

    @ApplicationScoped
    static class ApplicationScopedBean {
        @Inject
        PrototypeBean prototype;

        public String getPrototypeId() {
            return prototype.getId();
        }
    }

    @RequestScoped
    static class RequestScopedBean {
        @Inject
        PrototypeBean prototype;

        public String getPrototypeId() {
            return prototype.getId();
        }
    }

    @Dependent
    static class DependentBean {
        @Inject
        PrototypeBean prototype;

        public String getPrototypeId() {
            return prototype.getId();
        }
    }

    @Prototype
    static class PrototypeBean {
        private final String id = UUID.randomUUID().toString();

        public String getId() {
            return id;
        }
    }

    // ---

    public static class MyExtension implements BuildCompatibleExtension {
        @Discovery
        public void discovery(MetaAnnotations meta) {
            meta.addContext(Prototype.class, PrototypeContext.class);
        }
    }

    // ---

    /**
     * Specifies that a bean belongs to the <em>prototype</em> pseudo-scope.
     * <p>
     * When a bean is declared to have the {@code @Prototype} scope:
     * <ul>
     * <li>Each injection point or dynamic lookup receives a new instance; instances are never shared.</li>
     * <li>Lifecycle of instances is not managed by the CDI container.</li>
     * </ul>
     * <p>
     * Every invocation of the {@link Context#get(Contextual, CreationalContext)} operation on the
     * context object for the {@code @Prototype} scope returns a new instance of given bean.
     * <p>
     * Every invocation of the {@link Context#get(Contextual)} operation on the context object for the
     * {@code @Prototype} scope returns a {@code null} value.
     * <p>
     * The {@code @Prototype} scope is always active.
     */
    @Scope
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface Prototype {
    }

    public static class PrototypeContext implements AlterableContext {
        @Override
        public Class<? extends Annotation> getScope() {
            return Prototype.class;
        }

        @Override
        public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
            return creationalContext != null ? contextual.create(creationalContext) : null;
        }

        @Override
        public <T> T get(Contextual<T> contextual) {
            return null;
        }

        @Override
        public boolean isActive() {
            return true;
        }

        @Override
        public void destroy(Contextual<?> contextual) {
        }
    }
}
