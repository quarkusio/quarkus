package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.*;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.TestAbortedException;

import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DockerContainerLauncher;
import io.quarkus.test.common.JarLauncher;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;

public class QuarkusIntegrationTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, TestInstancePostProcessor {

    private static boolean failedBoot;

    private static List<Function<Class<?>, String>> testHttpEndpointProviders;
    private static boolean ssl;

    private static Class<? extends QuarkusTestProfile> quarkusTestProfile;
    private static Throwable firstException; //if this is set then it will be thrown from the very first test that is run, the rest are aborted

    private static Class<?> currentJUnitTestClass;
    private static boolean hasPerTestResources;

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            RestAssuredURLManager.clearURL();
            TestScopeManager.tearDown(true);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (failedBoot) {
            throwBootFailureException();
        } else {
            RestAssuredURLManager.setURL(ssl, QuarkusTestExtension.getEndpointPath(context, testHttpEndpointProviders));
            TestScopeManager.setup(true);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        ensureStarted(extensionContext);
    }

    private IntegrationTestExtensionState ensureStarted(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass);
        Properties quarkusArtifactProperties = readQuarkusArtifactProperties(extensionContext);

        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        IntegrationTestExtensionState state = store.get(IntegrationTestExtensionState.class.getName(),
                IntegrationTestExtensionState.class);
        Class<? extends QuarkusTestProfile> selectedProfile = IntegrationTestUtil.findProfile(testClass);
        boolean wrongProfile = !Objects.equals(selectedProfile, quarkusTestProfile);
        // we reload the test resources if we changed test class and if we had or will have per-test test resources
        boolean reloadTestResources = !Objects.equals(extensionContext.getRequiredTestClass(), currentJUnitTestClass)
                && (hasPerTestResources || QuarkusTestExtension.hasPerTestResources(extensionContext));
        if ((state == null && !failedBoot) || wrongProfile || reloadTestResources) {
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
                state = doProcessStart(quarkusArtifactProperties, selectedProfile, extensionContext);
                store.put(IntegrationTestExtensionState.class.getName(), state);
            } catch (Throwable e) {
                failedBoot = true;
                firstException = e;
            }
        }
        return state;
    }

    private Properties readQuarkusArtifactProperties(ExtensionContext context) {
        Path buildOutputDirectory = determineBuildOutputDirectory(context);
        Path artifactProperties = buildOutputDirectory.resolve("quarkus-artifact.properties");
        if (!Files.exists(artifactProperties)) {
            throw new IllegalStateException(
                    "Unable to locate the artifact metadata file created that must be created by Quarkus in order to run tests annotated with '@QuarkusIntegrationTest'.");
        }
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(artifactProperties.toFile()));
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to read artifact metadata file created that must be created by Quarkus in order to run tests annotated with '@QuarkusIntegrationTest'.",
                    e);
        }
    }

    private Path determineBuildOutputDirectory(ExtensionContext context) {
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
                File artifactPropertiesDirectory = determineBuildOutputDirectory(codeSourceLocation);
                if (artifactPropertiesDirectory == null) {
                    throw new IllegalStateException(
                            "Unable to determine the output of the Quarkus build. Consider setting the 'build.output.directory' system property.");
                }
                result = artifactPropertiesDirectory.toPath();
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

    private static File determineBuildOutputDirectory(final URL url) {
        if (url == null) {
            return null;
        }
        if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
            //we have the maven test classes dir
            File testClasses = new File(url.getPath());
            return testClasses.getParentFile();
        } else if (url.getProtocol().equals("file") && url.getPath().endsWith("test/")) {
            //we have the gradle test classes dir, build/classes/java/test
            File testClasses = new File(url.getPath());
            return testClasses.getParentFile().getParentFile().getParentFile();
        } else if (url.getProtocol().equals("file") && url.getPath().contains("/target/surefire/")) {
            //this will make mvn failsafe:integration-test work
            String path = url.getPath();
            int index = path.lastIndexOf("/target/");
            return new File(path.substring(0, index) + "/target/");
        }
        return null;
    }

    private IntegrationTestExtensionState doProcessStart(Properties quarkusArtifactProperties,
            Class<? extends QuarkusTestProfile> profile, ExtensionContext context)
            throws Throwable {
        Map<String, String> devDbProps = handleDevDb(context);
        quarkusTestProfile = profile;
        currentJUnitTestClass = context.getRequiredTestClass();
        TestResourceManager testResourceManager = null;
        try {
            Class<?> requiredTestClass = context.getRequiredTestClass();

            Map<String, String> sysPropRestore = getSysPropsToRestore();
            TestProfileAndProperties testProfileAndProperties = determineTestProfileAndProperties(profile, sysPropRestore);

            testResourceManager = new TestResourceManager(requiredTestClass, quarkusTestProfile,
                    Collections.emptyList(), testProfileAndProperties.testProfile != null
                            && testProfileAndProperties.testProfile.disableGlobalTestResources());
            testResourceManager.init();
            hasPerTestResources = testResourceManager.hasPerTestResources();

            Map<String, String> additionalProperties = new HashMap<>(testProfileAndProperties.properties);
            additionalProperties.putAll(devDbProps);
            additionalProperties.putAll(testResourceManager.start());

            String artifactType = quarkusArtifactProperties.getProperty("type");
            if (artifactType == null) {
                throw new IllegalStateException("Unable to determine the type of artifact created by the Quarkus build");
            }
            ArtifactLauncher launcher;
            // TODO: replace with ServiceLoader mechanism
            switch (artifactType) {
                case "native": {
                    String pathStr = quarkusArtifactProperties.getProperty("path");
                    if ((pathStr != null) && !pathStr.isEmpty()) {
                        String previousNativeImagePathValue = System.setProperty("native.image.path",
                                determineBuildOutputDirectory(context).resolve(pathStr).toAbsolutePath().toString());
                        sysPropRestore.put("native.image.path", previousNativeImagePathValue);
                        launcher = new NativeImageLauncher(requiredTestClass);
                    } else {
                        throw new IllegalStateException("The path of the native binary could not be determined");
                    }
                    break;
                }
                case "jar": {
                    String pathStr = quarkusArtifactProperties.getProperty("path");
                    if ((pathStr != null) && !pathStr.isEmpty()) {
                        launcher = new JarLauncher(determineBuildOutputDirectory(context).resolve(pathStr));
                    } else {
                        throw new IllegalStateException("The path of the native binary could not be determined");
                    }
                    break;
                }
                case "jar-container":
                case "native-container":
                    String containerImage = quarkusArtifactProperties.getProperty("metadata.container-image");
                    if ((containerImage != null) && !containerImage.isEmpty()) {
                        launcher = new DockerContainerLauncher(containerImage);
                    } else {
                        throw new IllegalStateException("The container image to be launched could not be determined");
                    }
                    break;
                default:
                    throw new IllegalStateException(
                            "Artifact type + '" + artifactType + "' is not supported by @QuarkusIntegrationTest");
            }

            startLauncher(launcher, additionalProperties, () -> ssl = true);

            IntegrationTestExtensionState state = new IntegrationTestExtensionState(testResourceManager, launcher,
                    sysPropRestore);
            testHttpEndpointProviders = TestHttpEndpointProvider.load();

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
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        if (!failedBoot) {
            doProcessTestInstance(testInstance, context);
        }
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
}
