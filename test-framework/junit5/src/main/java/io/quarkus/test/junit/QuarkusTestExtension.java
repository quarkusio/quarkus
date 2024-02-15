package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstantiationException;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestProfileBuildItem;
import io.quarkus.dev.testing.ExceptionReporting;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.runtime.ApplicationLifecycleManager;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.TestMethodInvoker;
import io.quarkus.test.common.GroovyClassValue;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.internal.DeepClone;
import io.quarkus.test.junit.internal.SerializationWithXStreamFallbackDeepClone;

public class QuarkusTestExtension extends AbstractJvmQuarkusTestExtension
        implements BeforeEachCallback, BeforeTestExecutionCallback, AfterTestExecutionCallback, AfterEachCallback,
        BeforeAllCallback, InvocationInterceptor, AfterAllCallback,
        ParameterResolver, ExecutionCondition {

    private static final Logger log = Logger.getLogger(QuarkusTestExtension.class);

    public static final String QUARKUS_TEST_HANG_DETECTION_TIMEOUT = "quarkus.test.hang-detection-timeout";

    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    // needed for @Nested
    private static final Deque<Object> outerInstances = new ArrayDeque<>(1);
    private static RunningQuarkusApplication runningQuarkusApplication;
    private static Pattern clonePattern;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static Class<?> quarkusTestMethodContextClass;
    private static boolean hasPerTestResources;
    private static List<Function<Class<?>, String>> testHttpEndpointProviders;

    private static List<Object> testMethodInvokers;

    private static DeepClone deepClone;
    private static volatile ScheduledExecutorService hangDetectionExecutor;
    private static volatile Duration hangTimeout;
    private static volatile ScheduledFuture<?> hangTaskKey;
    private static final Runnable hangDetectionTask = new Runnable() {

        final AtomicBoolean runOnce = new AtomicBoolean();

        @Override
        public void run() {
            if (!runOnce.compareAndSet(false, true)) {
                return;
            }
            System.err.println("@QuarkusTest has detected a hang, as there has been no test activity in " + hangTimeout);
            System.err.println("To configure this timeout use the " + QUARKUS_TEST_HANG_DETECTION_TIMEOUT + " config property");
            System.err.println("A stack trace is below to help diagnose the potential hang");
            System.err.println("=== Stack Trace ===");
            ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
            for (ThreadInfo info : threads) {
                if (info == null) {
                    System.err.println("  Inactive");
                    continue;
                }
                Thread.State state = info.getThreadState();
                System.err.println("Thread " + info.getThreadName() + ": " + state);
                if (state == Thread.State.WAITING) {
                    System.err.println("  Waiting on " + info.getLockName());
                } else if (state == Thread.State.BLOCKED) {
                    System.err.println("  Blocked on " + info.getLockName());
                    System.err.println("  Blocked by " + info.getLockOwnerName());
                }
                System.err.println("  Stack:");
                for (StackTraceElement frame : info.getStackTrace()) {
                    System.err.println("    " + frame.toString());
                }
            }
            System.err.println("=== End Stack Trace ===");
            //we only every dump once
        }
    };

    static {
        ClassLoader classLoader = QuarkusTestExtension.class.getClassLoader();
        if (classLoader instanceof QuarkusClassLoader) {
            ((QuarkusClassLoader) classLoader).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    ScheduledExecutorService h = QuarkusTestExtension.hangDetectionExecutor;
                    if (h != null) {
                        h.shutdownNow();
                        QuarkusTestExtension.hangDetectionExecutor = null;
                    }
                }
            });
        }
    }

    private ExtensionState doJavaStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile) throws Throwable {
        JBossVersion.disableVersionLogging();

        TracingHandler.quarkusStarting();
        hangDetectionExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Quarkus hang detection timer thread");
            }
        });
        String time = "10m";
        //config is not established yet
        //we can only read from system properties
        String sysPropString = System.getProperty(QUARKUS_TEST_HANG_DETECTION_TIMEOUT);
        if (sysPropString != null) {
            time = sysPropString;
        }
        hangTimeout = new DurationConverter().convert(time);
        hangTaskKey = hangDetectionExecutor.schedule(hangDetectionTask, hangTimeout.toMillis(), TimeUnit.MILLISECONDS);

        quarkusTestProfile = profile;
        Class<?> requiredTestClass = context.getRequiredTestClass();
        Closeable testResourceManager = null;
        try {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();
            PrepareResult result = createAugmentor(context, profile, shutdownTasks);
            AugmentAction augmentAction = result.augmentAction;
            QuarkusTestProfile profileInstance = result.profileInstance;

            testHttpEndpointProviders = TestHttpEndpointProvider.load();
            StartupAction startupAction = augmentAction.createInitialRuntimeApplication();
            Thread.currentThread().setContextClassLoader(startupAction.getClassLoader());
            populateDeepCloneField(startupAction);

            //must be done after the TCCL has been set
            testResourceManager = (Closeable) startupAction.getClassLoader().loadClass(TestResourceManager.class.getName())
                    .getConstructor(Class.class, Class.class, List.class, boolean.class, Map.class, Optional.class, Path.class)
                    .newInstance(requiredTestClass,
                            profile != null ? profile : null,
                            getAdditionalTestResources(profileInstance, startupAction.getClassLoader()),
                            profileInstance != null && profileInstance.disableGlobalTestResources(),
                            startupAction.getDevServicesProperties(), Optional.empty(), result.testClassLocation);
            testResourceManager.getClass().getMethod("init", String.class).invoke(testResourceManager,
                    profile != null ? profile.getName() : null);
            Map<String, String> properties = (Map<String, String>) testResourceManager.getClass().getMethod("start")
                    .invoke(testResourceManager);
            startupAction.overrideConfig(properties);
            hasPerTestResources = (boolean) testResourceManager.getClass().getMethod("hasPerTestResources")
                    .invoke(testResourceManager);

            // make sure that we start over every time we populate the callbacks
            // otherwise previous runs of QuarkusTest (with different TestProfile values can leak into the new run)
            quarkusTestMethodContextClass = null;
            populateCallbacks(startupAction.getClassLoader());
            populateTestMethodInvokers(startupAction.getClassLoader());

            if (profileInstance == null || !profileInstance.runMainMethod()) {
                runningQuarkusApplication = startupAction
                        .run(profileInstance == null ? new String[0] : profileInstance.commandLineParameters());
            } else {

                Class<?> lifecycleManager = Class.forName(ApplicationLifecycleManager.class.getName(), true,
                        startupAction.getClassLoader());
                lifecycleManager.getDeclaredMethod("setDefaultExitCodeHandler", Consumer.class).invoke(null,
                        (Consumer<Integer>) integer -> {
                        });
                runningQuarkusApplication = startupAction
                        .runMainClass(profileInstance.commandLineParameters());
            }
            String patternString = runningQuarkusApplication.getConfigValue("quarkus.test.class-clone-pattern", String.class)
                    .orElse("java\\..*");
            clonePattern = Pattern.compile(patternString);
            TracingHandler.quarkusStarted();

            //now we have full config reset the hang timer

            if (hangTaskKey != null) {
                hangTaskKey.cancel(false);
                hangTimeout = runningQuarkusApplication.getConfigValue(QUARKUS_TEST_HANG_DETECTION_TIMEOUT, Duration.class)
                        .orElse(Duration.of(10, ChronoUnit.MINUTES));
                hangTaskKey = hangDetectionExecutor.schedule(hangDetectionTask, hangTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            ConfigProviderResolver.setInstance(new RunningAppConfigResolver(runningQuarkusApplication));
            RestorableSystemProperties restorableSystemProperties = RestorableSystemProperties.setProperties(
                    Collections.singletonMap("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication)));

            Closeable tm = testResourceManager;
            Closeable shutdownTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    TracingHandler.quarkusStopping();
                    try {
                        runningQuarkusApplication.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        TracingHandler.quarkusStopped();
                        try {
                            while (!shutdownTasks.isEmpty()) {
                                shutdownTasks.pop().run();
                            }
                        } finally {
                            try {
                                tm.close();
                            } finally {
                                restorableSystemProperties.close();
                                shutdownHangDetection();
                            }
                        }
                        try {
                            TestClassIndexer.removeIndex(requiredTestClass);
                        } catch (Exception ignored) {
                        }
                    }
                }
            };
            ExtensionState state = new ExtensionState(testResourceManager, shutdownTask);
            return state;
        } catch (Throwable e) {
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                activateLogging();
            }

            Throwable effectiveException = determineEffectiveException(e);

            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception ex) {
                effectiveException.addSuppressed(determineEffectiveException(ex));
            }

            throw effectiveException;
        } finally {
            if (originalCl != null) {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }
    }

    private Throwable determineEffectiveException(Throwable e) {
        Throwable effectiveException = e;
        if ((e instanceof InvocationTargetException) && (e.getCause() != null)) { // QuarkusTestResourceLifecycleManager.start is called reflectively
            effectiveException = e.getCause();
            if ((effectiveException instanceof CompletionException) && (effectiveException.getCause() != null)) { // can happen because instances of QuarkusTestResourceLifecycleManager are started asynchronously
                effectiveException = effectiveException.getCause();
            }
        }
        return effectiveException;
    }

    private void shutdownHangDetection() {
        if (hangTaskKey != null) {
            hangTaskKey.cancel(true);
            hangTaskKey = null;
        }
        var h = hangDetectionExecutor;
        if (h != null) {
            h.shutdownNow();
            hangDetectionExecutor = null;
        }
    }

    private void populateDeepCloneField(StartupAction startupAction) {
        deepClone = new SerializationWithXStreamFallbackDeepClone(startupAction.getClassLoader());
    }

    private void populateTestMethodInvokers(ClassLoader quarkusClassLoader) {
        testMethodInvokers = new ArrayList<>();
        try {
            ServiceLoader<?> loader = ServiceLoader.load(quarkusClassLoader.loadClass(TestMethodInvoker.class.getName()),
                    quarkusClassLoader);
            for (Object testMethodInvoker : loader) {
                testMethodInvokers.add(testMethodInvoker);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass()) || isBeforeTestCallbacksEmpty()) {
            return;
        }
        if (!failedBoot) {
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
            try {
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                invokeBeforeTestExecutionCallbacks(tuple.getKey(), tuple.getValue());
            } finally {
                setCCL(original);
            }
        } else {
            throwBootFailureException();
            return;
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass())) {
            return;
        }
        resetHangTimeout();
        if (!failedBoot) {
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
            try {
                pushMockContext();
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                invokeBeforeEachCallbacks(tuple.getKey(), tuple.getValue());
                String endpointPath = getEndpointPath(context, testHttpEndpointProviders);
                if (runningQuarkusApplication != null) {
                    boolean secure = false;
                    Optional<String> insecureAllowed = runningQuarkusApplication
                            .getConfigValue("quarkus.http.insecure-requests", String.class);
                    if (insecureAllowed.isPresent()) {
                        secure = !insecureAllowed.get().toLowerCase(Locale.ENGLISH).equals("enabled");
                    }
                    runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                            .getDeclaredMethod("setURL", boolean.class, String.class).invoke(null, secure, endpointPath);
                    runningQuarkusApplication.getClassLoader().loadClass(TestScopeManager.class.getName())
                            .getDeclaredMethod("setup", boolean.class).invoke(null, false);
                }
            } finally {
                setCCL(original);
            }
        } else {
            throwBootFailureException();
            return;
        }
    }

    public static String getEndpointPath(ExtensionContext context, List<Function<Class<?>, String>> testHttpEndpointProviders) {
        String endpointPath = null;
        TestHTTPEndpoint testHTTPEndpoint = context.getRequiredTestMethod().getAnnotation(TestHTTPEndpoint.class);
        if (testHTTPEndpoint == null) {
            Class<?> clazz = context.getRequiredTestClass();
            while (true) {
                // go up the hierarchy because most Native tests extend from a regular Quarkus test
                testHTTPEndpoint = clazz.getAnnotation(TestHTTPEndpoint.class);
                if (testHTTPEndpoint != null) {
                    break;
                }
                clazz = clazz.getSuperclass();
                if (clazz == Object.class) {
                    break;
                }
            }
        }
        if (testHTTPEndpoint != null) {
            for (Function<Class<?>, String> i : testHttpEndpointProviders) {
                endpointPath = i.apply(testHTTPEndpoint.value());
                if (endpointPath != null) {
                    break;
                }
            }
            if (endpointPath == null) {
                throw new RuntimeException("Cannot determine HTTP path for endpoint " + testHTTPEndpoint.value()
                        + " for test method " + context.getRequiredTestMethod());
            }
        }
        if (endpointPath != null) {
            if (endpointPath.indexOf(':') != -1) {
                return sanitizeEndpointPath(endpointPath);
            }
        }

        return endpointPath;
    }

    /**
     * Remove any sort of regex restrictions from "variables in the path"
     */
    private static String sanitizeEndpointPath(String path) {
        int openBrackets = 0;
        boolean inRegex = false;
        StringBuilder replaced = new StringBuilder(path.length() - 1);
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '{') {
                openBrackets++;
            } else if (c == '}') {
                openBrackets--;
                if (openBrackets == 0) {
                    inRegex = false;
                }
            } else if ((c == ':') && (openBrackets > 0)) {
                inRegex = true;
            }
            if (!inRegex) {
                replaced.append(c);
            }

        }
        return replaced.toString();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass()) || isAfterTestCallbacksEmpty()) {
            return;
        }
        if (!failedBoot) {
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
            try {
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                invokeAfterTestExecutionCallbacks(tuple.getKey(), tuple.getValue());
            } finally {
                setCCL(original);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass())) {
            return;
        }
        resetHangTimeout();
        if (!failedBoot) {
            popMockContext();
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
            try {
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                invokeAfterEachCallbacks(tuple.getKey(), tuple.getValue());
                runningQuarkusApplication.getClassLoader().loadClass(RestAssuredURLManager.class.getName())
                        .getDeclaredMethod("clearURL").invoke(null);
                runningQuarkusApplication.getClassLoader().loadClass(TestScopeManager.class.getName())
                        .getDeclaredMethod("tearDown", boolean.class).invoke(null, false);
            } finally {
                setCCL(original);
            }
        }
    }

    // We need the usual ClassLoader hacks in order to present the callbacks with the proper test object and context
    private Map.Entry<Class<?>, ?> createQuarkusTestMethodContextTuple(ExtensionContext context) throws Exception {
        ClassLoader classLoader = runningQuarkusApplication.getClassLoader();
        if (quarkusTestMethodContextClass == null) {
            quarkusTestMethodContextClass = Class.forName(QuarkusTestMethodContext.class.getName(), true, classLoader);
        }

        Method originalTestMethod = context.getRequiredTestMethod();
        Class<?>[] originalParameterTypes = originalTestMethod.getParameterTypes();
        Method actualTestMethod = null;

        // go up the class hierarchy to fetch the proper test method
        Class<?> c = resolveDeclaringClass(originalTestMethod, actualTestClass);
        List<Class<?>> parameterTypesFromTccl = new ArrayList<>(originalParameterTypes.length);
        for (Class<?> type : originalParameterTypes) {
            if (type.isPrimitive()) {
                parameterTypesFromTccl.add(type);
            } else {
                parameterTypesFromTccl
                        .add(Class.forName(type.getName(), true, classLoader));
            }
        }
        Class<?>[] parameterTypes = parameterTypesFromTccl.toArray(new Class[0]);
        try {
            if (c != null) {
                actualTestMethod = c.getDeclaredMethod(originalTestMethod.getName(), parameterTypes);
            }
        } catch (NoSuchMethodException ignored) {

        }
        if (actualTestMethod == null) {
            throw new RuntimeException("Could not find method " + originalTestMethod + " on test class");
        }

        QuarkusTestExtensionState state = getState(context);
        Constructor<?> constructor = quarkusTestMethodContextClass.getConstructor(Object.class, List.class, Method.class,
                Throwable.class);
        return new AbstractMap.SimpleEntry<>(quarkusTestMethodContextClass,
                constructor.newInstance(actualTestInstance, new ArrayList<>(outerInstances), actualTestMethod,
                        state.getTestErrorCause()));
    }

    private boolean isNativeOrIntegrationTest(Class<?> clazz) {
        for (Class<?> i : currentTestClassStack) {
            if (i.isAnnotationPresent(QuarkusIntegrationTest.class)) {
                return true;
            }
        }
        if (clazz.isAnnotationPresent(QuarkusIntegrationTest.class)) {
            return true;
        }
        return false;
    }

    private QuarkusTestExtensionState ensureStarted(ExtensionContext extensionContext) {
        QuarkusTestExtensionState state = getState(extensionContext);
        Class<? extends QuarkusTestProfile> selectedProfile = getQuarkusTestProfile(extensionContext);
        boolean wrongProfile = !Objects.equals(selectedProfile, quarkusTestProfile);
        // we reset the failed state if we changed test class and the new test class is not a nested class
        boolean isNewTestClass = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass)
                && !isNested(currentJUnitTestClass, extensionContext.getRequiredTestClass());
        if (isNewTestClass && state != null) {
            state.setTestFailed(null);
            currentJUnitTestClass = extensionContext.getRequiredTestClass();
        }
        // we reload the test resources if we changed test class and the new test class is not a nested class, and if we had or will have per-test test resources
        boolean reloadTestResources = isNewTestClass && (hasPerTestResources || hasPerTestResources(extensionContext));
        if ((state == null && !failedBoot) || wrongProfile || reloadTestResources) {
            if (wrongProfile || reloadTestResources) {
                if (state != null) {
                    try {
                        state.close();
                    } catch (Throwable throwable) {
                        markTestAsFailed(extensionContext, throwable);
                    }
                }
            }
            PropertyTestUtil.setLogFileProperty();
            try {
                state = doJavaStart(extensionContext, selectedProfile);
                setState(extensionContext, state);

            } catch (Throwable e) {
                failedBoot = true;
                markTestAsFailed(extensionContext, e);
                firstException = e;
                getStoreFromContext(extensionContext).put(FailedCleanup.class.getName(), new FailedCleanup());
            }
        }
        return state;
    }

    private boolean isNested(Class<?> testClass, Class<?> currentTestClass) {
        if (testClass == null || currentTestClass.getEnclosingClass() == null) {
            return false;
        }

        Class<?> enclosingTestClass = currentTestClass.getEnclosingClass();
        return Objects.equals(testClass, enclosingTestClass) || isNested(testClass, enclosingTestClass);
    }

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    private void throwBootFailureException() {
        if (firstException != null) {
            Throwable throwable = firstException;
            firstException = null;
            throw new RuntimeException(throwable);
        } else {
            throw new TestAbortedException("Boot failed");
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        Class<?> requiredTestClass = context.getRequiredTestClass();
        GroovyClassValue.disable();
        currentTestClassStack.push(requiredTestClass);
        //set the right launch mode in the outer CL, used by the HTTP host config source
        ProfileManager.setLaunchMode(LaunchMode.TEST);
        if (isNativeOrIntegrationTest(requiredTestClass)) {
            return;
        }
        resetHangTimeout();
        ensureStarted(context);
        if (runningQuarkusApplication != null) {
            pushMockContext();
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
                invokeBeforeClassCallbacks(Class.class,
                        runningQuarkusApplication.getClassLoader().loadClass(requiredTestClass.getName()));
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } else {
            // can this ever happen?
            invokeBeforeClassCallbacks(Class.class, requiredTestClass);
        }
    }

    private void pushMockContext() {
        try {
            //classloader issues
            Method pushContext = runningQuarkusApplication.getClassLoader().loadClass(MockSupport.class.getName())
                    .getDeclaredMethod("pushContext");
            pushContext.setAccessible(true);
            pushContext.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void popMockContext() {
        try {
            //classloader issues
            Method popContext = runningQuarkusApplication.getClassLoader().loadClass(MockSupport.class.getName())
                    .getDeclaredMethod("popContext");
            popContext.setAccessible(true);
            popContext.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void interceptBeforeAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }
        resetHangTimeout();
        ensureStarted(extensionContext);
        if (failedBoot) {
            throwBootFailureException();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @Override
    public <T> T interceptTestClassConstructor(Invocation<T> invocation,
            ReflectiveInvocationContext<Constructor<T>> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            return invocation.proceed();
        }
        resetHangTimeout();
        QuarkusTestExtensionState state = ensureStarted(extensionContext);
        if (failedBoot) {
            throwBootFailureException();
            return null;
        }
        T result;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Class<?> requiredTestClass = extensionContext.getRequiredTestClass();

        try {
            Thread.currentThread().setContextClassLoader(requiredTestClass.getClassLoader());
            result = invocation.proceed();
        } catch (NullPointerException e) {
            throw new RuntimeException(
                    "When using constructor injection in a test, the only legal operation is to assign the constructor values to fields. Offending class is "
                            + requiredTestClass,
                    e);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        // We do this here as well, because when @TestInstance(Lifecycle.PER_CLASS) is used on a class,
        // interceptTestClassConstructor is called before beforeAll, meaning that the TCCL will not be set correctly
        // (for any test other than the first) unless this is done
        old = null;
        if (runningQuarkusApplication != null) {
            old = setCCL(runningQuarkusApplication.getClassLoader());
        }

        try {
            initTestState(extensionContext, state);
        } finally {
            if (old != null) {
                setCCL(old);
            }
        }
        return result;
    }

    private void initTestState(ExtensionContext extensionContext, QuarkusTestExtensionState state) {
        try {
            actualTestClass = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());
            if (extensionContext.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
                Class<?> outerClass = actualTestClass.getEnclosingClass();
                Constructor<?> declaredConstructor = actualTestClass.getDeclaredConstructor(outerClass);
                declaredConstructor.setAccessible(true);
                if (outerClass.isInstance(actualTestInstance)) {
                    outerInstances.add(actualTestInstance);
                    actualTestInstance = declaredConstructor.newInstance(actualTestInstance);
                } else {
                    Object outerInstance = createActualTestInstance(outerClass, state);
                    invokeAfterConstructCallbacks(Object.class, outerInstance);
                    actualTestInstance = declaredConstructor.newInstance(outerInstance);
                    outerInstances.add(outerInstance);
                }
            } else {
                outerInstances.clear();
                actualTestInstance = createActualTestInstance(actualTestClass, state);
            }

            invokeAfterConstructCallbacks(Object.class, actualTestInstance);
        } catch (Exception e) {
            throw new TestInstantiationException("Failed to create test instance",
                    e instanceof InvocationTargetException ? e.getCause() : e);
        }
    }

    private Object createActualTestInstance(Class<?> testClass, QuarkusTestExtensionState state)
            throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object testInstance = runningQuarkusApplication.instance(testClass);

        Class<?> resM = Thread.currentThread().getContextClassLoader().loadClass(TestHTTPResourceManager.class.getName());
        resM.getDeclaredMethod("inject", Object.class, List.class).invoke(null, testInstance,
                testHttpEndpointProviders);
        state.testResourceManager.getClass().getMethod("inject", Object.class).invoke(state.testResourceManager,
                testInstance);

        return testInstance;
    }

    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext, true);
        invocation.skip();
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }

        //as a convenience to the user we attach any exceptions from the server itself
        //as suppressed exceptions from the failure
        //this makes it easy to see why your request has failed in the test output itself
        //instead of needed to look in the log output
        List<Throwable> serverExceptions = new CopyOnWriteArrayList<>();
        ExceptionReporting.setListener(serverExceptions::add);
        try {
            runExtensionMethod(invocationContext, extensionContext, true);
            invocation.skip();
        } catch (Throwable t) {
            for (var serverException : serverExceptions) {
                if (t == serverException) {
                    // do not add a suppressed exception to itself
                    continue;
                }

                t.addSuppressed(serverException);
            }
            throw t;
        } finally {
            ExceptionReporting.setListener(null);
        }

    }

    @Override
    public void interceptDynamicTest(Invocation<Void> invocation, ExtensionContext extensionContext) throws Throwable {
        if (runningQuarkusApplication == null) {
            invocation.proceed();
            return;
        }
        var old = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
            invocation.proceed();
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            return invocation.proceed();
        }
        T result = (T) runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
        return result;
    }

    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext, true);
        invocation.skip();
    }

    @Override
    public void interceptAfterAllMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (isNativeOrIntegrationTest(extensionContext.getRequiredTestClass())) {
            invocation.proceed();
            return;
        }
        runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
    }

    private Object runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
            throws Throwable {
        return runExtensionMethod(invocationContext, extensionContext, false);
    }

    private Object runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext,
            boolean testMethodInvokersAllowed)
            throws Throwable {
        resetHangTimeout();

        ClassLoader old = setCCL(runningQuarkusApplication.getClassLoader());
        try {
            Class<?> testClassFromTCCL = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());
            Map<Class<?>, Object> allTestsClasses = new HashMap<>();
            // static loading
            allTestsClasses.put(testClassFromTCCL, actualTestInstance);
            // this is needed to support before*** and after*** methods that are part of class that encloses the test class
            // (the test class is in this case a @Nested test)
            outerInstances.forEach(i -> allTestsClasses.put(i.getClass(), i));
            Method newMethod = null;
            Object effectiveTestInstance = null;
            for (Map.Entry<Class<?>, Object> testClass : allTestsClasses.entrySet()) {
                newMethod = determineTCCLExtensionMethod(invocationContext.getExecutable(), testClass.getKey());
                if (newMethod != null) {
                    effectiveTestInstance = testClass.getValue();
                    break;
                }
            }

            if (newMethod == null) {
                throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
            }
            newMethod.setAccessible(true);

            Object testMethodInvokerToUse = null;
            if (testMethodInvokersAllowed) {
                for (Object testMethodInvoker : testMethodInvokers) {
                    boolean supportsMethod = (boolean) testMethodInvoker.getClass()
                            .getMethod("supportsMethod", Class.class, Method.class).invoke(testMethodInvoker,
                                    extensionContext.getRequiredTestClass(), invocationContext.getExecutable());
                    if (supportsMethod) {
                        testMethodInvokerToUse = testMethodInvoker;
                        break;
                    }
                }
            }

            // the arguments were not loaded from TCCL so we need to deep clone them into the TCCL
            // because the test method runs from a class loaded from the TCCL
            //TODO: make this more pluggable
            List<Object> originalArguments = invocationContext.getArguments();
            List<Object> argumentsFromTccl = new ArrayList<>();
            Parameter[] parameters = invocationContext.getExecutable().getParameters();
            for (int i = 0; i < originalArguments.size(); i++) {
                Object arg = originalArguments.get(i);
                boolean cloneRequired = false;
                Object replacement = null;
                Class<?> argClass = parameters[i].getType();
                if (arg != null) {
                    Class<?> theclass = argClass;
                    while (theclass.isArray()) {
                        theclass = theclass.getComponentType();
                    }
                    if (theclass.isPrimitive()) {
                        cloneRequired = false;
                    } else if (TestInfo.class.isAssignableFrom(theclass)) {
                        TestInfo info = (TestInfo) arg;
                        Method newTestMethod = info.getTestMethod().isPresent()
                                ? determineTCCLExtensionMethod(info.getTestMethod().get(), testClassFromTCCL)
                                : null;
                        replacement = new TestInfoImpl(info.getDisplayName(), info.getTags(),
                                Optional.of(testClassFromTCCL),
                                Optional.ofNullable(newTestMethod));
                    } else if (clonePattern.matcher(theclass.getName()).matches()) {
                        cloneRequired = true;
                    } else {
                        try {
                            cloneRequired = runningQuarkusApplication.getClassLoader()
                                    .loadClass(theclass.getName()) != theclass;
                        } catch (ClassNotFoundException e) {
                            if (arg instanceof Supplier) {
                                cloneRequired = true;
                            } else {
                                throw e;
                            }
                        }
                    }
                }

                if (replacement != null) {
                    argumentsFromTccl.add(replacement);
                } else if (cloneRequired) {
                    argumentsFromTccl.add(deepClone.clone(arg));
                } else if (testMethodInvokerToUse != null) {
                    argumentsFromTccl.add(testMethodInvokerToUse.getClass().getMethod("methodParamInstance", String.class)
                            .invoke(testMethodInvokerToUse, argClass.getName()));
                } else {
                    argumentsFromTccl.add(arg);
                }
            }

            if (testMethodInvokerToUse != null) {
                return testMethodInvokerToUse.getClass()
                        .getMethod("invoke", Object.class, Method.class, List.class, String.class)
                        .invoke(testMethodInvokerToUse, effectiveTestInstance, newMethod, argumentsFromTccl,
                                extensionContext.getRequiredTestClass().getName());
            } else {
                return newMethod.invoke(effectiveTestInstance, argumentsFromTccl.toArray(new Object[0]));
            }

        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            setCCL(old);
        }
    }

    private Method determineTCCLExtensionMethod(Method originalMethod, Class<?> c)
            throws ClassNotFoundException {
        Class<?> declaringClass = resolveDeclaringClass(originalMethod, c);
        if (declaringClass == null) {
            return null;
        }
        try {
            Class<?>[] originalParameterTypes = originalMethod.getParameterTypes();
            List<Class<?>> parameterTypesFromTccl = new ArrayList<>(originalParameterTypes.length);
            for (Class<?> type : originalParameterTypes) {
                if (type.isPrimitive()) {
                    parameterTypesFromTccl.add(type);
                } else {
                    parameterTypesFromTccl
                            .add(Class.forName(type.getName(), true,
                                    Thread.currentThread().getContextClassLoader()));
                }
            }
            return declaringClass.getDeclaredMethod(originalMethod.getName(),
                    parameterTypesFromTccl.toArray(new Class[0]));
        } catch (NoSuchMethodException ignored) {

        }

        return null;
    }

    private Class<?> resolveDeclaringClass(Method method, Class<?> c) {
        if (c == Object.class || c == null) {
            return null;
        }

        if (c.getName().equals(method.getDeclaringClass().getName())) {
            return c;
        }
        Class<?> declaringClass = resolveDeclaringClass(method, c.getSuperclass());
        if (declaringClass != null) {
            return declaringClass;
        }
        for (Class<?> anInterface : c.getInterfaces()) {
            declaringClass = resolveDeclaringClass(method, anInterface);
            if (declaringClass != null) {
                return declaringClass;
            }
        }
        return null;
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        resetHangTimeout();
        runAfterAllCallbacks(context);
        try {
            if (!isNativeOrIntegrationTest(context.getRequiredTestClass()) && (runningQuarkusApplication != null)) {
                popMockContext();
            }
            if (originalCl != null) {
                setCCL(originalCl);
            }
        } finally {
            currentTestClassStack.pop();
            if (!outerInstances.isEmpty()) {
                actualTestInstance = outerInstances.pop();
            }
        }
    }

    private void runAfterAllCallbacks(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass()) || failedBoot) {
            return;
        }
        if (isAfterAllCallbacksEmpty()) {
            return;
        }

        QuarkusTestExtensionState state = getState(context);
        Class<?> quarkusTestContextClass = Class.forName(QuarkusTestContext.class.getName(), true,
                runningQuarkusApplication.getClassLoader());
        Object quarkusTestContextInstance = quarkusTestContextClass.getConstructor(Object.class, List.class, Throwable.class)
                .newInstance(actualTestInstance, new ArrayList<>(outerInstances), state.getTestErrorCause());

        ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
        try {
            invokeAfterAllCallbacks(quarkusTestContextClass, quarkusTestContextInstance);
        } finally {
            setCCL(original);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        boolean isConstructor = parameterContext.getDeclaringExecutable() instanceof Constructor;
        if (isConstructor) {
            return true;
        }
        if (!(parameterContext.getDeclaringExecutable() instanceof Method)) {
            return false;
        }
        if (testMethodInvokers == null) {
            return false;
        }
        for (Object testMethodInvoker : testMethodInvokers) {
            boolean handlesMethodParamType = testMethodInvokerHandlesParamType(testMethodInvoker, parameterContext);
            if (handlesMethodParamType) {
                return true;
            }
        }
        return false;
    }

    /**
     * We don't actually have to resolve the parameter (thus the default values in the implementation)
     * since the class instance that is passed to JUnit isn't really used.
     * The actual test instance that is used is the one that is pulled from Arc, which of course will already have its
     * constructor parameters properly resolved
     */
    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if ((parameterContext.getDeclaringExecutable() instanceof Method) && (testMethodInvokers != null)) {
            for (Object testMethodInvoker : testMethodInvokers) {
                if (testMethodInvokerHandlesParamType(testMethodInvoker, parameterContext)) {
                    return null; // return null as this will actually be populated when we invoke the actual test instance
                }
            }
        }
        String className = parameterContext.getParameter().getType().getName();
        switch (className) {
            case "boolean":
                return false;
            case "byte":
            case "short":
            case "int":
                return 0;
            case "long":
                return 0L;
            case "float":
                return 0.0f;
            case "double":
                return 0.0d;
            case "char":
                return '\u0000';
            default:
                return null;
        }
    }

    // we need to use reflection because the instances of TestMethodInvoker are load from the QuarkusClassLoader
    private boolean testMethodInvokerHandlesParamType(Object testMethodInvoker, ParameterContext parameterContext) {
        try {
            return (boolean) testMethodInvoker.getClass().getMethod("handlesMethodParamType", String.class)
                    .invoke(testMethodInvoker, parameterContext.getParameter().getType().getName());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to determine if TestMethodInvoker supports parameter");
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!context.getTestClass().isPresent()) {
            return ConditionEvaluationResult.enabled("No test class specified");
        }
        if (context.getTestInstance().isPresent()) {
            return ConditionEvaluationResult.enabled("Quarkus Test Profile tags only affect classes");
        }
        String tagsStr = System.getProperty("quarkus.test.profile.tags");
        if ((tagsStr == null) || tagsStr.isEmpty()) {
            return ConditionEvaluationResult.enabled("No Quarkus Test Profile tags");
        }
        Class<? extends QuarkusTestProfile> testProfile = getQuarkusTestProfile(context);
        if (testProfile == null) {
            return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                    + "' is not annotated with '@QuarkusTestProfile' but 'quarkus.profile.test.tags' was set");
        }
        QuarkusTestProfile profileInstance;
        try {
            profileInstance = testProfile.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Set<String> testProfileTags = profileInstance.tags();
        String[] tags = tagsStr.split(",");
        for (String tag : tags) {
            String trimmedTag = tag.trim();
            if (testProfileTags.contains(trimmedTag)) {
                return ConditionEvaluationResult.enabled("Tag '" + trimmedTag + "' is present on '" + testProfile
                        + "' which is used on test '" + context.getRequiredTestClass());
            }
        }
        return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                + "' disabled because 'quarkus.profile.test.tags' don't match the tags of '" + testProfile + "'");
    }

    public class ExtensionState extends QuarkusTestExtensionState {

        public ExtensionState(Closeable testResourceManager, Closeable resource) {
            super(testResourceManager, resource);
        }

        @Override
        protected void doClose() throws IOException {
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            if (runningQuarkusApplication != null) {
                Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
            }
            try {
                resource.close();
            } catch (Throwable e) {
                log.error("Failed to shutdown Quarkus", e);
            } finally {
                runningQuarkusApplication = null;
                clonePattern = null;
                try {
                    if (QuarkusTestExtension.this.originalCl != null) {
                        setCCL(QuarkusTestExtension.this.originalCl);
                    }
                    testResourceManager.close();
                } catch (Exception e) {
                    log.error("Failed to shutdown Quarkus test resources", e);
                } finally {
                    Thread.currentThread().setContextClassLoader(old);
                    ConfigProviderResolver.setInstance(null);
                }
            }
        }
    }

    class FailedCleanup implements ExtensionContext.Store.CloseableResource {

        @Override
        public void close() {
            shutdownHangDetection();
            firstException = null;
            failedBoot = false;
            ConfigProviderResolver.setInstance(null);
        }
    }

    public static class TestBuildChainFunction implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> stringObjectMap) {
            Path testLocation = (Path) stringObjectMap.get(TEST_LOCATION);
            // the index was written by the extension
            Index testClassesIndex = TestClassIndexer.readIndex(testLocation, (Class<?>) stringObjectMap.get(TEST_CLASS));

            List<Consumer<BuildChainBuilder>> allCustomizers = new ArrayList<>(1);
            Consumer<BuildChainBuilder> defaultCustomizer = new Consumer<BuildChainBuilder>() {

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
                            //we need to make sure all hot reloadable classes are application classes
                            context.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
                                @Override
                                public boolean test(String s) {
                                    QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                                            .getContextClassLoader();
                                    //if the class file is present in this (and not the parent) CL then it is an application class
                                    List<ClassPathElement> res = cl
                                            .getElementsWithResource(s.replace(".", "/") + ".class", true);
                                    return !res.isEmpty();
                                }
                            }));
                        }
                    }).produces(ApplicationClassPredicateBuildItem.class).build();
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new TestAnnotationBuildItem(QuarkusTest.class.getName()));
                        }
                    }).produces(TestAnnotationBuildItem.class)
                            .build();

                    List<String> testClassBeans = new ArrayList<>();

                    List<AnnotationInstance> extendWith = testClassesIndex
                            .getAnnotations(DotNames.EXTEND_WITH);
                    for (AnnotationInstance annotationInstance : extendWith) {
                        if (annotationInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                            continue;
                        }
                        ClassInfo classInfo = annotationInstance.target().asClass();
                        if (classInfo.isAnnotation()) {
                            continue;
                        }
                        Type[] extendsWithTypes = annotationInstance.value().asClassArray();
                        for (Type type : extendsWithTypes) {
                            if (DotNames.QUARKUS_TEST_EXTENSION.equals(type.name())) {
                                testClassBeans.add(classInfo.name().toString());
                            }
                        }
                    }

                    List<AnnotationInstance> registerExtension = testClassesIndex.getAnnotations(DotNames.REGISTER_EXTENSION);
                    for (AnnotationInstance annotationInstance : registerExtension) {
                        if (annotationInstance.target().kind() != AnnotationTarget.Kind.FIELD) {
                            continue;
                        }
                        FieldInfo fieldInfo = annotationInstance.target().asField();
                        if (DotNames.QUARKUS_TEST_EXTENSION.equals(fieldInfo.type().name())) {
                            testClassBeans.add(fieldInfo.declaringClass().name().toString());
                        }
                    }

                    if (!testClassBeans.isEmpty()) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                for (String quarkusExtendWithTestClass : testClassBeans) {
                                    context.produce(new TestClassBeanBuildItem(quarkusExtendWithTestClass));
                                }
                            }
                        }).produces(TestClassBeanBuildItem.class)
                                .build();
                    }

                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            Object testProfile = stringObjectMap.get(TEST_PROFILE);
                            if (testProfile != null) {
                                context.produce(new TestProfileBuildItem(testProfile.toString()));
                            }
                        }
                    }).produces(TestProfileBuildItem.class).build();

                }
            };
            allCustomizers.add(defaultCustomizer);

            // give other extensions the ability to customize the build chain
            for (TestBuildChainCustomizerProducer testBuildChainCustomizerProducer : ServiceLoader
                    .load(TestBuildChainCustomizerProducer.class, this.getClass().getClassLoader())) {
                allCustomizers.add(testBuildChainCustomizerProducer.produce(testClassesIndex));
            }

            return allCustomizers;
        }
    }

    private static void resetHangTimeout() {
        if (hangTaskKey != null) {
            hangTaskKey.cancel(false);
            ScheduledExecutorService h = QuarkusTestExtension.hangDetectionExecutor;
            if (h != null) {
                try {
                    hangTaskKey = h.schedule(hangDetectionTask, hangTimeout.toMillis(), TimeUnit.MILLISECONDS);
                } catch (RejectedExecutionException ignore) {

                }
            }
        }
    }
}
