package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_JAR_WAIT_TIME_SECONDS;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.determineTestProfileAndProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.doProcessTestInstance;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;
import static io.quarkus.test.junit.IntegrationTestUtil.getSysPropsToRestore;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevDb;
import static io.quarkus.test.junit.IntegrationTestUtil.startLauncher;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.TestAbortedException;

import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.DefaultNativeImageLauncher;
import io.quarkus.test.common.LauncherUtil;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.junit.launcher.NativeImageLauncherProvider;

public class NativeTestExtension
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
                state = doNativeStart(extensionContext, selectedProfile);
                store.put(IntegrationTestExtensionState.class.getName(), state);

            } catch (Throwable e) {
                failedBoot = true;
                firstException = e;
            }
        }
        return state;
    }

    private IntegrationTestExtensionState doNativeStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile)
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
                    getAdditionalTestResources(testProfileAndProperties.testProfile, currentJUnitTestClass.getClassLoader()),
                    testProfileAndProperties.testProfile != null
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

            NativeImageLauncher launcher = createLauncher(requiredTestClass);
            startLauncher(launcher, additionalProperties, () -> ssl = true);

            final IntegrationTestExtensionState state = new IntegrationTestExtensionState(testResourceManager,
                    launcher, sysPropRestore);

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

    private DefaultNativeImageLauncher createLauncher(Class<?> requiredTestClass) {
        DefaultNativeImageLauncher launcher = new DefaultNativeImageLauncher();
        Config config = LauncherUtil.installAndGetSomeConfig();
        launcher.init(new NativeImageLauncherProvider.DefaultNativeImageInitContext(
                config.getValue("quarkus.http.test-port", OptionalInt.class).orElse(DEFAULT_PORT),
                config.getValue("quarkus.http.test-ssl-port", OptionalInt.class).orElse(DEFAULT_HTTPS_PORT),
                Duration.ofSeconds(config.getValue("quarkus.test.jar-wait-time", OptionalLong.class)
                        .orElse(DEFAULT_JAR_WAIT_TIME_SECONDS)),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                System.getProperty("native.image.path"),
                requiredTestClass));
        return launcher;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        if (!failedBoot) {
            doProcessTestInstance(testInstance, context);
        }
    }

    private void throwBootFailureException() throws Exception {
        if (firstException != null) {
            Throwable throwable = firstException;
            firstException = null;

            if (throwable instanceof Exception) {
                throw (Exception) throwable;
            }

            throw new RuntimeException(throwable);
        } else {
            throw new TestAbortedException("Boot failed");
        }
    }

}
