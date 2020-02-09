package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.AdditionalDependency;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.runtime.Timing;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

//todo: share common core with QuarkusUnitTest
public class QuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, InvocationInterceptor, AfterAllCallback {

    protected static final String TEST_LOCATION = "test-location";
    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    private static ClassLoader originalCl;
    private static RunningQuarkusApplication runningQuarkusApplication;
    private static Path testClassLocation;

    private ExtensionState doJavaStart(ExtensionContext context, TestResourceManager testResourceManager) {

        try {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();

            Path appClassLocation = getAppClassLocation(context.getRequiredTestClass());

            final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder(appClassLocation)
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.TEST);

            originalCl = Thread.currentThread().getContextClassLoader();
            testClassLocation = getTestClassesLocation(context.getRequiredTestClass());

            if (!appClassLocation.equals(testClassLocation)) {
                runnerBuilder.addAdditionalApplicationArchive(new AdditionalDependency(testClassLocation, false, true, true));
            }
            CuratedApplication curatedApplication = runnerBuilder
                    .setTest(true)
                    .setProjectRoot(new File("").toPath())
                    .setLocalProjectDiscovery(true).build()
                    .bootstrap();
            Timing.staticInitStarted(curatedApplication.getBaseRuntimeClassLoader());
            AugmentAction augmentAction = curatedApplication.createAugmentor(TestBuildChainFunction.class.getName(),
                    Collections.singletonMap(TEST_LOCATION, testClassLocation));
            runningQuarkusApplication = augmentAction.createInitialRuntimeApplication().run();

            ConfigProviderResolver.setInstance(new RunningAppConfigResolver(runningQuarkusApplication));

            Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());

            System.setProperty("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication));

            Closeable shutdownTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        runningQuarkusApplication.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        while (!shutdownTasks.isEmpty()) {
                            shutdownTasks.pop().run();
                        }
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        shutdownTask.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        curatedApplication.close();
                    }
                }
            }, "Quarkus Test Cleanup Shutdown task"));
            return new ExtensionState(testResourceManager, shutdownTask);
        } catch (java.util.ServiceConfigurationError | Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        if (!failedBoot) {
            boolean nativeImageTest = context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class)
                    || isNativeTest(context);
            runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                    .getDeclaredMethod("clearURL").invoke(null);
            runningQuarkusApplication.getClassLoader().loadClass(TestScopeManager.class.getName())
                    .getDeclaredMethod("tearDown", boolean.class).invoke(null, nativeImageTest);
        }
    }

    private boolean isNativeTest(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(NativeImageTest.class)
                | context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        if (!failedBoot) {
            boolean nativeImageTest = context.getRequiredTestClass().isAnnotationPresent(SubstrateTest.class)
                    || isNativeTest(context);
            if (runningQuarkusApplication != null) {
                runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                        .getDeclaredMethod("setURL", boolean.class).invoke(null, false);
                runningQuarkusApplication.getClassLoader().loadClass(TestScopeManager.class.getName())
                        .getDeclaredMethod("setup", boolean.class).invoke(null, nativeImageTest);
            }
        }
    }

    private ExtensionState ensureStarted(ExtensionContext extensionContext) {
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        if (state == null && !failedBoot) {
            PropertyTestUtil.setLogFileProperty();
            TestResourceManager testResourceManager = new TestResourceManager(extensionContext.getRequiredTestClass());
            try {
                testResourceManager.start();
                state = doJavaStart(extensionContext, testResourceManager);
                store.put(ExtensionState.class.getName(), state);

            } catch (Throwable e) {
                try {
                    testResourceManager.stop();
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
                failedBoot = true;
                throw e;
            }
        }
        return state;
    }

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
        ensureStarted(context);
        if (runningQuarkusApplication != null) {
            setCCL(runningQuarkusApplication.getClassLoader());
        }
        if (failedBoot) {
            throw new TestAbortedException("Not running test as boot failed");
        }
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        ensureStarted(extensionContext);
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
            ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            return invocation.proceed();
        }
        T result;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(extensionContext.getRequiredTestClass().getClassLoader());
            result = invocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        ExtensionState state = ensureStarted(extensionContext);
        if (failedBoot) {
            return invocation.proceed();
        }
        initTestState(extensionContext, state);
        return result;
    }

    private void initTestState(ExtensionContext extensionContext, ExtensionState state) {
        try {
            actualTestClass = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());

            actualTestInstance = runningQuarkusApplication.instance(actualTestClass);

            Class<?> resM = Thread.currentThread().getContextClassLoader().loadClass(TestHTTPResourceManager.class.getName());
            resM.getDeclaredMethod("inject", Object.class).invoke(null, actualTestInstance);
            state.testResourceManager.inject(actualTestInstance);
        } catch (Exception e) {
            throw new TestInstantiationException("Failed to create test instance", e);
        }
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    private void runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
        Method newMethod = null;

        try {
            Class<?> c = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());
            ;
            while (c != Object.class) {
                if (c.getName().equals(invocationContext.getExecutable().getDeclaringClass().getName())) {
                    try {
                        newMethod = c.getDeclaredMethod(invocationContext.getExecutable().getName(),
                                invocationContext.getExecutable().getParameterTypes());
                        break;
                    } catch (NoSuchMethodException e) {
                        //ignore
                    }
                }
                c = c.getSuperclass();
            }
            if (newMethod == null) {
                throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
            }
            newMethod.setAccessible(true);
            newMethod.invoke(actualTestInstance, invocationContext.getArguments().toArray());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        } catch (IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (originalCl != null) {
            setCCL(originalCl);
        }
    }

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
        }

        @Override
        public void close() throws Throwable {
            try {
                resource.close();
            } finally {
                if (QuarkusTestExtension.this.originalCl != null) {
                    setCCL(QuarkusTestExtension.this.originalCl);
                }
                testResourceManager.stop();
            }
        }
    }

    public static class TestBuildChainFunction implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> stringObjectMap) {
            Path testLocation = (Path) stringObjectMap.get(TEST_LOCATION);
            return Collections.singletonList(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                                @Override
                                public boolean test(String className) {
                                    return PathTestHelper.isTestClass(className,
                                            Thread.currentThread().getContextClassLoader(), testLocation);
                                }
                            }));
                        }
                    }).produces(TestClassPredicateBuildItem.class)
                            .build();

                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
                        }
                    }).produces(TestAnnotationBuildItem.class)
                            .build();
                }
            });
        }
    }
}
