package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
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
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import io.quarkus.test.junit.callback.QuarkusTestAfterConstructCallback;
import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeAllCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeClassCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.internal.DeepClone;
import io.quarkus.test.junit.internal.XStreamDeepClone;

//todo: share common core with QuarkusUnitTest
public class QuarkusTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, InvocationInterceptor, AfterAllCallback,
        ParameterResolver {

    private static final Logger log = Logger.getLogger(QuarkusTestExtension.class);

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";

    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    private static ClassLoader originalCl;
    private static RunningQuarkusApplication runningQuarkusApplication;
    private static Path testClassLocation;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static List<Object> beforeClassCallbacks;
    private static List<Object> afterConstructCallbacks;
    private static List<Object> legacyAfterConstructCallbacks;
    private static List<Object> beforeEachCallbacks;
    private static List<Object> afterEachCallbacks;
    private static Class<?> quarkusTestMethodContextClass;
    private static Class<? extends QuarkusTestProfile> quarkusTestProfile;
    private static List<Function<Class<?>, String>> testHttpEndpointProviders;

    private static DeepClone deepClone;

    private ExtensionState doJavaStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile) throws Throwable {
        quarkusTestProfile = profile;
        Closeable testResourceManager = null;
        try {
            final LinkedBlockingDeque<Runnable> shutdownTasks = new LinkedBlockingDeque<>();

            Class<?> requiredTestClass = context.getRequiredTestClass();
            testClassLocation = getTestClassesLocation(requiredTestClass);
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

            final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder()
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.TEST);
            QuarkusTestProfile profileInstance = null;
            if (profile != null) {
                profileInstance = profile.newInstance();
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
            runnerBuilder.setProjectRoot(projectRoot);
            Path outputDir;
            try {
                // this should work for both maven and gradle
                outputDir = projectRoot.resolve(projectRoot.relativize(testClassLocation).getName(0));
            } catch (Exception e) {
                // this shouldn't happen since testClassLocation is usually found under the project dir
                outputDir = projectRoot;
            }
            runnerBuilder.setTargetDirectory(outputDir);

            rootBuilder.add(appClassLocation);
            final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
            if (appResourcesLocation != null) {
                rootBuilder.add(appResourcesLocation);
            }

            // If gradle project running directly with IDE
            if (System.getProperty(BootstrapConstants.SERIALIZED_APP_MODEL) == null) {
                BuildToolHelper.enableGradleAppModelForTest(projectRoot);
            }

            runnerBuilder.setApplicationRoot(rootBuilder.build());

            CuratedApplication curatedApplication = runnerBuilder
                    .setTest(true)
                    .build()
                    .bootstrap();

            Index testClassesIndex = TestClassIndexer.indexTestClasses(requiredTestClass);
            // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
            TestClassIndexer.writeIndex(testClassesIndex, requiredTestClass);

            Timing.staticInitStarted(curatedApplication.getBaseRuntimeClassLoader());
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
                    .getConstructor(Class.class, List.class)
                    .newInstance(requiredTestClass,
                            getAdditionalTestResources(profileInstance, startupAction.getClassLoader()));
            testResourceManager.getClass().getMethod("init").invoke(testResourceManager);
            testResourceManager.getClass().getMethod("start").invoke(testResourceManager);

            populateCallbacks(startupAction.getClassLoader());

            runningQuarkusApplication = startupAction.run();

            ConfigProviderResolver.setInstance(new RunningAppConfigResolver(runningQuarkusApplication));

            System.setProperty("test.url", TestHTTPResourceManager.getUri(runningQuarkusApplication));

            Closeable tm = testResourceManager;
            Closeable shutdownTask = new Closeable() {
                @Override
                public void close() throws IOException {
                    try {
                        runningQuarkusApplication.close();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        try {
                            while (!shutdownTasks.isEmpty()) {
                                shutdownTasks.pop().run();
                            }
                        } finally {
                            for (Map.Entry<String, String> entry : sysPropRestore.entrySet()) {
                                String val = entry.getValue();
                                if (val == null) {
                                    System.clearProperty(entry.getKey());
                                } else {
                                    System.setProperty(entry.getKey(), val);
                                }
                            }
                            tm.close();
                        }
                        try {
                            TestClassIndexer.removeIndex(requiredTestClass);
                        } catch (Exception ignored) {
                        }
                    }
                }
            };
            ExtensionState state = new ExtensionState(testResourceManager, shutdownTask);
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    state.close();
                }
            }, "Quarkus Test Cleanup Shutdown task"));
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

    /**
     * Since {@link TestResourceManager} is loaded from the ClassLoader passed in as an argument,
     * we need to convert the user input {@link QuarkusTestProfile.TestResourceEntry} into instances of
     * {@link TestResourceManager.TestResourceClassEntry}
     * that are loaded from that ClassLoader
     */
    private List<Object> getAdditionalTestResources(
            QuarkusTestProfile profileInstance, ClassLoader classLoader) {
        if ((profileInstance == null) || profileInstance.testResources().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Constructor<?> testResourceClassEntryConstructor = Class
                    .forName(TestResourceManager.TestResourceClassEntry.class.getName(), true, classLoader)
                    .getConstructor(Class.class, Map.class);

            List<QuarkusTestProfile.TestResourceEntry> testResources = profileInstance.testResources();
            List<Object> result = new ArrayList<>(testResources.size());
            for (QuarkusTestProfile.TestResourceEntry testResource : testResources) {
                Object instance = testResourceClassEntryConstructor.newInstance(
                        Class.forName(testResource.getClazz().getName(), true, classLoader), testResource.getArgs());
                result.add(instance);
            }

            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to handle profile " + profileInstance.getClass());
        }
    }

    // keep it super simple for now, but we might need multiple strategies in the future
    private void populateDeepCloneField(StartupAction startupAction) {
        deepClone = new XStreamDeepClone(startupAction.getClassLoader());
    }

    private void populateCallbacks(ClassLoader classLoader) throws ClassNotFoundException {
        // make sure that we start over everytime we populate the callbacks
        // otherwise previous runs of QuarkusTest (with different TestProfile values can leak into the new run)
        quarkusTestMethodContextClass = null;
        beforeClassCallbacks = new ArrayList<>();
        afterConstructCallbacks = new ArrayList<>();
        legacyAfterConstructCallbacks = new ArrayList<>();
        beforeEachCallbacks = new ArrayList<>();
        afterEachCallbacks = new ArrayList<>();

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
        ServiceLoader<?> quarkusTestLegacyAfterConstructLoader = ServiceLoader
                .load(Class.forName(QuarkusTestBeforeAllCallback.class.getName(), false, classLoader), classLoader);
        for (Object quarkusTestLegacyAfterConstructCallback : quarkusTestLegacyAfterConstructLoader) {
            legacyAfterConstructCallbacks.add(quarkusTestLegacyAfterConstructCallback);
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
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (isNativeTest(context)) {
            return;
        }
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
        if (isNativeTest(context)) {
            return;
        }
        if (!failedBoot) {
            popMockContext();
            for (Object afterEachCallback : afterEachCallbacks) {
                Map.Entry<Class<?>, ?> tuple = createQuarkusTestMethodContextTuple(context);
                afterEachCallback.getClass().getMethod("afterEach", tuple.getKey())
                        .invoke(afterEachCallback, tuple.getValue());
            }
            ClassLoader original = setCCL(runningQuarkusApplication.getClassLoader());
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

        Constructor<?> constructor = quarkusTestMethodContextClass.getConstructor(Object.class, Method.class);
        return new AbstractMap.SimpleEntry<>(quarkusTestMethodContextClass,
                constructor.newInstance(actualTestInstance, actualTestMethod));
    }

    private boolean isNativeTest(ExtensionContext context) {
        return context.getRequiredTestClass().isAnnotationPresent(NativeImageTest.class);
    }

    private ExtensionState ensureStarted(ExtensionContext extensionContext) {
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        TestProfile annotation = extensionContext.getRequiredTestClass().getAnnotation(TestProfile.class);
        Class<? extends QuarkusTestProfile> selectedProfile = null;
        if (annotation != null) {
            selectedProfile = annotation.value();
        }
        boolean wrongProfile = !Objects.equals(selectedProfile, quarkusTestProfile);
        if ((state == null && !failedBoot) || wrongProfile) {
            if (wrongProfile) {
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
        if (isNativeTest(context)) {
            return;
        }
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
        if (isNativeTest(extensionContext)) {
            invocation.proceed();
            return;
        }
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
        if (isNativeTest(extensionContext)) {
            return invocation.proceed();
        }
        ExtensionState state = ensureStarted(extensionContext);
        if (failedBoot) {
            throwBootFailureException();
            return null;
        }
        T result;
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Class<?> requiredTestClass = extensionContext.getRequiredTestClass();
        for (Object beforeClassCallback : beforeClassCallbacks) {
            beforeClassCallback.getClass().getMethod("beforeClass", Class.class).invoke(beforeClassCallback,
                    requiredTestClass);
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

            actualTestInstance = runningQuarkusApplication.instance(actualTestClass);

            Class<?> resM = Thread.currentThread().getContextClassLoader().loadClass(TestHTTPResourceManager.class.getName());
            resM.getDeclaredMethod("inject", Object.class, List.class).invoke(null, actualTestInstance,
                    testHttpEndpointProviders);
            state.testResourceManager.getClass().getMethod("inject", Object.class).invoke(state.testResourceManager,
                    actualTestInstance);
            for (Object afterConstructCallback : afterConstructCallbacks) {
                afterConstructCallback.getClass().getMethod("afterConstruct", Object.class).invoke(afterConstructCallback,
                        actualTestInstance);
            }
            for (Object legacyAfterConstructCallback : legacyAfterConstructCallbacks) {
                legacyAfterConstructCallback.getClass().getMethod("beforeAll", Object.class)
                        .invoke(legacyAfterConstructCallback, actualTestInstance);
            }
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T interceptTestFactoryMethod(Invocation<T> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (isNativeTest(extensionContext)) {
            return invocation.proceed();
        }
        T result = (T) runExtensionMethod(invocationContext, extensionContext);
        invocation.skip();
        return result;
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

    private Object runExtensionMethod(ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
            throws Throwable {
        Method newMethod = null;

        ClassLoader old = setCCL(runningQuarkusApplication.getClassLoader());
        try {
            Class<?> c = Class.forName(extensionContext.getRequiredTestClass().getName(), true,
                    Thread.currentThread().getContextClassLoader());
            while (c != Object.class) {
                if (c.getName().equals(invocationContext.getExecutable().getDeclaringClass().getName())) {
                    try {
                        Class<?>[] originalParameterTypes = invocationContext.getExecutable().getParameterTypes();
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
                        newMethod = c.getDeclaredMethod(invocationContext.getExecutable().getName(),
                                parameterTypesFromTccl.toArray(new Class[0]));
                        break;
                    } catch (NoSuchMethodException ignored) {

                    }
                }
                c = c.getSuperclass();
            }
            if (newMethod == null) {
                throw new RuntimeException("Could not find method " + invocationContext.getExecutable() + " on test class");
            }
            newMethod.setAccessible(true);

            // the arguments were not loaded from TCCL so we need to deep clone them into the TCCL
            // because the test method runs from a class loaded from the TCCL
            List<Object> originalArguments = invocationContext.getArguments();
            List<Object> argumentsFromTccl = new ArrayList<>();
            for (Object arg : originalArguments) {
                argumentsFromTccl.add(deepClone.clone(arg));
            }

            return newMethod.invoke(actualTestInstance, argumentsFromTccl.toArray(new Object[0]));
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            setCCL(old);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (!isNativeTest(context) && (runningQuarkusApplication != null)) {
            popMockContext();
        }
        if (originalCl != null) {
            setCCL(originalCl);
        }
    }

    /**
     * Return true if we need a parameter for constructor injection
     */
    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getDeclaringExecutable() instanceof Constructor;
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

    class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final Closeable testResourceManager;
        private final Closeable resource;
        private final AtomicBoolean closed = new AtomicBoolean();

        ExtensionState(Closeable testResourceManager, Closeable resource) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
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
                    try {
                        if (QuarkusTestExtension.this.originalCl != null) {
                            setCCL(QuarkusTestExtension.this.originalCl);
                        }
                        testResourceManager.close();
                    } catch (IOException e) {
                        log.error("Failed to shutdown Quarkus test resources", e);
                    } finally {
                        Thread.currentThread().setContextClassLoader(old);
                    }
                }
            }
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

                private static final int ANNOTATION = 0x00002000;

                boolean isAnnotation(final int mod) {
                    return (mod & ANNOTATION) != 0;
                }

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

                    List<String> testClassBeans = new ArrayList<>();

                    List<AnnotationInstance> extendWith = testClassesIndex
                            .getAnnotations(DotNames.EXTEND_WITH);
                    for (AnnotationInstance annotationInstance : extendWith) {
                        if (annotationInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                            continue;
                        }
                        ClassInfo classInfo = annotationInstance.target().asClass();
                        if (isAnnotation(classInfo.flags())) {
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
}
