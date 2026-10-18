package io.quarkus.arc.test.instance.destroy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.test.ArcTestContainer;

public class InstanceHandleDestroyTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer.Builder()
            .beanClasses(MyDependentBean.class, MyAppScopedBean.class)
            .strictCompatibility(true)
            .build();

    @Test
    public void testDestroy() {
        assertFalse(MyDependentBean.DESTROYED.get());
        try (InstanceHandle<MyDependentBean> handle = Arc.container().instance(MyDependentBean.class)) {
            assertNotNull(handle.get().toString());
        }
        assertTrue(MyDependentBean.DESTROYED.get());

        // normal-scoped
        String oldId;
        assertFalse(MyAppScopedBean.DESTROYED.get());
        try (InstanceHandle<MyAppScopedBean> handle = Arc.container().instance(MyAppScopedBean.class)) {
            assertNotNull(handle.get().toString());
            oldId = handle.get().getId();
        }
        assertTrue(MyAppScopedBean.DESTROYED.get());

        String newId = Arc.container().instance(MyAppScopedBean.class).get().getId();
        assertNotEquals(oldId, newId);
    }

    @Dependent
    static class MyDependentBean {
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }

    @ApplicationScoped
    static class MyAppScopedBean {
        static final AtomicBoolean DESTROYED = new AtomicBoolean(false);

        String id;

        String getId() {
            return id;
        }

        @PostConstruct
        void init() {
            this.id = UUID.randomUUID().toString();
        }

        @PreDestroy
        void destroy() {
            DESTROYED.set(true);
        }
    }
}
