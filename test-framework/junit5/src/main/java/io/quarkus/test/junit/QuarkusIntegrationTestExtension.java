package io.quarkus.test.junit;

import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.determineBuildOutputDirectory;
import static io.quarkus.test.junit.IntegrationTestUtil.determineTestProfileAndProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.doProcessTestInstance;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.findProfile;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;
import static io.quarkus.test.junit.IntegrationTestUtil.getArtifactType;
import static io.quarkus.test.junit.IntegrationTestUtil.getSysPropsToRestore;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevServices;
import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.startLauncher;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.LauncherUtil;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.RunCommandLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.quarkus.test.common.TestHostLauncher;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.launcher.ArtifactLauncherProvider;

public class QuarkusIntegrationTestExtension extends AbstractQuarkusTestWithContextExtension
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeEachCallback, AfterEachCallback,
        BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor {

    private static final String ENABLED_CALLBACKS_PROPERTY = "quarkus.test.enable-callbacks-for-integration-tests";

    private static boolean failedBoot;

    private static List<Function<Class<?>, String>> testHttpEndpointProviders;
    private static boolean ssl;

    private static Class<? extends QuarkusTestProfile> quarkusTestProfile;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static Class<?> currentJUnitTestClass;
    private static boolean hasPerTestResources;

    private static Map<String, String> devServicesProps;
    private static String containerNetworkId;

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (!failedBoot && !isAfterTestCallbacksEmpty()) {
            invokeAfterTestExecutionCallbacks(createQuarkusTestMethodContext(context));
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            if (!isAfterEachCallbacksEmpty()) {
                invokeAfterEachCallbacks(createQuarkusTestMethodContext(context));
            }

            RestAssuredURLManager.clearURL();
            TestScopeManager.tearDown(true);
        }
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            if (!isBeforeTestCallbacksEmpty()) {
                invokeBeforeTestExecutionCallbacks(createQuarkusTestMethodContext(context));
            }

        } else {
            throwBootFailureException();
            return;
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (failedBoot) {
            throwBootFailureException();
        } else {
            if (!isBeforeEachCallbacksEmpty()) {
                invokeBeforeEachCallbacks(createQuarkusTestMethodContext(context));
            }

            RestAssuredURLManager.setURL(ssl, QuarkusTestExtension.getEndpointPath(context, testHttpEndpointProviders));
            TestScopeManager.setup(true);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        ensureStarted(context);
        invokeBeforeClassCallbacks(context.getRequiredTestClass());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (!failedBoot && !isAfterAllCallbacksEmpty()) {
            invokeAfterAllCallbacks(createQuarkusTestMethodContext(context));
        }
    }

    private QuarkusTestExtensionState ensureStarted(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass);
        Properties quarkusArtifactProperties = readQuarkusArtifactProperties(extensionContext);

        QuarkusTestExtensionState state = getState(extensionContext);
        Class<? extends QuarkusTestProfile> selectedProfile = findProfile(testClass);
        boolean wrongProfile = !Objects.equals(selectedProfile, quarkusTestProfile);
        // we reset the failed state if we changed test class
        boolean isNewTestClass = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass);
        if (isNewTestClass && state != null) {
            state.setTestFailed(null);
            currentJUnitTestClass = extensionContext.getRequiredTestClass();
        }
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean reloadTestResources = isNewTestClass
                && (hasPerTestResources || QuarkusTestExtension.hasPerTestResources(extensionContext));
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
            try {
                state = doProcessStart(quarkusArtifactProperties, selectedProfile, extensionContext);
                setState(extensionContext, state);
            } catch (Throwable e) {
                try {
                    Path appLogPath = PropertyTestUtil.getLogFilePath();
                    File appLogFile = appLogPath.toFile();
                    if (appLogFile.exists() && (appLogFile.length() > 0)) {
                        System.err.println("Failed to launch the application. The application logs can be found at: "
                                + appLogPath.toAbsolutePath());
                    }
                } catch (IllegalStateException ignored) {

                }

                failedBoot = true;
                firstException = e;
            }
        }
        return state;
    }

    private QuarkusTestExtensionState doProcessStart(Properties quarkusArtifactProperties,
            Class<? extends QuarkusTestProfile> profile, ExtensionContext context)
            throws Throwable {
        JBossVersion.disableVersionLogging();

        String artifactType = getArtifactType(quarkusArtifactProperties);

        boolean isDockerLaunch = isContainer(artifactType);

        ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult = handleDevServices(context,
                isDockerLaunch);
        devServicesProps = devServicesLaunchResult.properties();
        containerNetworkId = devServicesLaunchResult.networkId();
        quarkusTestProfile = profile;
        currentJUnitTestClass = context.getRequiredTestClass();
        TestResourceManager testResourceManager = null;
        try {
            Class<?> requiredTestClass = context.getRequiredTestClass();

            Map<String, String> sysPropRestore = getSysPropsToRestore();
            for (String devServicesProp : devServicesProps.keySet()) {
                sysPropRestore.put(devServicesProp, null); // used to signal that the property needs to be cleared
            }
            TestProfileAndProperties testProfileAndProperties = determineTestProfileAndProperties(profile, sysPropRestore);

            testResourceManager = new TestResourceManager(requiredTestClass, quarkusTestProfile,
                    getAdditionalTestResources(testProfileAndProperties.testProfile,
                            context.getRequiredTestClass().getClassLoader()),
                    testProfileAndProperties.testProfile != null
                            && testProfileAndProperties.testProfile.disableGlobalTestResources(),
                    devServicesProps, containerNetworkId == null ? Optional.empty() : Optional.of(containerNetworkId));
            testResourceManager.init(
                    testProfileAndProperties.testProfile != null ? testProfileAndProperties.testProfile.getClass().getName()
                            : null);
            hasPerTestResources = testResourceManager.hasPerTestResources();
            if (isCallbacksEnabledForIntegrationTests()) {
                populateCallbacks(requiredTestClass.getClassLoader());
            }

            Map<String, String> additionalProperties = new HashMap<>();

            // propagate Quarkus properties set from the build tool
            Properties existingSysProps = System.getProperties();
            for (String name : existingSysProps.stringPropertyNames()) {
                if (name.startsWith("quarkus.")) {
                    additionalProperties.put(name, existingSysProps.getProperty(name));
                }
            }

            additionalProperties.putAll(testProfileAndProperties.properties);
            //we also make the dev services config accessible from the test itself
            Map<String, String> resourceManagerProps = new HashMap<>(QuarkusIntegrationTestExtension.devServicesProps);
            // Allow override of dev services props by integration test extensions
            resourceManagerProps.putAll(testResourceManager.start());
            Map<String, String> old = new HashMap<>();
            for (Map.Entry<String, String> i : resourceManagerProps.entrySet()) {
                old.put(i.getKey(), System.getProperty(i.getKey()));
                if (i.getValue() == null) {
                    System.clearProperty(i.getKey());
                } else {
                    System.setProperty(i.getKey(), i.getValue());
                }
            }
            context.getStore(ExtensionContext.Namespace.GLOBAL).put(
                    QuarkusIntegrationTestExtension.class.getName() + ".systemProps",
                    new ExtensionContext.Store.CloseableResource() {
                        @Override
                        public void close() throws Throwable {
                            for (Map.Entry<String, String> i : old.entrySet()) {
                                old.put(i.getKey(), System.getProperty(i.getKey()));
                                if (i.getValue() == null) {
                                    System.clearProperty(i.getKey());
                                } else {
                                    System.setProperty(i.getKey(), i.getValue());
                                }
                            }
                        }
                    });
            additionalProperties.putAll(resourceManagerProps);

            ArtifactLauncher<?> launcher = null;
            String testHost = System.getProperty("quarkus.http.test-host");
            if ((testHost != null) && !testHost.isEmpty()) {
                launcher = new TestHostLauncher();
            } else {
                Config config = LauncherUtil.installAndGetSomeConfig();
                Duration waitDuration = TestConfigUtil.waitTimeValue(config);
                String target = TestConfigUtil.runTarget(config);
                // try to execute a run command published by an extension if it exists.  We do this so that extensions that have a custom run don't have to create any special artifact type
                launcher = RunCommandLauncher.tryLauncher(devServicesLaunchResult.getCuratedApplication().getQuarkusBootstrap(),
                        target, waitDuration);
                if (launcher == null) {
                    ServiceLoader<ArtifactLauncherProvider> loader = ServiceLoader.load(ArtifactLauncherProvider.class);
                    for (ArtifactLauncherProvider launcherProvider : loader) {
                        if (launcherProvider.supportsArtifactType(artifactType)) {
                            launcher = launcherProvider.create(
                                    new DefaultArtifactLauncherCreateContext(quarkusArtifactProperties, context,
                                            requiredTestClass,
                                            devServicesLaunchResult));
                            break;
                        }
                    }
                }
            }
            if (launcher == null) {
                throw new IllegalStateException(
                        "Artifact type + '" + artifactType + "' is not supported by @QuarkusIntegrationTest");
            }

            activateLogging();
            startLauncher(launcher, additionalProperties, () -> ssl = true);

            Closeable resource = new IntegrationTestExtensionStateResource(launcher,
                    devServicesLaunchResult.getCuratedApplication());
            IntegrationTestExtensionState state = new IntegrationTestExtensionState(testResourceManager, resource,
                    sysPropRestore);
            testHttpEndpointProviders = TestHttpEndpointProvider.load();

            return state;
        } catch (Throwable e) {
            if (!InitialConfigurator.DELAYED_HANDLER.isActivated()) {
                activateLogging();
            }

            try {
                if (testResourceManager != null) {
                    testResourceManager.close();
                }
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        ensureStarted(context);
        if (!failedBoot) {
            doProcessTestInstance(testInstance, context);
            injectTestContext(testInstance);
        }
    }

    private void injectTestContext(Object testInstance) {
        Class<?> c = testInstance.getClass();
        while (c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType().equals(DevServicesContext.class)) {
                    try {
                        f.setAccessible(true);
                        f.set(testInstance, createTestContext());
                        return;
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to set field '" + f.getName()
                                + "' with the proper test context", e);
                    }
                } else if (DevServicesContext.ContextAware.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    try {
                        DevServicesContext.ContextAware val = (DevServicesContext.ContextAware) f.get(testInstance);
                        val.setIntegrationTestContext(createTestContext());
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to inject context into field " + f.getName(), e);
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private DevServicesContext createTestContext() {
        Map<String, String> devServicesPropsCopy = devServicesProps.isEmpty() ? Collections.emptyMap()
                : Collections.unmodifiableMap(devServicesProps);
        return new DefaultQuarkusIntegrationTestContext(devServicesPropsCopy,
                containerNetworkId == null ? Optional.empty() : Optional.of(containerNetworkId));
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

    private boolean isCallbacksEnabledForIntegrationTests() {
        return Optional.ofNullable(System.getProperty(ENABLED_CALLBACKS_PROPERTY)).map(Boolean::parseBoolean)
                .or(() -> LauncherUtil.installAndGetSomeConfig().getOptionalValue(ENABLED_CALLBACKS_PROPERTY, Boolean.class))
                .orElse(false);
    }

    private QuarkusTestMethodContext createQuarkusTestMethodContext(ExtensionContext context) {
        Object testInstance = context.getTestInstance().orElse(null);
        List<Object> outerInstances = context.getTestInstances()
                .map(testInstances -> testInstances.getAllInstances().stream()
                        .filter(instance -> instance != testInstance)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        return new QuarkusTestMethodContext(
                testInstance,
                outerInstances,
                context.getTestMethod().orElse(null),
                getState(context).getTestErrorCause());
    }

    private static class DefaultArtifactLauncherCreateContext implements ArtifactLauncherProvider.CreateContext {
        private final Properties quarkusArtifactProperties;
        private final ExtensionContext context;
        private final Class<?> requiredTestClass;
        private final ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;

        DefaultArtifactLauncherCreateContext(Properties quarkusArtifactProperties, ExtensionContext context,
                Class<?> requiredTestClass, ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult) {
            this.quarkusArtifactProperties = quarkusArtifactProperties;
            this.context = context;
            this.requiredTestClass = requiredTestClass;
            this.devServicesLaunchResult = devServicesLaunchResult;
        }

        @Override
        public Properties quarkusArtifactProperties() {
            return quarkusArtifactProperties;
        }

        @Override
        public Path buildOutputDirectory() {
            return determineBuildOutputDirectory(context);
        }

        @Override
        public Class<?> testClass() {
            return requiredTestClass;
        }

        @Override
        public ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult() {
            return devServicesLaunchResult;
        }
    }

    private static class DefaultQuarkusIntegrationTestContext implements DevServicesContext {

        private final Map<String, String> devServicesProperties;
        private final Optional<String> containerNetworkId;

        private DefaultQuarkusIntegrationTestContext(Map<String, String> devServicesProperties,
                Optional<String> containerNetworkId) {
            this.devServicesProperties = devServicesProperties;
            this.containerNetworkId = containerNetworkId;
        }

        @Override
        public Map<String, String> devServicesProperties() {
            return devServicesProperties;
        }

        @Override
        public Optional<String> containerNetworkId() {
            return containerNetworkId;
        }
    }

    private static final class IntegrationTestExtensionStateResource implements Closeable {

        private final ArtifactLauncher<?> launcher;
        private final CuratedApplication curatedApplication;

        public IntegrationTestExtensionStateResource(ArtifactLauncher<?> launcher,
                CuratedApplication curatedApplication) {
            this.launcher = launcher;
            this.curatedApplication = curatedApplication;
        }

        @Override
        public void close() throws IOException {
            if (launcher != null) {
                try {
                    launcher.close();
                } catch (Exception e) {
                    System.err.println("Unable to close ArtifactLauncher: " + e.getMessage());
                }
            }
            try {
                curatedApplication.close();
            } catch (Exception e) {
                System.err.println("Unable to close CuratedApplication: " + e.getMessage());
            }
        }
    }
}
