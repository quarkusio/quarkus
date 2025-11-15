package io.quarkus.test.junit;

import static io.quarkus.test.junit.ArtifactTypeUtil.isContainer;
import static io.quarkus.test.junit.ArtifactTypeUtil.isJar;
import static io.quarkus.test.junit.IntegrationTestUtil.activateLogging;
import static io.quarkus.test.junit.IntegrationTestUtil.determineBuildOutputDirectory;
import static io.quarkus.test.junit.IntegrationTestUtil.determineTestProfileAndProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.getEffectiveArtifactType;
import static io.quarkus.test.junit.IntegrationTestUtil.getSysPropsToRestore;
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

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.runtime.logging.JBossVersion;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.launcher.ArtifactLauncherProvider;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.smallrye.config.SmallRyeConfig;

public class QuarkusMainIntegrationTestExtension extends AbstractQuarkusTestWithContextExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
            .create("io.quarkus.test.main.integration");

    private static Map<String, String> devServicesProps;

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
        }
    }

    private LaunchResult doLaunch(ExtensionContext context, String[] arguments) throws Exception {
        JBossVersion.disableVersionLogging();

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
    }

    private void prepare(ExtensionContext extensionContext) throws Exception {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass, "@QuarkusMainIntegrationTest");

        quarkusArtifactProperties = readQuarkusArtifactProperties(extensionContext);
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        String artifactType = getEffectiveArtifactType(quarkusArtifactProperties, config);

        TestConfig testConfig = config.getConfigMapping(TestConfig.class);

        boolean isDockerLaunch = isContainer(artifactType)
                || (isJar(artifactType) && "test-with-native-agent".equals(testConfig.integrationTestProfile()));

        devServicesLaunchResult = handleDevServices(extensionContext, isDockerLaunch);
        devServicesProps = devServicesLaunchResult.properties();

        ExtensionContext root = extensionContext.getRoot();
        root.getStore(NAMESPACE).put("devServicesLaunchResult", devServicesLaunchResult);
    }

    private ArtifactLauncher.LaunchResult doProcessStart(ExtensionContext context, String[] args) {
        try {
            Class<? extends QuarkusTestProfile> profile = IntegrationTestUtil.findProfile(context.getRequiredTestClass());
            TestResourceManager testResourceManager = null;
            Map<String, String> old = new HashMap<>();
            try {
                Class<?> requiredTestClass = context.getRequiredTestClass();

                Map<String, String> sysPropRestore = getSysPropsToRestore();
                TestProfileAndProperties testProfileAndProperties = determineTestProfileAndProperties(profile, sysPropRestore);
                // prepare dev services after profile and properties have been determined
                if (quarkusArtifactProperties == null) {
                    prepare(context);
                }
                String artifactType = quarkusArtifactProperties.getProperty("type");

                testResourceManager = new TestResourceManager(requiredTestClass, profile,
                        copyEntriesFromProfile(testProfileAndProperties.testProfile,
                                context.getRequiredTestClass().getClassLoader()),
                        testProfileAndProperties.testProfile != null
                                && testProfileAndProperties.testProfile.disableGlobalTestResources());
                testResourceManager.init(
                        testProfileAndProperties.testProfile != null ? testProfileAndProperties.testProfile.getClass().getName()
                                : null);

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

                additionalProperties.putAll(testProfileAndProperties.properties);
                //also make the dev services props accessible from the test
                Map<String, String> resourceManagerProps = new HashMap<>(QuarkusMainIntegrationTestExtension.devServicesProps);
                // Allow override of dev services props by integration test extensions
                resourceManagerProps.putAll(testResourceManager.start());
                for (Map.Entry<String, String> i : resourceManagerProps.entrySet()) {
                    old.put(i.getKey(), System.getProperty(i.getKey()));
                    if (i.getValue() == null) {
                        System.clearProperty(i.getKey());
                    } else {
                        System.setProperty(i.getKey(), i.getValue());
                    }
                }
                additionalProperties.putAll(resourceManagerProps);
                // recalculate the property names that may have changed with testProfileAndProperties.properties
                ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getLatestPropertyNames();

                testResourceManager.inject(context.getRequiredTestInstance());

                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
                TestConfig testConfig = config.getConfigMapping(TestConfig.class);

                ArtifactLauncher<?> launcher = null;
                ServiceLoader<ArtifactLauncherProvider> loader = ServiceLoader.load(ArtifactLauncherProvider.class);
                for (ArtifactLauncherProvider launcherProvider : loader) {
                    if (launcherProvider.supportsArtifactType(artifactType, testConfig.integrationTestProfile())) {
                        launcher = launcherProvider.create(
                                new DefaultArtifactLauncherCreateContext(quarkusArtifactProperties, context, requiredTestClass,
                                        devServicesLaunchResult));
                        break;
                    }
                }
                if (launcher == null) {
                    throw new IllegalStateException(
                            "Artifact type + '" + artifactType + "' is not supported by @QuarkusMainIntegrationTest");
                }

                launcher.includeAsSysProps(additionalProperties);
                activateLogging();
                return launcher.runToCompletion(args);

            } finally {
                for (Map.Entry<String, String> i : old.entrySet()) {
                    if (i.getValue() == null) {
                        System.clearProperty(i.getKey());
                    } else {
                        System.setProperty(i.getKey(), i.getValue());
                    }
                }
                // recalculate the property names that may have changed with the restore
                ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getLatestPropertyNames();
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
        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class || type == QuarkusMainLauncher.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
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

}
