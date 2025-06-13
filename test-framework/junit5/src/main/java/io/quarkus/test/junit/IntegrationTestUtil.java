package io.quarkus.test.junit;

import static io.quarkus.deployment.util.ContainerRuntimeUtil.detectContainerRuntime;
import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;
import static java.lang.ProcessBuilder.Redirect.DISCARD;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.builditem.DevServicesCustomizerBuildItem;
import io.quarkus.deployment.builditem.DevServicesLauncherConfigResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesNetworkIdBuildItem;
import io.quarkus.deployment.builditem.DevServicesRegistryBuildItem;
import io.quarkus.deployment.util.ContainerRuntimeUtil;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.logging.LoggingSetupRecorder;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;
import io.smallrye.config.SmallRyeConfig;

public final class IntegrationTestUtil {

    public static final int DEFAULT_PORT = 8081;
    public static final int DEFAULT_HTTPS_PORT = 8444;

    private IntegrationTestUtil() {
    }

    static void ensureNoInjectAnnotationIsUsed(Class<?> testClass, String quarkusTestAnnotation) {
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getAnnotation(Inject.class) != null) {
                    throw new JUnitException(
                            "@Inject is not supported in " + quarkusTestAnnotation + " tests. Offending field is "
                                    + field.getDeclaringClass().getTypeName() + "."
                                    + field.getName());
                }
                if (field.getAnnotation(ConfigProperty.class) != null) {
                    throw new JUnitException(
                            "@ConfigProperty is not supported in " + quarkusTestAnnotation
                                    + " tests. Offending field is "
                                    + field.getDeclaringClass().getTypeName() + "."
                                    + field.getName());
                }
            }
            current = current.getSuperclass();
        }

    }

    static Class<? extends QuarkusTestProfile> findProfile(Class<?> testClass) {
        while (testClass != Object.class) {
            TestProfile annotation = testClass.getAnnotation(TestProfile.class);
            if (annotation != null) {
                return annotation.value();
            }
            testClass = testClass.getSuperclass();
        }
        return null;
    }

    static void doProcessTestInstance(Object testInstance, ExtensionContext context) {
        TestHTTPResourceManager.inject(testInstance);
        ExtensionContext root = context.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        QuarkusTestExtensionState state = store.get(QuarkusTestExtensionState.class.getName(),
                QuarkusTestExtensionState.class);
        Object testResourceManager = state.testResourceManager;
        if (!(testResourceManager instanceof TestResourceManager)) {
            throw new RuntimeException(
                    "An unexpected situation occurred while trying to instantiate the testing infrastructure. Have you perhaps mixed @QuarkusTest and @QuarkusIntegrationTest in the same test run?");
        }
        ((TestResourceManager) state.testResourceManager).inject(testInstance);
    }

    static Map<String, String> getSysPropsToRestore() {
        Map<String, String> sysPropRestore = new HashMap<>();
        sysPropRestore.put(LaunchMode.DEVELOPMENT.getProfileKey(), System.getProperty(LaunchMode.TEST.getProfileKey()));
        return sysPropRestore;
    }

    static TestProfileAndProperties determineTestProfileAndProperties(Class<? extends QuarkusTestProfile> profile,
            Map<String, String> sysPropRestore)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final Map<String, String> properties = new HashMap<>();
        QuarkusTestProfile testProfile = null;
        if (profile != null) {
            testProfile = profile.getDeclaredConstructor().newInstance();
            properties.putAll(testProfile.getConfigOverrides());
            final Set<Class<?>> enabledAlternatives = testProfile.getEnabledAlternatives();
            if (!enabledAlternatives.isEmpty()) {
                properties.put("quarkus.arc.selected-alternatives", enabledAlternatives.stream()
                        .peek((c) -> {
                            if (!c.isAnnotationPresent(Alternative.class)) {
                                throw new RuntimeException(
                                        "Enabled alternative " + c + " is not annotated with @Alternative");
                            }
                        })
                        .map(Class::getName).collect(Collectors.joining(",")));
            }
            final String configProfile = testProfile.getConfigProfile();
            if (configProfile != null) {
                properties.put(LaunchMode.NORMAL.getProfileKey(), configProfile);
            }
            properties.put("quarkus.config.build-time-mismatch-at-runtime", "fail");
            for (Map.Entry<String, String> i : properties.entrySet()) {
                sysPropRestore.put(i.getKey(), System.getProperty(i.getKey()));
            }
            for (Map.Entry<String, String> i : properties.entrySet()) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }
        // recalculate the property names that may have changed
        ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getLatestPropertyNames();
        return new TestProfileAndProperties(testProfile, properties);
    }

    static void startLauncher(ArtifactLauncher launcher, Map<String, String> additionalProperties, Runnable sslSetter)
            throws IOException {
        launcher.includeAsSysProps(additionalProperties);
        try {
            launcher.start();
        } catch (IOException e) {
            try {
                launcher.close();
            } catch (Throwable ignored) {
            }
            throw e;
        }
        if (launcher.listensOnSsl()) {
            if (sslSetter != null) {
                sslSetter.run();
            }
        }
    }

    static ArtifactLauncher.InitContext.DevServicesLaunchResult handleDevServices(ExtensionContext context,
            boolean isDockerAppLaunch) throws Exception {
        Class<?> requiredTestClass = context.getRequiredTestClass();
        Path testClassLocation = getTestClassesLocation(requiredTestClass);
        final Path appClassLocation = getAppClassLocationForTestLocation(testClassLocation);

        final PathList.Builder rootBuilder = PathList.builder();

        if (!appClassLocation.equals(testClassLocation)) {
            rootBuilder.add(testClassLocation);
            // if test classes is a dir, we should also check whether test resources dir exists as a separate dir (gradle)
            // TODO: this whole app/test path resolution logic is pretty dumb, it needs be re-worked using proper workspace discovery
            final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(testClassLocation, "test");
            if (testResourcesLocation != null) {
                rootBuilder.add(testResourcesLocation);
            }
        }
        final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder()
                .setIsolateDeployment(true)
                .setMode(QuarkusBootstrap.Mode.TEST);

        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();
        runnerBuilder.setProjectRoot(projectRoot);
        runnerBuilder.setTargetDirectory(PathTestHelper.getProjectBuildDir(projectRoot, testClassLocation));

        if (Files.exists(appClassLocation)) {
            rootBuilder.add(appClassLocation);
        }
        final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
        if (appResourcesLocation != null) {
            if (Files.exists(appResourcesLocation)) {
                rootBuilder.add(appResourcesLocation);
            }
        }

        // If gradle project running directly with IDE
        if (System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null) {
            ApplicationModel model = BuildToolHelper.enableGradleAppModelForTest(projectRoot);
            if (model != null && model.getApplicationModule() != null) {
                final ArtifactSources testSources = model.getApplicationModule().getTestSources();
                if (testSources != null) {
                    for (SourceDir src : testSources.getSourceDirs()) {
                        if (!Files.exists(src.getOutputDir())) {
                            final Path classes = src.getOutputDir();
                            if (!rootBuilder.contains(classes)) {
                                rootBuilder.add(classes);
                            }
                        }
                    }
                }
                for (SourceDir src : model.getApplicationModule().getMainSources().getSourceDirs()) {
                    if (!Files.exists(src.getOutputDir())) {
                        final Path classes = src.getOutputDir();
                        if (!rootBuilder.contains(classes)) {
                            rootBuilder.add(classes);
                        }
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
        runnerBuilder.setApplicationRoot(rootBuilder.build());

        CuratedApplication curatedApplication = runnerBuilder
                .setTest(true)
                .build()
                .bootstrap();

        Index testClassesIndex = TestClassIndexer.indexTestClasses(requiredTestClass);
        // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
        TestClassIndexer.writeIndex(testClassesIndex, requiredTestClass);

        Map<String, String> propertyMap = new HashMap<>();
        AugmentAction augmentAction;
        String networkId = null;
        if (isDockerAppLaunch) {
            // when the application is going to be launched as a docker container, we need to make containers started by DevServices
            // use a shared network that the application container can then use as well
            augmentAction = curatedApplication.createAugmentor(
                    "io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem$Factory",
                    Map.of(io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem.SOURCE_PROPERTY,
                            "io.quarkus.test.junit"));
        } else {
            augmentAction = curatedApplication.createAugmentor();
        }
        augmentAction.performCustomBuild(NativeDevServicesHandler.class.getName(), new BiConsumer<String, String>() {
            @Override
            public void accept(String s, String s2) {
                propertyMap.put(s, s2);
            }
        }, DevServicesLauncherConfigResultBuildItem.class.getName(), DevServicesNetworkIdBuildItem.class.getName(),
                DevServicesRegistryBuildItem.class.getName(), DevServicesCustomizerBuildItem.class.getName());

        networkId = propertyMap.get("quarkus.test.container.network");
        boolean manageNetwork = false;
        if (isDockerAppLaunch) {
            if (networkId == null) {
                // use the network the use has specified or else just generate one if none is configured
                Optional<String> networkIdOpt = ConfigProvider.getConfig().getOptionalValue(
                        "quarkus.test.container.network", String.class);
                if (networkIdOpt.isPresent()) {
                    networkId = networkIdOpt.get();
                } else {
                    networkId = "quarkus-integration-test-" + RandomStringUtils.insecure().next(5, true, false);
                    manageNetwork = true;
                }
            }
        }

        DefaultDevServicesLaunchResult result = new DefaultDevServicesLaunchResult(propertyMap, networkId, manageNetwork,
                curatedApplication);
        createNetworkIfNecessary(result);
        return result;
    }

    // this probably isn't the best place for this method, but we need to create the docker container before
    // user code is aware of the network
    private static void createNetworkIfNecessary(
            final ArtifactLauncher.InitContext.DevServicesLaunchResult devServicesLaunchResult) {
        if (devServicesLaunchResult.manageNetwork() && (devServicesLaunchResult.networkId() != null)) {
            ContainerRuntimeUtil.ContainerRuntime containerRuntime = detectContainerRuntime(true);

            try {
                int networkCreateResult = new ProcessBuilder().redirectError(DISCARD).redirectOutput(DISCARD)
                        .command(containerRuntime.getExecutableName(), "network", "create",
                                devServicesLaunchResult.networkId())
                        .start().waitFor();
                if (networkCreateResult > 0) {
                    throw new RuntimeException("Creating container network '" + devServicesLaunchResult.networkId()
                            + "' completed unsuccessfully");
                }
                // do the cleanup in a shutdown hook because there might be more services (launched via QuarkusTestResourceLifecycleManager) connected to the network
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            new ProcessBuilder().redirectError(DISCARD).redirectOutput(DISCARD)
                                    .command(containerRuntime.getExecutableName(), "network", "rm",
                                            devServicesLaunchResult.networkId())
                                    .start()
                                    .waitFor();
                        } catch (InterruptedException | IOException ignored) {
                            System.out.println(
                                    "Unable to delete container network '" + devServicesLaunchResult.networkId() + "'");
                        }
                    }
                }));
            } catch (Exception e) {
                throw new RuntimeException("Creating container network '" + devServicesLaunchResult.networkId()
                        + "' completed unsuccessfully");
            }
        }
    }

    static void activateLogging() {
        // calling this method of the Recorder essentially sets up logging and configures most things
        // based on the provided configuration

        //we need to run this from the TCCL, as we want to activate it from
        //inside the isolated CL, if one exists
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> lrs = cl.loadClass(LoggingSetupRecorder.class.getName());
            lrs.getDeclaredMethod("handleFailedStart").invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static class DefaultDevServicesLaunchResult implements ArtifactLauncher.InitContext.DevServicesLaunchResult {
        private final Map<String, String> properties;
        private final String networkId;
        private final boolean manageNetwork;
        private final CuratedApplication curatedApplication;

        DefaultDevServicesLaunchResult(Map<String, String> properties, String networkId,
                boolean manageNetwork, CuratedApplication curatedApplication) {
            this.properties = properties;
            this.networkId = networkId;
            this.manageNetwork = manageNetwork;
            this.curatedApplication = curatedApplication;
        }

        public Map<String, String> properties() {
            return properties;
        }

        public String networkId() {
            return networkId;
        }

        @Override
        public boolean manageNetwork() {
            return manageNetwork;
        }

        @Override
        public CuratedApplication getCuratedApplication() {
            return curatedApplication;
        }

        @Override
        public void close() {
            curatedApplication.close();
        }
    }

    public static Properties readQuarkusArtifactProperties(ExtensionContext context) {
        Path buildOutputDirectory = determineBuildOutputDirectory(context);
        Path artifactProperties = buildOutputDirectory.resolve("quarkus-artifact.properties");
        if (!Files.exists(artifactProperties)) {
            TestLauncher testLauncher = determineTestLauncher();
            String errorMessage = "Unable to locate the artifact metadata file created that must be created by Quarkus in order to run integration tests. ";
            if (testLauncher == TestLauncher.MAVEN) {
                errorMessage += "Make sure this test is run after 'mvn package'. ";
                if (context.getTestClass().isPresent()) {
                    String testClassName = context.getTestClass().get().getName();
                    if (testClassName.endsWith("Test")) {
                        errorMessage += "The easiest way to ensure this is by having the 'maven-failsafe-plugin' run the test instead of the 'maven-surefire-plugin'.";
                    }
                }
            } else if (testLauncher == TestLauncher.GRADLE) {
                errorMessage += "Make sure this test is run after the 'quarkusBuild' Gradle task.";
            } else {
                errorMessage += "Make sure this test is run after the Quarkus artifact is built from your build tool.";
            }
            throw new IllegalStateException(errorMessage);
        }

        Properties properties = new Properties();
        try (var fis = new FileInputStream(artifactProperties.toFile())) {
            properties.load(fis);
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to read artifact metadata file created that must be created by Quarkus in order to run integration tests.",
                    e);
        }
    }

    static String getEffectiveArtifactType(Properties quarkusArtifactProperties, SmallRyeConfig config) {
        Optional<String> maybeType = config.getOptionalValue("quarkus.test.integration-test-artifact-type", String.class);
        if (maybeType.isPresent()) {
            return maybeType.get();
        }
        return getArtifactType(quarkusArtifactProperties);
    }

    static String getArtifactType(Properties quarkusArtifactProperties) {

        String artifactType = quarkusArtifactProperties.getProperty("type");
        if (artifactType == null) {
            throw new IllegalStateException("Unable to determine the type of artifact created by the Quarkus build");
        }
        return artifactType;
    }

    private static TestLauncher determineTestLauncher() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        int i = stackTrace.length - 1;
        TestLauncher testLauncher = TestLauncher.UNKNOWN;
        while (true) {
            StackTraceElement element = stackTrace[i--];
            String className = element.getClassName();
            if (className.startsWith("org.apache.maven")) {
                testLauncher = TestLauncher.MAVEN;
                break;
            }
            if (className.startsWith("org.gradle")) {
                testLauncher = TestLauncher.GRADLE;
            }
            if (i == 0) {
                break;
            }
        }
        return testLauncher;
    }

    private enum TestLauncher {
        MAVEN,
        GRADLE,
        UNKNOWN
    }

    static Path determineBuildOutputDirectory(ExtensionContext context) {
        String buildOutputDirStr = System.getProperty("build.output.directory");
        Path result = null;
        if (buildOutputDirStr != null) {
            result = Paths.get(buildOutputDirStr);
        } else {
            // we need to guess where the artifact properties file is based on the location of the test class
            Class<?> testClass = context.getRequiredTestClass();
            final CodeSource codeSource = testClass.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                URL codeSourceLocation = codeSource.getLocation();
                Path artifactPropertiesDirectory = determineBuildOutputDirectory(codeSourceLocation);
                if (artifactPropertiesDirectory == null) {
                    throw new IllegalStateException(
                            "Unable to determine the output of the Quarkus build. Consider setting the 'build.output.directory' system property.");
                }
                result = artifactPropertiesDirectory;
            }
        }
        if (result == null) {
            throw new IllegalStateException(
                    "Unable to locate the artifact metadata file created that must be created by Quarkus in order to run tests annotated with '@QuarkusIntegrationTest'.");
        }
        if (!Files.isDirectory(result)) {
            throw new IllegalStateException(
                    "The determined Quarkus build output '" + result.toAbsolutePath().toString() + "' is not a directory");
        }
        return result;
    }

    private static Path determineBuildOutputDirectory(final URL url) {
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file")) {
            if (url.getPath().endsWith("test-classes/")) {
                // we have the maven test classes dir
                return toPath(url).getParent();
            } else if (url.getPath().endsWith("test/") || url.getPath().endsWith("integrationTest/")) {
                // we have the gradle test classes dir, build/classes/java/test
                return toPath(url).getParent().getParent().getParent();
            } else if (url.getPath().contains("/target/surefire/")) {
                // this will make mvn failsafe:integration-test work
                String path = url.getPath();
                int index = path.lastIndexOf("/target/");
                try {
                    return Paths.get(new URI("file:" + (path.substring(0, index) + "/target/")));
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            } else if (url.getPath().endsWith("-tests.jar")) {
                // integration platform test
                final Path baseDir = Path.of("").normalize().toAbsolutePath();
                Path outputDir = baseDir.resolve("target");
                if (Files.exists(outputDir)) {
                    return outputDir;
                }
                outputDir = baseDir.resolve("build");
                if (Files.exists(outputDir)) {
                    return outputDir;
                }
            }
        }
        return null;
    }

    private static Path toPath(URL url) {
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
