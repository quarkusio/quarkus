package io.quarkus.test.junit;

import static io.quarkus.runtime.LaunchMode.NORMAL;
import static io.quarkus.runtime.configuration.ConfigSourceOrdinal.INTEGRATION_TEST;
import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.determineBuildOutputDirectory;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.getEffectiveArtifactType;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevServices;
import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;
import static io.quarkus.test.junit.TestResourceUtil.TestResourceManagerReflections.copyEntriesFromProfile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.deployment.dev.testing.TestConfigCustomizer;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ValueRegistryConfigSource;
import io.quarkus.runtime.ValueRegistryImpl;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.config.ConfigInjector;
import io.quarkus.test.config.ThreadLocalConfigSourceProvider;
import io.quarkus.test.config.ValueRegistryInjector;
import io.quarkus.test.junit.launcher.ArtifactLauncherProvider;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.value.registry.ValueRegistry;
import io.smallrye.config.Config;
import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;

public class QuarkusMainIntegrationTestExtension extends AbstractQuarkusTestWithContextExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create("io.quarkus.test.main.integration");

    ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult;
    Properties quarkusArtifactProperties;

    LaunchResult result;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        var launch = context.getRequiredTestMethod().getAnnotation(Launch.class);
        if (launch != null) {
            String[] arguments = launch.value();
            LaunchResult r = doLaunch(context, arguments);
            Assertions.assertEquals(launch.exitCode(), r.exitCode(), "Exit code did not match");
            this.result = r;

            ValueRegistry valueRegistry = ValueRegistryInjector.get(context);
            Config config = ConfigInjector.get(context);

            Object testInstance = context.getRequiredTestInstance();
            ValueRegistryInjector.inject(testInstance, valueRegistry);
            ConfigInjector.inject(testInstance, config);
            ThreadLocalConfigSourceProvider.set(ConfigInjector.get(context));
        }
    }

    private LaunchResult doLaunch(ExtensionContext context, String[] arguments) throws Exception {
        var result = doProcessStart(context, arguments);
        List<String> out = Arrays.asList(new String(result.getOutput(), StandardCharsets.UTF_8).split("\n"));
        List<String> err = Arrays.asList(new String(result.getStderror(), StandardCharsets.UTF_8).split("\n"));
        return new LaunchResult() {
            @Override
            public List<String> getOutputStream() {
                return out;
            }

            @Override
            public List<String> getErrorStream() {
                return err;
            }

            @Override
            public int exitCode() {
                return result.getStatusCode();
            }
        };
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        result = null;
        ThreadLocalConfigSourceProvider.reset();
        ConfigInjector.clear(context);
        ValueRegistryInjector.clear(context);
    }

    // TODO - Make this consistent with QuarkusIntegrationTestExtension
    private void prepare(ExtensionContext extensionContext, TestProfileAndProperties testProfileAndProperties)
            throws Exception {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass, "@QuarkusMainIntegrationTest");

        quarkusArtifactProperties = readQuarkusArtifactProperties(extensionContext);
        Config config = Config.get();
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);
        String artifactType = getEffectiveArtifactType(testConfig, quarkusArtifactProperties);
        boolean isDockerLaunch = isContainer(artifactType)
                || (isJar(artifactType) && "test-with-native-agent".equals(testConfig.integrationTestProfile()));

        devServicesLaunchResult = handleDevServices(extensionContext, isDockerLaunch, testProfileAndProperties);

        ExtensionContext root = extensionContext.getRoot();
        root.getStore(NAMESPACE).put("devServicesLaunchResult", devServicesLaunchResult);
    }

    private ArtifactLauncher.LaunchResult doProcessStart(ExtensionContext context, String[] args) {
        Config config = Config.get();
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);

        try {
            Class<? extends QuarkusTestProfile> profile = IntegrationTestUtil.findProfile(context.getRequiredTestClass());
            TestResourceManager testResourceManager = null;
            try {
                Class<?> requiredTestClass = context.getRequiredTestClass();
                TestProfileAndProperties testProfileAndProperties = TestProfileAndProperties.ofNullable(profile, NORMAL);
                // prepare dev services after profile and properties have been determined
                if (quarkusArtifactProperties == null) {
                    prepare(context, testProfileAndProperties);
                }
                String artifactType = quarkusArtifactProperties.getProperty("type");

                testResourceManager = new TestResourceManager(
                        requiredTestClass,
                        profile,
                        copyEntriesFromProfile(testProfileAndProperties.testProfile(),
                                context.getRequiredTestClass().getClassLoader()),
                        testProfileAndProperties.isDisabledGlobalTestResources());
                testResourceManager.init(testProfileAndProperties.testProfileClassName().orElse(null));

                Map<String, String> additionalProperties = new HashMap<>();

                // propagate Quarkus properties set from the build tool
                Properties existingSysProps = System.getProperties();
                for (String name : existingSysProps.stringPropertyNames()) {
                    if (name.startsWith("quarkus.")
                            // don't include 'quarkus.profile' as that has already been taken into account when determining the launch profile
                            // so we don't want this to end up in multiple launch arguments
                            && !name.equals("quarkus.profile")) {
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

                ArtifactLauncher<?> launcher = null;
                ServiceLoader<ArtifactLauncherProvider> loader = ServiceLoader.load(ArtifactLauncherProvider.class);
                for (ArtifactLauncherProvider launcherProvider : loader) {
                    if (launcherProvider.supportsArtifactType(artifactType, testConfig.integrationTestProfile())) {
                        launcher = launcherProvider.create(
                                new DefaultArtifactLauncherCreateContext(quarkusArtifactProperties, context, requiredTestClass,
                                        profile,
                                        devServicesLaunchResult));
                        break;
                    }
                }
                if (launcher == null) {
                    throw new IllegalStateException(
                            "Artifact type + '" + artifactType + "' is not supported by @QuarkusMainIntegrationTest");
                }

                testResourceManager.inject(context.getRequiredTestInstance());

                activateLogging();

                launcher.includeAsSysProps(additionalProperties);
                return launcher.runToCompletion(args);

            } finally {
                try {
                    if (testResourceManager != null) {
                        testResourceManager.close();
                    }
                } catch (Exception e) {
                    System.err.println("Unable to shutdown resource: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class || type == QuarkusMainLauncher.class;
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

        Class<?> type = parameterContext.getParameter().getType();
        if (type == LaunchResult.class) {
            return result;
        } else if (type == QuarkusMainLauncher.class) {
            return new QuarkusMainLauncher() {
                @Override
                public LaunchResult launch(String... args) {
                    try {
                        return doLaunch(extensionContext, args);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        } else {
            throw new RuntimeException("Parameter type not supported");
        }
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

}
