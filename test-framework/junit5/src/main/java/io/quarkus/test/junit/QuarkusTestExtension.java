package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;

import java.io.Closeable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
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
import java.util.stream.Collectors;

import javax.enterprise.inject.Alternative;

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
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
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

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.model.gradle.QuarkusModel;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.util.PathsUtils;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.deployment.dev.testing.CurrentTestApplication;
import io.quarkus.dev.testing.TracingHandler;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.GroovyCacheCleaner;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.callback.QuarkusTestAfterAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestContext;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.internal.DeepClone;
import io.quarkus.test.junit.internal.SerializationWithXStreamFallbackDeepClone;

public class QuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, InvocationInterceptor, AfterAllCallback,
        ParameterResolver, ExecutionCondition {

    private static final Logger log = Logger.getLogger(QuarkusTestExtension.class);

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    public static final String QUARKUS_TEST_HANG_DETECTION_TIMEOUT = "quarkus.test.hang-detection-timeout";
    public static final String IO_QUARKUS_TESTING_TYPE = "io.quarkus.testing.type";

    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    // needed for @Nested
    private static Object outerInstance;
    private static ClassLoader originalCl;
    private static RunningQuarkusApplication runningQuarkusApplication;
    private static Pattern clonePattern;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static List<Object> beforeClassCallbacks;
    private static List<Object> afterConstructCallbacks;
    private static List<Object> beforeEachCallbacks;
    private static List<Object> afterEachCallbacks;
    private static List<Object> afterAllCallbacks;
    private static Class<?> quarkusTestMethodContextClass;
    private static Class<? extends QuarkusTestProfile> quarkusTestProfile;
    private static boolean hasPerTestResources;
    private static Class<?> currentJUnitTestClass;
    private static List<Function<Class<?>, String>> testHttpEndpointProviders;

    private static List<Object> testMethodInvokers;

    private static DeepClone deepClone;
    //needed for @Nested
    private static final Deque<Class<?>> currentTestClassStack = new ArrayDeque<>();
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
            System.err.println("A stack track is below to help diagnose the potential hang");
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
        currentJUnitTestClass = context.getRequiredTestClass();
        Closeable testResourceManager = null;
        try {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();

            Class<?> requiredTestClass = context.getRequiredTestClass();
            Path testClassLocation = getTestClassesLocation(requiredTestClass);
            final Path appClassLocation = getAppClassLocationForTestLocation(testClassLocation.toString());

            PathsCollection.Builder rootBuilder = PathsCollection.builder();

            if (!appClassLocation.equals(testClassLocation)) {
                rootBuilder.add(testClassLocation);
                // if test classes is a dir, we should also check whether test resources dir exists as a separate dir (gradle)
                // TODO: this whole app/test path resolution logic is pretty dumb, it needs be re-worked using proper workspace discovery
                final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(testClassLocation, "test");
                if (testResourcesLocation != null) {
                    rootBuilder.add(testResourcesLocation);
                }
            }

            originalCl = Thread.currentThread().getContextClassLoader();
            Map<String, String> sysPropRestore = new HashMap<>();
            sysPropRestore.put(ProfileManager.QUARKUS_TEST_PROFILE_PROP,
                    System.getProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP));

            // clear the test.url system property as the value leaks into the run when using different profiles
            System.clearProperty("test.url");

            QuarkusTestProfile profileInstance = null;
            if (profile != null) {
                profileInstance = profile.getConstructor().newInstance();
                Map<String, String> additional = new HashMap<>(profileInstance.getConfigOverrides());
                if (!profileInstance.getEnabledAlternatives().isEmpty()) {
                    additional.put("quarkus.arc.selected-alternatives", profileInstance.getEnabledAlternatives().stream()
                            .peek((c) -> {
                                if (!c.isAnnotationPresent(Alternative.class)) {
                                    throw new RuntimeException(
                                            "Enabled alternative " + c + " is not annotated with @Alternative");
                                }
                            })
                            .map(Class::getName).collect(Collectors.joining(",")));
                }
                if (profileInstance.disableApplicationLifecycleObservers()) {
                    additional.put("quarkus.arc.test.disable-application-lifecycle-observers", "true");
                }
                if (profileInstance.getConfigProfile() != null) {
                    System.setProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP, profileInstance.getConfigProfile());
                }
                //we just use system properties for now
                //its a lot simpler
                for (Map.Entry<String, String> i : additional.entrySet()) {
                    sysPropRestore.put(i.getKey(), System.getProperty(i.getKey()));
                }
                for (Map.Entry<String, String> i : additional.entrySet()) {
                    System.setProperty(i.getKey(), i.getValue());
                }
            }

            final Path projectRoot = Paths.get("").normalize().toAbsolutePath();
            Path outputDir;
            try {
                // this should work for both maven and gradle
                outputDir = projectRoot.resolve(projectRoot.relativize(testClassLocation).getName(0));
            } catch (Exception e) {
                // this shouldn't happen since testClassLocation is usually found under the project dir
                outputDir = projectRoot;
            }

            rootBuilder.add(appClassLocation);
            final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
            if (appResourcesLocation != null) {
                rootBuilder.add(appResourcesLocation);
            }

            // If gradle project running directly with IDE
            if (System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null) {
                QuarkusModel model = BuildToolHelper.enableGradleAppModelForTest(projectRoot);
                if (model != null) {
                    final PathsCollection classDirectories = PathsUtils
                            .toPathsCollection(model.getWorkspace().getMainModule().getSourceSet()
                                    .getSourceDirectories());
                    for (Path classes : classDirectories) {
                        if (Files.exists(classes) && !rootBuilder.contains(classes)) {
                            rootBuilder.add(classes);
                        }
                    }
                }
            } else if (System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR) != null) {
                final String[] sourceDirectories = System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR).split(",");
                for (String sourceDirectory : sourceDirectories) {
                    final Path directory = Paths.get(sourceDirectory);
                    if (Files.exists(directory) && !rootBuilder.contains(directory)) {
                        rootBuilder.add(directory);
                    }
                }
            }
            CuratedApplication curatedApplication;
            if (CurrentTestApplication.curatedApplication != null) {
                curatedApplication = CurrentTestApplication.curatedApplication;
            } else {
                final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder()
                        .setIsolateDeployment(true)
                        .setMode(QuarkusBootstrap.Mode.TEST);
                runnerBuilder.setTargetDirectory(outputDir);
                runnerBuilder.setProjectRoot(projectRoot);
                runnerBuilder.setApplicationRoot(rootBuilder.build());

                curatedApplication = runnerBuilder
                        .setTest(true)
                        .build()
                        .bootstrap();
                shutdownTasks.add(curatedApplication::close);
            }

            if (curatedApplication.getAppModel().getUserDependencies().isEmpty()) {
                throw new RuntimeException(
                        "The tests were run against a directory that does not contain a Quarkus project. Please ensure that the test is configured to use the proper working directory.");
            }

            Index testClassesIndex = TestClassIndexer.indexTestClasses(requiredTestClass);
            // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
            TestClassIndexer.writeIndex(testClassesIndex, requiredTestClass);

            Timing.staticInitStarted(curatedApplication.getBaseRuntimeClassLoader(),
                    curatedApplication.getQuarkusBootstrap().isAuxiliaryApplication());
            final Map<String, Object> props = new HashMap<>();
            props.put(TEST_LOCATION, testClassLocation);
            props.put(TEST_CLASS, requiredTestClass);
            AugmentAction augmentAction = curatedApplication
                    .createAugmentor(TestBuildChainFunction.class.getName(), props);
            testHttpEndpointProviders = TestHttpEndpointProvider.load();
            StartupAction startupAction = augmentAction.createInitialRuntimeApplication();
            Thread.currentThread().setContextClassLoader(startupAction.getClassLoader());
            populateDeepCloneField(startupAction);

            //must be done after the TCCL has been set
            testResourceManager = (Closeable) startupAction.getClassLoader().loadClass(TestResourceManager.class.getName())
                    .getConstructor(Class.class, Class.class, List.class, boolean.class)
                    .newInstance(requiredTestClass,
                            profile != null ? profile : null,
                            getAdditionalTestResources(profileInstance, startupAction.getClassLoader()),
                            profileInstance != null && profileInstance.disableGlobalTestResources());
            testResourceManager.getClass().getMethod("init").invoke(testResourceManager);
            Map<String, String> properties = (Map<String, String>) testResourceManager.getClass().getMethod("start")
                    .invoke(testResourceManager);
            startupAction.overrideConfig(properties);
            hasPerTestResources = (boolean) testResourceManager.getClass().getMethod("hasPerTestResources")
                    .invoke(testResourceManager);

            populateCallbacks(startupAction.getClassLoader());
            populateTestMethodInvokers(startupAction.getClassLoader());

            runningQuarkusApplication = startupAction.run();
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

            System.setProperty("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication));

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
                                for (Map.Entry<String, String> entry : sysPropRestore.entrySet()) {
                                    String val = entry.getValue();
                                    if (val == null) {
                                        System.clearProperty(entry.getKey());
                                    } else {
                                        System.setProperty(entry.getKey(), val);
                                    }
                                }
                                tm.close();
                            } finally {
                                GroovyCacheCleaner.clearGroovyCache();
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

            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            if (originalCl != null) {
                Thread.currentThread().setContextClassLoader(originalCl);
            }
        }
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

    private void populateCallbacks(ClassLoader classLoader) throws ClassNotFoundException {
        // make sure that we start over everytime we populate the callbacks
        // otherwise previous runs of QuarkusTest (with different TestProfile values can leak into the new run)
        quarkusTestMethodContextClass = null;
        beforeClassCallbacks = new ArrayList<>();
        afterConstructCallbacks = new ArrayList<>();
        beforeEachCallbacks = new ArrayList<>();
        afterEachCallbacks = new ArrayList<>();
        afterAllCallbacks = new ArrayList<>();

        ServiceLoader<?> quarkusTestBeforeClassLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeClassCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestBeforeClassCallback : quarkusTestBeforeClassLoader) {
            beforeClassCallbacks.add(quarkusTestBeforeClassCallback);
        }
        ServiceLoader<?> quarkusTestAfterConstructLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterConstructCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterConstructCallback : quarkusTestAfterConstructLoader) {
            afterConstructCallbacks.add(quarkusTestAfterConstructCallback);
        }
        ServiceLoader<?> quarkusTestBeforeEachLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeEachCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestBeforeEachCallback : quarkusTestBeforeEachLoader) {
            beforeEachCallbacks.add(quarkusTestBeforeEachCallback);
        }
        ServiceLoader<?> quarkusTestAfterEachLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterEachCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterEach : quarkusTestAfterEachLoader) {
            afterEachCallbacks.add(quarkusTestAfterEach);
        }
        ServiceLoader<?> quarkusTestAfterAllLoader = ServiceLoader
                .load(Class.forName(QuarkusTestAfterAllCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestAfterAll : quarkusTestAfterAllLoader) {
            afterAllCallbacks.add(quarkusTestAfterAll);
        }
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
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass())) {
            return;
        }
        resetHangTimeout();
        if (!failedBoot) {
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
            try {
                pushMockContext();
                for (Object beforeEachCallback : beforeEachCallbacks) {
                    Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                    beforeEachCallback.getClass().getMethod("beforeEach", tuple.getKey())
                            .invoke(beforeEachCallback, tuple.getValue());
                }
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
        return endpointPath;
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
            for (Object afterEachCallback : afterEachCallbacks) {
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                afterEachCallback.getClass().getMethod("afterEach", tuple.getKey())
                        .invoke(afterEachCallback, tuple.getValue());
            }
            try {
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
        Class<?> c = actualTestClass;
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
        while (c != Object.class) {
            try {
                actualTestMethod = c.getDeclaredMethod(originalTestMethod.getName(), parameterTypes);
                break;
            } catch (NoSuchMethodException ignored) {

            }
            c = c.getSuperclass();
        }
        if (actualTestMethod == null) {
            throw new RuntimeException("Could not find method " + originalTestMethod + " on test class");
        }

        Constructor<?> constructor = quarkusTestMethodContextClass.getConstructor(Object.class, Object.class, Method.class);
        return new AbstractMap.SimpleEntry<>(quarkusTestMethodContextClass,
                constructor.newInstance(actualTestInstance, outerInstance, actualTestMethod));
    }

    private boolean isNativeOrIntegrationTest(Class<?> clazz) {
        for (Class<?> i : currentTestClassStack) {
            if (i.isAnnotationPresent(NativeImageTest.class) || i.isAnnotationPresent(QuarkusIntegrationTest.class)) {
                return true;
            }
        }
        if (clazz.isAnnotationPresent(NativeImageTest.class) || clazz.isAnnotationPresent(QuarkusIntegrationTest.class)) {
            return true;
        }
        return false;
    }

    private ExtensionState ensureStarted(ExtensionContext extensionContext) {
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        Class<?> testType = store.get(IO_QUARKUS_TESTING_TYPE, Class.class);
        if (testType != null) {
            if (testType != QuarkusTest.class) {
                throw new IllegalStateException(
                        "Cannot mix both @QuarkusTest based tests and " + testType.getName() + " based tests in the same run");
            }
        } else {
            store.put(IO_QUARKUS_TESTING_TYPE, QuarkusTest.class);
        }
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        Class<? extends QuarkusTestProfile> selectedProfile = getQuarkusTestProfile(extensionContext);
        boolean wrongProfile = !Objects.equals(selectedProfile, quarkusTestProfile);
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean reloadTestResources = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass)
                && (hasPerTestResources || hasPerTestResources(extensionContext));
        if ((state == null && !failedBoot) || wrongProfile || reloadTestResources) {
            if (wrongProfile || reloadTestResources) {
                if (state != null) {
                    try {
                        state.close();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
            PropertyTestUtil.setLogFileProperty();
            try {
                state = doJavaStart(extensionContext, selectedProfile);
                store.put(ExtensionState.class.getName(), state);

            } catch (Throwable e) {
                failedBoot = true;
                firstException = e;
                store.put(FailedCleanup.class.getName(), new FailedCleanup());
            }
        }
        return state;
    }

    private Class<? extends QuarkusTestProfile> getQuarkusTestProfile(ExtensionContext extensionContext) {
        TestProfile annotation = extensionContext.getRequiredTestClass().getAnnotation(TestProfile.class);
        Class<? extends QuarkusTestProfile> selectedProfile = null;
        if (annotation != null) {
            selectedProfile = annotation.value();
        }
        return selectedProfile;
    }

    private static ClassLoader setCCL(ClassLoader cl) {
        final Thread thread = Thread.currentThread();
        final ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(cl);
        return original;
    }

    private void throwBootFailureException() throws Exception {
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
        currentTestClassStack.push(context.getRequiredTestClass());
        //set the right launch mode in the outer CL, used by the HTTP host config source
        ProfileManager.setLaunchMode(LaunchMode.TEST);
        if (isNativeOrIntegrationTest(context.getRequiredTestClass())) {
            return;
        }
        resetHangTimeout();
        ensureStarted(context);
        if (runningQuarkusApplication != null) {
            pushMockContext();
        }
    }

    private void pushMockContext() {
        try {
            //classloader issues
            Method pushContext = runningQuarkusApplication.getClassLoader().loadClass(MockSupport.class.getName())
                    .getDeclaredMethod("pushContext");
            pushContext.setAccessible(true);
            pushContext
                    .invoke(null);
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
            popContext
                    .invoke(null);
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
        ExtensionState state = ensureStarted(extensionContext);
        if (failedBoot) {
            throwBootFailureException();
            return null;
        }
        T result;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Class<?> requiredTestClass = extensionContext.getRequiredTestClass();

        if (runningQuarkusApplication != null) {
            try {
                Thread.currentThread().setContextClassLoader(runningQuarkusApplication.getClassLoader());
                for (Object beforeClassCallback : beforeClassCallbacks) {
                    beforeClassCallback.getClass().getMethod("beforeClass", Class.class).invoke(beforeClassCallback,
                            runningQuarkusApplication.getClassLoader().loadClass(requiredTestClass.getName()));
                }
            } finally {
                Thread.currentThread().setContextClassLoader(old);
            }
        } else {
            // can this ever happen?
            for (Object beforeClassCallback : beforeClassCallbacks) {
                beforeClassCallback.getClass().getMethod("beforeClass", Class.class).invoke(beforeClassCallback,
                        requiredTestClass);
            }
        }

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

        initTestState(extensionContext, state);
        if (old != null) {
            setCCL(old);
        }
        return result;
    }

    private void initTestState(ExtensionContext extensionContext, ExtensionState state) {
        try {
            Class<?> previousActualTestClass = actualTestClass;
            actualTestClass = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());
            outerInstance = null;
            if (extensionContext.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
                Class<?> outerClass = actualTestClass.getEnclosingClass();
                outerInstance = runningQuarkusApplication.instance(outerClass);
                Constructor<?> declaredConstructor = actualTestClass.getDeclaredConstructor(outerClass);
                declaredConstructor.setAccessible(true);
                actualTestInstance = declaredConstructor.newInstance(outerInstance);
            } else {
                actualTestInstance = runningQuarkusApplication.instance(actualTestClass);
            }

            Class<?> resM = Thread.currentThread().getContextClassLoader().loadClass(TestHTTPResourceManager.class.getName());
            resM.getDeclaredMethod("inject", Object.class, List.class).invoke(null, actualTestInstance,
                    testHttpEndpointProviders);
            state.testResourceManager.getClass().getMethod("inject", Object.class).invoke(state.testResourceManager,
                    actualTestInstance);
            for (Object afterConstructCallback : afterConstructCallbacks) {
                afterConstructCallback.getClass().getMethod("afterConstruct", Object.class).invoke(afterConstructCallback,
                        actualTestInstance);
            }
            if (outerInstance != null) {
                for (Object afterConstructCallback : afterConstructCallbacks) {
                    afterConstructCallback.getClass().getMethod("afterConstruct", Object.class).invoke(afterConstructCallback,
                            outerInstance);
                }
            }
        } catch (Exception e) {
            throw new TestInstantiationException("Failed to create test instance", e);
        }
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
        runExtensionMethod(invocationContext, extensionContext, true);
        invocation.skip();
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
            Method newMethod = determineTCCLExtensionMethod(invocationContext.getExecutable(), testClassFromTCCL);
            boolean methodFromEnclosing = false;
            // this is needed to support before*** and after*** methods that are part of class that encloses the test class
            // (the test class is in this case a @Nested test)
            if ((newMethod == null) && (testClassFromTCCL.getEnclosingClass() != null)) {
                testClassFromTCCL = testClassFromTCCL.getEnclosingClass();
                newMethod = determineTCCLExtensionMethod(invocationContext.getExecutable(), testClassFromTCCL);
                methodFromEnclosing = true;
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
            for (int i = 0; i < originalArguments.size(); i++) {
                Object arg = originalArguments.get(i);
                boolean cloneRequired = false;
                Object replacement = null;
                Class<?> argClass = invocationContext.getExecutable().getParameters()[i].getType();
                if (arg != null) {
                    Class<?> theclass = argClass;
                    while (theclass.isArray()) {
                        theclass = theclass.getComponentType();
                    }
                    String className = theclass.getName();
                    if (theclass.isPrimitive()) {
                        cloneRequired = false;
                    } else if (TestInfo.class.isAssignableFrom(theclass)) {
                        TestInfo info = (TestInfo) arg;
                        Method newTestMethod = info.getTestMethod().isPresent()
                                ? determineTCCLExtensionMethod(info.getTestMethod().get(), testClassFromTCCL)
                                : null;
                        replacement = new TestInfoImpl(info.getDisplayName(), info.getTags(), Optional.of(testClassFromTCCL),
                                Optional.ofNullable(newTestMethod));
                    } else if (clonePattern.matcher(className).matches()) {
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

            Object effectiveTestInstance = actualTestInstance;
            if (methodFromEnclosing) {
                // TODO: this is a little dodgy, ideally we would need to use the same constructor that was used for the original object
                // but it's unlikely(?) we will run into this combo
                effectiveTestInstance = testClassFromTCCL.getConstructor().newInstance();
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

        Method newMethod = null;
        while (c != Object.class) {
            if (c.getName().equals(originalMethod.getDeclaringClass().getName())) {
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
                    newMethod = c.getDeclaredMethod(originalMethod.getName(),
                            parameterTypesFromTccl.toArray(new Class[0]));
                    break;
                } catch (NoSuchMethodException ignored) {

                }
            }
            c = c.getSuperclass();
        }
        return newMethod;
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
            outerInstance = null;
        }
    }

    private void runAfterAllCallbacks(ExtensionContext context) throws Exception {
        if (isNativeOrIntegrationTest(context.getRequiredTestClass()) || failedBoot) {
            return;
        }
        if (afterAllCallbacks.isEmpty()) {
            return;
        }

        Class<?> quarkusTestContextClass = Class.forName(QuarkusTestContext.class.getName(), true,
                runningQuarkusApplication.getClassLoader());
        Object quarkusTestContextInstance = quarkusTestContextClass.getConstructor(Object.class, Object.class)
                .newInstance(actualTestInstance, outerInstance);

        ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
        try {
            for (Object afterAllCallback : afterAllCallbacks) {
                afterAllCallback.getClass().getMethod("afterAll", quarkusTestContextClass)
                        .invoke(afterAllCallback, quarkusTestContextInstance);
            }
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

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final Closeable testResourceManager;
        private final Closeable resource;
        private final AtomicBoolean closed = new AtomicBoolean();
        private final Thread shutdownHook;

        ExtensionState(Closeable testResourceManager, Closeable resource) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
            this.shutdownHook = new Thread(new Runnable() {
                @Override
                public void run() {
                    ExtensionState.this.close();
                }
            }, "Quarkus Test Cleanup Shutdown task");
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
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
                    } catch (IOException e) {
                        log.error("Failed to shutdown Quarkus test resources", e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(old);
                        ConfigProviderResolver.setInstance(null);
                    }
                }
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (Throwable t) {
                    //won't work if we are already shutting down
                }
            }
        }
    }

    class FailedCleanup implements ExtensionContext.Store.CloseableResource {

        @Override
        public void close() {
            resetHangTimeout();
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
            Index testClassesIndex = TestClassIndexer.readIndex((Class<?>) stringObjectMap.get(TEST_CLASS));

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

    static boolean hasPerTestResources(ExtensionContext extensionContext) {
        return hasPerTestResources(extensionContext.getRequiredTestClass());
    }

    public static boolean hasPerTestResources(Class<?> requiredTestClass) {
        while (requiredTestClass != Object.class) {
            for (QuarkusTestResource testResource : requiredTestClass.getAnnotationsByType(QuarkusTestResource.class)) {
                if (testResource.restrictToAnnotatedClass()) {
                    return true;
                }
            }
            // scan for meta-annotations
            for (Annotation annotation : requiredTestClass.getAnnotations()) {
                // skip TestResource annotations
                if (annotation.annotationType() != QuarkusTestResource.class) {
                    // look for a TestResource on the annotation itself
                    if (annotation.annotationType().getAnnotationsByType(QuarkusTestResource.class).length > 0) {
                        // meta-annotations are per-test scoped for now
                        return true;
                    }
                }
            }
            // look up
            requiredTestClass = requiredTestClass.getSuperclass();
        }
        return false;
    }
}
