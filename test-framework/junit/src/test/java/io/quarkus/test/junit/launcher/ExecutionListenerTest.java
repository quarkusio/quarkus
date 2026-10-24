package io.quarkus.test.junit.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import io.quarkus.bootstrap.classloading.MemoryClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;

class ExecutionListenerTest {

    private static final int TIMEOUT_SECONDS = 10;

    private final List<QuarkusClassLoader> createdClassLoaders = new ArrayList<>();

    @AfterEach
    void closeClassLoaders() throws Exception {
        for (QuarkusClassLoader classLoader : createdClassLoaders) {
            classLoader.close();
        }
        createdClassLoaders.clear();
    }

    @Test
    void shouldRestoreOriginalTcclOfEachThreadWhenTestsExecuteInParallel() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier quarkusTest = quarkusTestIdentifier();

        ClassLoader originalOfFirst = new ClassLoader("original-of-first", null) {
        };
        ClassLoader originalOfSecond = new ClassLoader("original-of-second", null) {
        };

        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch firstFinished = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ClassLoader> first = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(originalOfFirst);
                listener.executionStarted(quarkusTest);
                firstStarted.countDown();
                await(secondStarted);
                listener.executionFinished(quarkusTest, TestExecutionResult.successful());
                ClassLoader afterFinish = Thread.currentThread().getContextClassLoader();
                firstFinished.countDown();
                return afterFinish;
            });
            Future<ClassLoader> second = executor.submit(() -> {
                await(firstStarted);
                Thread.currentThread().setContextClassLoader(originalOfSecond);
                listener.executionStarted(quarkusTest);
                secondStarted.countDown();
                await(firstFinished);
                listener.executionFinished(quarkusTest, TestExecutionResult.successful());
                return Thread.currentThread().getContextClassLoader();
            });

            assertThat(first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(originalOfFirst);
            assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isSameAs(originalOfSecond);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldSetTcclToTestClassLoaderAndRestoreOriginalOnSingleThread() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier quarkusTest = quarkusTestIdentifier();
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        listener.executionStarted(quarkusTest);
        try {
            assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(quarkusTest));
        } finally {
            listener.executionFinished(quarkusTest, TestExecutionResult.successful());
        }

        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void shouldRestoreInReverseOrderForNestedQuarkusContainers() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier outer = quarkusTestIdentifier("outer");
        TestIdentifier nested = quarkusTestIdentifier("nested");
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        listener.executionStarted(outer);
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(outer));
        listener.executionStarted(nested);
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(nested));

        listener.executionFinished(nested, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(outer));

        listener.executionFinished(outer, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void shouldKeepTheStackIntactWhenAnUnrelatedTestFinishes() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier started = quarkusTestIdentifier("started");
        TestIdentifier unrelated = quarkusTestIdentifier("unrelated");
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        listener.executionStarted(started);
        listener.executionFinished(unrelated, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(started));

        listener.executionFinished(started, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void shouldNotTouchTheTcclOfAThreadFinishingATestItDidNotStart() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier quarkusTest = quarkusTestIdentifier();
        ClassLoader finishingThreadOriginal = new ClassLoader("finishing-thread-original", null) {
        };

        ExecutorService starter = Executors.newSingleThreadExecutor();
        ExecutorService finisher = Executors.newSingleThreadExecutor();
        try {
            ClassLoader tcclAfterStart = starter.submit(() -> {
                listener.executionStarted(quarkusTest);
                return Thread.currentThread().getContextClassLoader();
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(tcclAfterStart).isSameAs(classLoaderOf(quarkusTest));

            ClassLoader tcclAfterFinish = finisher.submit(() -> {
                Thread.currentThread().setContextClassLoader(finishingThreadOriginal);
                listener.executionFinished(quarkusTest, TestExecutionResult.successful());
                return Thread.currentThread().getContextClassLoader();
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(tcclAfterFinish).isSameAs(finishingThreadOriginal);
        } finally {
            starter.shutdownNow();
            finisher.shutdownNow();
        }
    }

    @Test
    void shouldRestoreANullTccl() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier quarkusTest = quarkusTestIdentifier();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ClassLoader afterFinish = executor.submit(() -> {
                Thread.currentThread().setContextClassLoader(null);
                listener.executionStarted(quarkusTest);
                listener.executionFinished(quarkusTest, TestExecutionResult.successful());
                return Thread.currentThread().getContextClassLoader();
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(afterFinish).isNull();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldIgnoreIdentifiersThatDoNotPointToAQuarkusTestClass() {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier sourceless = identifier("sourceless", null);
        TestIdentifier methodOfPlainClass = identifier("plain-method",
                MethodSource.from(ExecutionListenerTest.class.getName(), "closeClassLoaders"));
        TestIdentifier methodOfUnloadableClass = identifier("unloadable-method",
                MethodSource.from("io.quarkus.test.junit.launcher.DoesNotExist", "anyMethod"));
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        for (TestIdentifier id : new TestIdentifier[] { sourceless, methodOfPlainClass, methodOfUnloadableClass }) {
            listener.executionStarted(id);
            assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
            listener.executionFinished(id, TestExecutionResult.successful());
            assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
        }
    }

    @Test
    void shouldSetTcclForMethodNodesOnAWorkerThatDoesNotHaveTheQuarkusClassLoader() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier method = quarkusMethodIdentifier("method");
        ClassLoader workerOriginal = new ClassLoader("worker-original", null) {
        };

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            ClassLoader[] seen = worker.submit(() -> {
                Thread.currentThread().setContextClassLoader(workerOriginal);
                listener.executionStarted(method);
                ClassLoader duringTest = Thread.currentThread().getContextClassLoader();
                listener.executionFinished(method, TestExecutionResult.successful());
                return new ClassLoader[] { duringTest, Thread.currentThread().getContextClassLoader() };
            }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertThat(seen[0]).isSameAs(classLoaderOf(method));
            assertThat(seen[1]).isSameAs(workerOriginal);
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void shouldKeepTheClassLoaderOfTheClassWhileAMethodOfItRuns() throws Exception {
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier method = quarkusMethodIdentifier("method");
        TestIdentifier owningClass = identifier("class",
                ClassSource.from(((MethodSource) method.getSource().orElseThrow()).getJavaClass()));
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        listener.executionStarted(owningClass);
        listener.executionStarted(method);
        listener.executionFinished(method, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(classLoaderOf(owningClass));

        listener.executionFinished(owningClass, TestExecutionResult.successful());
        assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
    }

    @Test
    void shouldRestoreOwnTcclWhenMethodsOfTheSameClassRunConcurrently() throws Exception {
        int methods = 8;
        ExecutionListener listener = new ExecutionListener();
        TestIdentifier[] identifiers = new TestIdentifier[methods];
        for (int i = 0; i < methods; i++) {
            identifiers[i] = quarkusMethodIdentifier("method-" + i);
        }
        CyclicBarrier allStarted = new CyclicBarrier(methods);
        ExecutorService executor = Executors.newFixedThreadPool(methods);
        try {
            List<ClassLoader> originals = new ArrayList<>();
            List<Future<ClassLoader[]>> results = new ArrayList<>();
            for (int i = 0; i < methods; i++) {
                ClassLoader original = new ClassLoader("worker-" + i, null) {
                };
                originals.add(original);
                TestIdentifier id = identifiers[i];
                results.add(executor.submit(() -> {
                    Thread.currentThread().setContextClassLoader(original);
                    listener.executionStarted(id);
                    allStarted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                    ClassLoader duringTest = Thread.currentThread().getContextClassLoader();
                    listener.executionFinished(id, TestExecutionResult.successful());
                    return new ClassLoader[] { duringTest, Thread.currentThread().getContextClassLoader() };
                }));
            }
            for (int i = 0; i < methods; i++) {
                ClassLoader[] seen = results.get(i).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertThat(seen[0]).isSameAs(classLoaderOf(identifiers[i]));
                assertThat(seen[1]).isSameAs(originals.get(i));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    private static ClassLoader classLoaderOf(TestIdentifier identifier) {
        TestSource source = identifier.getSource().orElseThrow();
        Class<?> testClass = source instanceof MethodSource methodSource ? methodSource.getJavaClass()
                : ((ClassSource) source).getJavaClass();
        return testClass.getClassLoader();
    }

    private TestIdentifier quarkusMethodIdentifier(String name) throws Exception {
        Class<?> testClass = loadWithQuarkusClassLoader(QuarkusTestFixture.class);
        return identifier(name, MethodSource.from(testClass, testClass.getDeclaredMethod("aTestMethod")));
    }

    private TestIdentifier quarkusTestIdentifier() throws Exception {
        return quarkusTestIdentifier("fixture");
    }

    private TestIdentifier quarkusTestIdentifier(String name) throws Exception {
        Class<?> testClass = loadWithQuarkusClassLoader(QuarkusTestFixture.class);
        return identifier(name, ClassSource.from(testClass));
    }

    private static TestIdentifier identifier(String name, TestSource source) {
        TestDescriptor descriptor = new AbstractTestDescriptor(UniqueId.forEngine(name), name, source) {
            @Override
            public Type getType() {
                return Type.CONTAINER;
            }
        };
        return TestIdentifier.from(descriptor);
    }

    private Class<?> loadWithQuarkusClassLoader(Class<?> original) throws Exception {
        String resourceName = original.getName().replace('.', '/') + ".class";
        byte[] bytes;
        try (InputStream in = original.getClassLoader().getResourceAsStream(resourceName)) {
            bytes = in.readAllBytes();
        }
        QuarkusClassLoader classLoader = QuarkusClassLoader
                .builder("execution-listener-test", ExecutionListenerTest.class.getClassLoader(), false)
                .addNormalPriorityElement(new MemoryClassPathElement(Map.of(resourceName, bytes), true))
                .build();
        createdClassLoaders.add(classLoader);
        Class<?> loaded = Class.forName(original.getName(), false, classLoader);
        assertThat(loaded.getClassLoader()).isInstanceOf(QuarkusClassLoader.class);
        return loaded;
    }

    static class QuarkusTestFixture {
        void aTestMethod() {
        }
    }
}
