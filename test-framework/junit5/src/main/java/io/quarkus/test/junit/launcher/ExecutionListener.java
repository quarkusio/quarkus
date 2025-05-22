package io.quarkus.test.junit.launcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * The earliest hook a test extension can have is BeforeAllCallback.
 * Since we don't know if other extensions might be registered, we want to get in before that callback and set the TCCL to be
 * the classloader of the test class.
 */
public class ExecutionListener implements TestExecutionListener {

    private final Deque<ClassLoader> origCl = new ArrayDeque<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        invokeIfTestIsQuarkusTest(testIdentifier, this::setTCCL);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        invokeIfTestIsQuarkusTest(testIdentifier, this::unsetTCCL);
    }

    private void invokeIfTestIsQuarkusTest(TestIdentifier testIdentifier, Consumer<ClassLoader> consumer) {
        // This will be called for various levels of containers, only some of which are tests, so check carefully and do not assume
        Optional<TestSource> oSource = testIdentifier.getSource();
        if (oSource.isPresent()) {
            TestSource source = oSource.get();
            if (source instanceof ClassSource cs) {
                ClassLoader classLoader = cs.getJavaClass().getClassLoader();
                // Only adjust the TCCL in cases where we know the QuarkusTestExtension would be about to do it anyway
                if (isQuarkusTest(classLoader)) {
                    consumer.accept(classLoader);
                }
            }
        }
    }

    private static boolean isQuarkusTest(ClassLoader classLoader) {
        // We could check annotations, but that would be slow, and the assumption that only Quarkus Tests are loaded with the quarkus classloader should be a fair one
        return classLoader instanceof QuarkusClassLoader;
    }

    private void setTCCL(ClassLoader classLoader) {
        origCl.push(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    private void unsetTCCL(ClassLoader classLoader) {
        ClassLoader cl = origCl.pop();
        // If execution is parallel this stack logic could produce odd results, but if execution is parallel any kind of TCCL manipulation will be ill-fated
        Thread.currentThread().setContextClassLoader(cl);
    }

}