package io.quarkus.test.junit.launcher;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public final class SelectedTestsIndex {

    private static final Set<String> SELECTED_CLASSES = ConcurrentHashMap.newKeySet();
    private static volatile boolean initialized;

    private SelectedTestsIndex() {
    }

    public static void initialize(TestPlan testPlan) {
        initialized = true;
        SELECTED_CLASSES.clear();
        for (TestIdentifier root : testPlan.getRoots()) {
            visit(testPlan, root);
        }
    }

    public static void clear() {
        initialized = false;
        SELECTED_CLASSES.clear();
    }

    public static boolean shouldStart(Class<?> testClass) {
        if (!initialized) {
            return true;
        }
        Class<?> current = testClass;
        while ((current != null) && (current != Object.class)) {
            if (SELECTED_CLASSES.contains(current.getName())) {
                return true;
            }
            current = current.getEnclosingClass();
        }
        return false;
    }

    private static void visit(TestPlan testPlan, TestIdentifier identifier) {
        identifier.getSource().ifPresent(SelectedTestsIndex::record);
        for (TestIdentifier child : testPlan.getChildren(identifier)) {
            visit(testPlan, child);
        }
    }

    private static void record(TestSource source) {
        if (source instanceof ClassSource classSource) {
            SELECTED_CLASSES.add(classSource.getClassName());
        } else if (source instanceof MethodSource methodSource) {
            SELECTED_CLASSES.add(methodSource.getClassName());
        }
    }
}
