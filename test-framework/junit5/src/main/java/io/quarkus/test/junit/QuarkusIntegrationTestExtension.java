package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.*;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;

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
            Map<String, String> resourceManagerProps = testResourceManager.start();
            Map<String, String> old = new HashMap<>();
            for (Map.Entry<String, String> i : resourceManagerProps.entrySet()) {
                old.put(i.getKey(), System.getProperty(i.getKey()));
                if (i.getValue() == null) {
                    System.clearProperty(i.getKey());
                } else {
                    System.setProperty(i.getKey(), i.getValue());
                }
            }
            context.getStore(ExtensionContext.Namespace.GLOBAL).put(NativeTestExtension.class.getName() + ".systemProps",
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
                    boolean pullRequired = Boolean
                            .parseBoolean(quarkusArtifactProperties.getProperty("metadata.pull-required", "false"));
                    if ((containerImage != null) && !containerImage.isEmpty()) {
                        launcher = new DockerContainerLauncher(containerImage, pullRequired);
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
