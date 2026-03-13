package io.quarkus.test.junit;

import static io.quarkus.runtime.LaunchMode.NORMAL;
import static io.quarkus.runtime.configuration.ConfigSourceOrdinal.INTEGRATION_TEST;
import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.determineBuildOutputDirectory;
import static io.quarkus.test.junit.IntegrationTestUtil.doProcessTestInstance;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.findProfile;
import static io.quarkus.test.junit.IntegrationTestUtil.getEffectiveArtifactType;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevServices;
import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.startLauncher;
import static io.quarkus.test.junit.TestResourceUtil.TestResourceManagerReflections.copyEntriesFromProfile;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConfigCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ValueRegistryConfigSource;
import io.quarkus.runtime.ValueRegistryImpl;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.logging.LogRuntimeConfig;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.ListeningAddress;
import io.quarkus.test.common.RestAssuredStateManager;
import io.quarkus.test.common.RunCommandLauncher;
import io.quarkus.test.common.TestConfigUtil;
import io.quarkus.test.common.TestHostLauncher;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.config.ConfigInjector;
import io.quarkus.test.config.ThreadLocalConfigSourceProvider;
import io.quarkus.test.config.ValueRegistryInjector;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import io.quarkus.test.junit.launcher.ArtifactLauncherProvider;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class QuarkusIntegrationTestExtension extends AbstractQuarkusTestWithContextExtension
        implements BeforeTestExecutionCallback, AfterTestExecutionCallback, BeforeEachCallback, AfterEachCallback,
        BeforeAllCallback, AfterAllCallback, TestInstancePostProcessor, ParameterResolver {

    private static boolean failedBoot;

    private static List<Function<Class<?>, String>> testHttpEndpointProviders;

    private static Class<? extends QuarkusTestProfile> quarkusTestProfile;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static Class<?> currentJUnitTestClass;

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
            ThreadLocalConfigSourceProvider.reset();
            RestAssuredStateManager.clearState();
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

            // Inject of ValueRegistry and Config done IntegrationTestUtil.doProcessTestInstance

            ValueRegistry valueRegistry = ValueRegistryInjector.get(context);
            Optional<ListeningAddress> listeningAddress = valueRegistry.get(ListeningAddress.LISTENING_ADDRESS);
            listeningAddress.ifPresent(new Consumer<ListeningAddress>() {
                @Override
                public void accept(ListeningAddress listeningAddress) {
                    RestAssuredStateManager.setURL(listeningAddress.isSsl(), listeningAddress.port(),
                            QuarkusTestExtension.getEndpointPath(context, testHttpEndpointProviders));
                }
            });
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
        ensureNoInjectAnnotationIsUsed(testClass, "@QuarkusIntegrationTest");

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
        boolean reloadTestResources = false;
        if ((state == null && !failedBoot) || wrongProfile || (reloadTestResources = isNewTestClass
                && TestResourceUtil.testResourcesRequireReload(state, extensionContext.getRequiredTestClass(),
                        Optional.ofNullable(selectedProfile)))) {
            if (wrongProfile || reloadTestResources) {
                if (state != null) {
                    try {
                        ConfigInjector.clear(extensionContext);
                        ValueRegistryInjector.clear(extensionContext);
                        state.close();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
            try {
                state = doProcessStart(selectedProfile, extensionContext);
                setState(extensionContext, state);
            } catch (Throwable e) {
                try {
                    LogRuntimeConfig logRuntimeConfig = Config.get().getConfigMapping(LogRuntimeConfig.class);
                    File appLogFile = logRuntimeConfig.file().path();
                    if (appLogFile.exists() && (appLogFile.length() > 0)) {
                        System.err.println("Failed to launch the application. The application logs can be found at: "
                                + appLogFile.getAbsolutePath());
                    }
                } catch (IllegalStateException ignored) {

                }

                failedBoot = true;
                firstException = e;
            }
        }
        return state;
    }

    private QuarkusTestExtensionState doProcessStart(Class<? extends QuarkusTestProfile> profile, ExtensionContext context)
            throws Throwable {

        Properties quarkusArtifactProperties = readQuarkusArtifactProperties(context);
        Config config = Config.get();
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);
        String artifactType = getEffectiveArtifactType(testConfig, quarkusArtifactProperties);
        boolean isDockerLaunch = isContainer(artifactType)
                || (isJar(artifactType) && "test-with-native-agent".equals(testConfig.integrationTestProfile()));

        quarkusTestProfile = profile;
        currentJUnitTestClass = context.getRequiredTestClass();
        TestResourceManager testResourceManager = null;
        try {
            Class<?> requiredTestClass = context.getRequiredTestClass();

            TestProfileAndProperties testProfileAndProperties = TestProfileAndProperties.ofNullable(profile, NORMAL);
            // prepare dev services after profile and properties have been determined
            ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult = handleDevServices(context,
                    isDockerLaunch, testProfileAndProperties);

            testResourceManager = new TestResourceManager(
                    requiredTestClass,
                    quarkusTestProfile,
                    copyEntriesFromProfile(testProfileAndProperties.testProfile(),
                            context.getRequiredTestClass().getClassLoader()),
                    testProfileAndProperties.isDisabledGlobalTestResources(),
                    devServicesLaunchResult.properties(),
                    Optional.ofNullable(devServicesLaunchResult.networkId()));
            testResourceManager.init(testProfileAndProperties.testProfileClassName().orElse(null));

            if (testConfig.enableCallbacksForIntegrationTests()) {
                populateCallbacks(requiredTestClass.getClassLoader());
            }

            Map<String, String> additionalProperties = new HashMap<>();

            // propagate Quarkus properties set from the build tool
            Properties existingSysProps = System.getProperties();
            for (String name : existingSysProps.stringPropertyNames()) {
                if (name.startsWith("quarkus.")
                        // don't include 'quarkus.profile' as that has already been taken into account when determining the launch profile
                        // so we don't want this to end up in multiple launch arguments
                        && !name.equals("quarkus.profile")
                        // 'quarkus.test.arg-line' is provided directly to DefaultInitContextBase as a list of properties to add,
                        // if it's not excluded here, it will be added to the command line (unquoted) resulting in
                        // many duplicate system property arguments
                        && !name.equals("quarkus.test.arg-line")) {
                    additionalProperties.put(name, existingSysProps.getProperty(name));
                }
            }

            // Properties set by @TestProfile
            additionalProperties.putAll(testProfileAndProperties.properties());
            // Make the dev services config accessible from the test itself
            additionalProperties.putAll(devServicesLaunchResult.properties());
            // Allow override of dev services props by integration test extensions
            additionalProperties.putAll(testResourceManager.start());

            // Create the ValueRegistry with the current Config and test config
            ConfigSource integrationTestSource = new PropertiesConfigSource(
                    additionalProperties, INTEGRATION_TEST.getName(), INTEGRATION_TEST.getOrdinal());
            ValueRegistry valueRegistry = ValueRegistryImpl.builder().addDiscoveredInfos()
                    .withRuntimeSource(new SmallRyeConfigBuilder().withSources(integrationTestSource).build())
                    .withRuntimeSource(config)
                    .build();
            ValueRegistryInjector.set(context, valueRegistry);

            // Create a new Config to add the configuration coming from test profiles, devservices and test resources
            SmallRyeConfig newConfig = ConfigUtils.configBuilder(LaunchMode.TEST)
                    .forClassLoader(Config.get().getClass().getClassLoader())
                    .withCustomizers(new TestConfigCustomizer(LaunchMode.TEST))
                    .withCustomizers(ValueRegistryConfigSource.customizer(valueRegistry))
                    .withSources(integrationTestSource)
                    .build();
            ConfigInjector.set(context, newConfig);

            ArtifactLauncher<?> launcher;
            Optional<String> testHost = config.getOptionalValue("quarkus.http.test-host", String.class);
            if (testHost.isPresent()) {
                launcher = new TestHostLauncher();
            } else {
                String target = TestConfigUtil.runTarget(config);
                // try to execute a run command published by an extension if it exists.  We do this so that extensions that have a custom run don't have to create any special artifact type
                launcher = RunCommandLauncher.tryLauncher(devServicesLaunchResult.getCuratedApplication().getQuarkusBootstrap(),
                        target, testConfig.waitTime());
                if (launcher == null) {
                    ServiceLoader<ArtifactLauncherProvider> loader = ServiceLoader.load(ArtifactLauncherProvider.class);
                    for (ArtifactLauncherProvider launcherProvider : loader) {
                        if (launcherProvider.supportsArtifactType(artifactType, testConfig.integrationTestProfile())) {
                            launcher = launcherProvider.create(
                                    new DefaultArtifactLauncherCreateContext(quarkusArtifactProperties, context,
                                            requiredTestClass,
                                            profile,
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

            // Start Quarkus, capture the listening port if available and register it in ValueRegistry
            Optional<ListeningAddress> listeningAddress = startLauncher(launcher, additionalProperties);
            listeningAddress.ifPresent(address -> {
                address.register(valueRegistry, newConfig);
                valueRegistry.register(ListeningAddress.LISTENING_ADDRESS, listeningAddress);
            });

            testHttpEndpointProviders = TestHttpEndpointProvider.load();

            return new IntegrationTestExtensionState(
                    testResourceManager,
                    new IntegrationTestExtensionStateResource(launcher, devServicesLaunchResult.getCuratedApplication()),
                    AbstractTestWithCallbacksExtension::clearCallbacks);
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
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (ValueRegistryInjector.PARAMETER_RESOLVER.supportsParameter(parameterContext, extensionContext)) {
            return true;
        }
        if (ConfigInjector.PARAMETER_RESOLVER.supportsParameter(parameterContext, extensionContext)) {
            return true;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (ValueRegistryInjector.PARAMETER_RESOLVER.supportsParameter(parameterContext, extensionContext)) {
            return ValueRegistryInjector.PARAMETER_RESOLVER.resolveParameter(parameterContext, extensionContext);
        }
        if (ConfigInjector.PARAMETER_RESOLVER.supportsParameter(parameterContext, extensionContext)) {
            return ConfigInjector.PARAMETER_RESOLVER.resolveParameter(parameterContext, extensionContext);
        }
        return null;
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
        private final Class<? extends QuarkusTestProfile> profile;
        private final ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;

        DefaultArtifactLauncherCreateContext(Properties quarkusArtifactProperties, ExtensionContext context,
                Class<?> requiredTestClass,
                Class<? extends QuarkusTestProfile> profile,
                ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult) {
            this.quarkusArtifactProperties = quarkusArtifactProperties;
            this.context = context;
            this.requiredTestClass = requiredTestClass;
            this.profile = profile;
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
        public Class<? extends QuarkusTestProfile> profile() {
            return profile;
        }

        @Override
        public ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult() {
            return devServicesLaunchResult;
        }

    }

    private static final class IntegrationTestExtensionStateResource implements Closeable {

        private final ArtifactLauncher<?> launcher;
        private final CuratedApplication curatedApplication;

        IntegrationTestExtensionStateResource(ArtifactLauncher<?> launcher, CuratedApplication curatedApplication) {
            this.launcher = launcher;
            this.curatedApplication = curatedApplication;
        }

        @Override
        public void close() {
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
