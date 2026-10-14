package io.quarkus.arc.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
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
    void missingSuperclassMustNotBeLookedUpRepeatedly() throws IOException {
        // Only Child is indexed; its superclass is deliberately absent.
        Index index = Index.of(Child.class);
        ClassInfo child = index.getClassByName(Child.class);

        AtomicInteger lookups = new AtomicInteger();

        IndexView guardedIndex = (IndexView) Proxy.newProxyInstance(
                IndexView.class.getClassLoader(),
                new Class<?>[] {IndexView.class},
                (proxy, method, args) -> {
                    if ("getClassByName".equals(method.getName())) {
                        int count = lookups.incrementAndGet();

                        if (count > 1) {
                            throw new AssertionError(
                                    "ISSUE 57311: missing superclass queried repeatedly");
                        }

                        return null;
                    }

                    throw new AssertionError("Unexpected index operation: " + method);
                });

        AutoAddScopeBuildItem item = AutoAddScopeBuildItem.builder()
                .implementsInterface(DotName.createSimple(Runnable.class.getName()))
                .build();

        // No implementation of Runnable exists in this class hierarchy.
        assertFalse(item.test(child, List.of(), guardedIndex));
        assertEquals(1, lookups.get());
    }
}
