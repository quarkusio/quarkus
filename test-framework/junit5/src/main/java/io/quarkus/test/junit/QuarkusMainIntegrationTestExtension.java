package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.determineBuildOutputDirectory;
import static io.quarkus.test.junit.IntegrationTestUtil.determineTestProfileAndProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.getSysPropsToRestore;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevServices;
import static io.quarkus.test.junit.IntegrationTestUtil.readQuarkusArtifactProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.launcher.ArtifactLauncherProvider;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.util.CloseAdaptor;

public class QuarkusMainIntegrationTestExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

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
        if (quarkusArtifactProperties == null) {
            prepare(context);
        }
        var result = doProcessStart(context, arguments);
        List<String> out = Arrays.asList(new String(result.getOutput(), StandardCharsets.UTF_8).split("\n"));
        List<String> err = Arrays.asList(new String(result.getOutput(), StandardCharsets.UTF_8).split("\n"));
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
        quarkusArtifactProperties = readQuarkusArtifactProperties(extensionContext);
        String artifactType = quarkusArtifactProperties.getProperty("type");
        if (artifactType == null) {
            throw new IllegalStateException("Unable to determine the type of artifact created by the Quarkus build");
        }
        boolean isDockerLaunch = "jar-container".equals(artifactType) || "native-container".equals(artifactType);

        ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult = handleDevServices(extensionContext,
                isDockerLaunch);
        devServicesProps = devServicesLaunchResult.properties();

        ExtensionContext root = extensionContext.getRoot();
        root.getStore(NAMESPACE).put("devServicesLaunchResult", new CloseAdaptor(devServicesLaunchResult));
    }

    private ArtifactLauncher.LaunchResult doProcessStart(ExtensionContext context, String[] args) {
        try {
            Class<? extends QuarkusTestProfile> profile = IntegrationTestUtil.findProfile(context.getRequiredTestClass());
            TestResourceManager testResourceManager = null;
            Map<String, String> old = new HashMap<>();
            String artifactType = quarkusArtifactProperties.getProperty("type");
            try {
                Class<?> requiredTestClass = context.getRequiredTestClass();

                Map<String, String> sysPropRestore = getSysPropsToRestore();
                TestProfileAndProperties testProfileAndProperties = determineTestProfileAndProperties(profile, sysPropRestore);

                testResourceManager = new TestResourceManager(requiredTestClass, profile,
                        Collections.emptyList(), testProfileAndProperties.testProfile != null
                                && testProfileAndProperties.testProfile.disableGlobalTestResources());
                testResourceManager.init();
                Map<String, String> additionalProperties = new HashMap<>(testProfileAndProperties.properties);
                additionalProperties.putAll(QuarkusMainIntegrationTestExtension.devServicesProps);
                Map<String, String> resourceManagerProps = testResourceManager.start();
                for (Map.Entry<String, String> i : resourceManagerProps.entrySet()) {
                    old.put(i.getKey(), System.getProperty(i.getKey()));
                    if (i.getValue() == null) {
                        System.clearProperty(i.getKey());
                    } else {
                        System.setProperty(i.getKey(), i.getValue());
                    }
                }
                additionalProperties.putAll(resourceManagerProps);

                ArtifactLauncher<?> launcher = null;
                ServiceLoader<ArtifactLauncherProvider> loader = ServiceLoader.load(ArtifactLauncherProvider.class);
                for (ArtifactLauncherProvider launcherProvider : loader) {
                    if (launcherProvider.supportsArtifactType(artifactType)) {
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
                return launcher.runToCompletion(args);

            } finally {

                for (Map.Entry<String, String> i : old.entrySet()) {
                    old.put(i.getKey(), System.getProperty(i.getKey()));
                    if (i.getValue() == null) {
                        System.clearProperty(i.getKey());
                    } else {
                        System.setProperty(i.getKey(), i.getValue());
                    }
                }
                if (testResourceManager != null) {
                    testResourceManager.close();
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
