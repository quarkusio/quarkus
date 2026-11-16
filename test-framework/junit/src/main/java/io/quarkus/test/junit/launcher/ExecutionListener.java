package io.quarkus.test.junit.launcher;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.logging.Logger;
import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

/**
 * Sets the thread context classloader (TCCL) to the classloader of a Quarkus test while JUnit executes it, and restores
 * the previous TCCL when the execution finishes.
 * <p>
 * The earliest hook a test extension can have is BeforeAllCallback.
 * Since we don't know if other extensions might be registered, we want to get in before that callback and set the TCCL to be
 * the classloader of the test class.
 * <p>
 * The TCCL is switched for the start and finish of nodes whose source is a test class or a test method, and only when
 * that class was loaded by a {@link QuarkusClassLoader}. Test methods are included because JUnit may run them on
 * worker threads that never saw the event of their class, and such threads keep whatever TCCL they were created with.
 * <p>
 * The original TCCLs are remembered in a stack per thread, so nested nodes (a class, then one of its methods, or a
 * nested class) restore in reverse order, and a thread never restores a classloader saved by another thread. With the
 * Jupiter engine the start and the finish of a node are reported on the same thread, so no state is shared between
 * threads.
 * <p>
 * This class makes the TCCL handling thread safe. It does not make running Quarkus tests concurrently safe: the Quarkus
 * test extensions still keep static state, such as the running application.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/52994">#52994</a>
 */
public class ExecutionListener implements TestExecutionListener {

    private static final Logger log = Logger.getLogger(ExecutionListener.class);

    /**
     * The TCCLs the current thread had before each test node it started, most recent first. Entries are removed when
     * the node finishes, and the thread local is cleared when the stack is empty so that pooled threads do not keep
     * any state.
     */
    private final ThreadLocal<Deque<SavedTccl>> origCl = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * The TCCL to restore when a test node finishes.
     *
     * @param testId the unique id of the node that switched the TCCL, used to restore only for that same node
     * @param original the TCCL of the thread before the node started, which may be null. It is held in a record
     *        because {@link ArrayDeque} does not accept null elements
     */
    private record SavedTccl(String testId, ClassLoader original) {
    }

    /**
     * Switches the TCCL of the current thread to the classloader of the test class when the started node is a Quarkus
     * test class or method, remembering the previous TCCL. Other nodes are ignored.
     */
    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        invokeIfTestIsQuarkusTest(testIdentifier, classLoader -> setTCCL(testIdentifier, classLoader));
    }

    /**
     * Restores the TCCL that the current thread had when the finished node started. Nothing is restored if the latest
     * node started on this thread is not the one that finished, for instance because it started on another thread, so
     * that a thread never ends up with a classloader that belongs to a different test.
     */
    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        invokeIfTestIsQuarkusTest(testIdentifier, classLoader -> unsetTCCL(testIdentifier));
    }

    /**
     * Runs the consumer only if the node is a test class or method loaded by a {@link QuarkusClassLoader}.
     *
     * @param testIdentifier the node JUnit is starting or has finished
     * @param consumer the action to run, which receives the classloader of the test class
     */
    private void invokeIfTestIsQuarkusTest(TestIdentifier testIdentifier, Consumer<ClassLoader> consumer) {
        // This will be called for various levels of containers, only some of which are tests, so check carefully and do not assume
        Optional<TestSource> oSource = testIdentifier.getSource();
        if (oSource.isPresent()) {
            Class<?> testClass = testClassOf(oSource.get());
            if (testClass != null) {
                ClassLoader classLoader = testClass.getClassLoader();
                // Only adjust the TCCL in cases where we know the QuarkusTestExtension would be about to do it anyway
                if (isQuarkusTest(classLoader)) {
                    consumer.accept(classLoader);
                }
            }
        }
    }

    /**
     * Finds the test class a source refers to.
     *
     * @param source the source of a node, which describes where the node comes from
     * @return the class of a {@link ClassSource} or {@link MethodSource}, or null if the source is of another kind, or
     *         if the class of a method cannot be loaded. The latter can happen with engines that describe methods by
     *         name only, in which case the node is not one of ours
     */
    private static Class<?> testClassOf(TestSource source) {
        if (source instanceof ClassSource cs) {
            return cs.getJavaClass();
        }
        if (source instanceof MethodSource ms) {
            try {
                return ms.getJavaClass();
            } catch (PreconditionViolationException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Tells whether a test class is a Quarkus test.
     *
     * @param classLoader the classloader of a test class
     * @return true if the class was loaded by a {@link QuarkusClassLoader}, which is how Quarkus tests are told apart
     */
    private static boolean isQuarkusTest(ClassLoader classLoader) {
        // We could check annotations, but that would be slow, and the assumption that only Quarkus Tests are loaded with the quarkus classloader should be a fair one
        return classLoader instanceof QuarkusClassLoader;
    }

    /**
     * Pushes the current TCCL of this thread onto its stack and replaces it with the classloader of the test.
     *
     * @param testIdentifier the node being started
     * @param classLoader the classloader of the test class, which becomes the TCCL
     */
    private void setTCCL(TestIdentifier testIdentifier, ClassLoader classLoader) {
        origCl.get().push(new SavedTccl(testIdentifier.getUniqueId(), Thread.currentThread().getContextClassLoader()));
        Thread.currentThread().setContextClassLoader(classLoader);
    }

    /**
     * Pops the latest entry of this thread's stack and makes it the TCCL again, but only if that entry was pushed by
     * the node that is finishing. Otherwise the TCCL is left as it is and the mismatch is logged at debug level.
     *
     * @param testIdentifier the node that has finished
     */
    private void unsetTCCL(TestIdentifier testIdentifier) {
        Deque<SavedTccl> stack = origCl.get();
        SavedTccl saved = stack.peek();
        if (saved == null || !saved.testId().equals(testIdentifier.getUniqueId())) {
            log.debugf("Not restoring the TCCL: %s did not start on thread %s or is not its latest test", testIdentifier,
                    Thread.currentThread().getName());
            if (stack.isEmpty()) {
                origCl.remove();
            }
            return;
        }
        stack.pop();
        if (stack.isEmpty()) {
            origCl.remove();
        }
        // TCCL state is per thread; parallel execution of Quarkus tests is still unsupported because QuarkusTestExtension keeps static state (#52994)
        Thread.currentThread().setContextClassLoader(saved.original());
    }

}
