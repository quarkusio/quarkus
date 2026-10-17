package io.quarkus.arc.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

class AutoAddScopeMissingSuperclassTest {

    static class Parent {
    }

    static class Child extends Parent {
    }

    @Test
    void missingSuperclassFailsFastWhenMatchingInterface() throws IOException {
        ClassInfo child = Index.of(Child.class).getClassByName(Child.class);
        AtomicInteger lookups = new AtomicInteger();
        IndexView index = missingSuperclassIndex(child.superName(), lookups);

        AutoAddScopeBuildItem item = AutoAddScopeBuildItem.builder()
                .implementsInterface(DotName.createSimple(Runnable.class.getName()))
                .build();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> item.test(child, List.of(), index));

        assertTrue(failure.getMessage().contains(child.superName().toString()));
        assertTrue(failure.getMessage().contains("Jandex index"));
        assertTrue(failure.getMessage().contains("application setup error"));
        assertEquals(1, lookups.get());
    }

    @Test
    void missingSuperclassFailsFastWhenCheckingContainerServices() throws IOException {
        ClassInfo child = Index.of(Child.class).getClassByName(Child.class);
        AtomicInteger lookups = new AtomicInteger();
        IndexView index = missingSuperclassIndex(child.superName(), lookups);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new AutoAddScopeProcessor().requiresContainerServices(child, Set.of(), index));

        assertTrue(failure.getMessage().contains(child.superName().toString()));
        assertTrue(failure.getMessage().contains("Jandex index"));
        assertTrue(failure.getMessage().contains("application setup error"));
        assertEquals(1, lookups.get());
    }

    @Test
    void completeHierarchyCanBeTraversedWithoutContainerServices() throws IOException {
        Index index = Index.of(Child.class, Parent.class);
        ClassInfo child = index.getClassByName(Child.class);

        assertFalse(new AutoAddScopeProcessor().requiresContainerServices(child, Set.of(), index));
    }

    private static IndexView missingSuperclassIndex(DotName superName, AtomicInteger lookups) {
        return (IndexView) Proxy.newProxyInstance(
                IndexView.class.getClassLoader(),
                new Class<?>[] { IndexView.class },
                (proxy, method, args) -> {
                    if ("getClassByName".equals(method.getName()) && superName.equals(args[0])) {
                        if (lookups.incrementAndGet() > 1) {
                            throw new AssertionError("Missing superclass queried repeatedly");
                        }
                        return null;
                    }
                    throw new AssertionError("Unexpected index operation: " + method);
                });
    }
}
