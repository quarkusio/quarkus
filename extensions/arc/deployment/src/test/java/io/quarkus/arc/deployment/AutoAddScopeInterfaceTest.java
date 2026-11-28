package io.quarkus.arc.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Test;

class AutoAddScopeInterfaceTest {

    static class RunnableParent implements Runnable {

        @Override
        public void run() {
        }
    }

    static class RunnableChild extends RunnableParent {
    }

    static class DirectRunnable implements Runnable {

        @Override
        public void run() {
        }
    }

    static class PlainParent {
    }

    static class PlainChild extends PlainParent {
    }

    private static AutoAddScopeBuildItem runnableMatcher() {
        return AutoAddScopeBuildItem.builder()
                .implementsInterface(DotName.createSimple(Runnable.class.getName()))
                .build();
    }

    @Test
    void directInterfaceImplementationStillMatches() throws IOException {
        Index index = Index.of(DirectRunnable.class);
        ClassInfo clazz = index.getClassByName(DirectRunnable.class);

        assertTrue(runnableMatcher().test(clazz, List.of(), index));
    }

    @Test
    void inheritedInterfaceImplementationStillMatches() throws IOException {
        Index index = Index.of(RunnableChild.class, RunnableParent.class);
        ClassInfo clazz = index.getClassByName(RunnableChild.class);

        assertTrue(runnableMatcher().test(clazz, List.of(), index));
    }

    @Test
    void completeHierarchyWithoutInterfaceDoesNotMatch() throws IOException {
        Index index = Index.of(PlainChild.class, PlainParent.class);
        ClassInfo clazz = index.getClassByName(PlainChild.class);

        assertFalse(runnableMatcher().test(clazz, List.of(), index));
    }
}
