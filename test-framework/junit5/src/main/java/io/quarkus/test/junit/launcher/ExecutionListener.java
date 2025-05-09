package io.quarkus.test.junit.launcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

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

    private Deque<ClassLoader> origCl = new ArrayDeque<>();

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // This will be called for various levels of containers, only some of which are tests, so check carefully and do not assume
        Optional<TestSource> oSource = testIdentifier.getSource();
        if (oSource.isPresent()) {
            TestSource source = oSource.get();
            if (source instanceof ClassSource cs) {
                ClassLoader classLoader = cs.getJavaClass().getClassLoader();
                // Only adjust the TCCL in cases where we know the QuarkusTestExtension would be about to do it anyway
                // We could check annotations, but that would be slow, and the assumption that only Quarkus Tests are loaded with the quarkus classloader should be a fair one
                if (isQuarkusTest(classLoader)) {
                    origCl.push(Thread.currentThread().getContextClassLoader());
                    Thread.currentThread().setContextClassLoader(classLoader);
                } else {
                    origCl = null;
                }
            }
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        Optional<TestSource> oSource = testIdentifier.getSource();
        if (oSource.isPresent()) {
            TestSource source = oSource.get();
            if (source instanceof ClassSource cs) {
                ClassLoader classLoader = cs.getJavaClass().getClassLoader();
                // Only pop if we meet the conditions under which we pushed
                if (isQuarkusTest(classLoader)) {
                    ClassLoader cl = origCl.pop();
                    if (cl != null) {
                        // If execution is parallel this could produce odd results, but if execution is parallel any kind of TCCL manipulation will be ill-fated
                        Thread.currentThread().setContextClassLoader(cl);
                    }
                }
            }
        }
    }

    private static boolean isQuarkusTest(ClassLoader classLoader) {
        return classLoader instanceof QuarkusClassLoader;
    }
}