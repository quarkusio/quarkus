package io.quarkus.test.junit;

import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_HTTPS_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.DEFAULT_PORT;
import static io.quarkus.test.junit.IntegrationTestUtil.determineTestProfileAndProperties;
import static io.quarkus.test.junit.IntegrationTestUtil.doProcessTestInstance;
import static io.quarkus.test.junit.IntegrationTestUtil.ensureNoInjectAnnotationIsUsed;
import static io.quarkus.test.junit.IntegrationTestUtil.getAdditionalTestResources;
import static io.quarkus.test.junit.IntegrationTestUtil.getSysPropsToRestore;
import static io.quarkus.test.junit.IntegrationTestUtil.handleDevServices;
import static io.quarkus.test.junit.IntegrationTestUtil.startLauncher;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.opentest4j.TestAbortedException;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.DefaultNativeImageLauncher;
import io.quarkus.test.common.LauncherUtil;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.junit.launcher.ConfigUtil;
import io.quarkus.test.junit.launcher.NativeImageLauncherProvider;

public class NativeTestExtension extends AbstractQuarkusTestWithContextExtension
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

    private QuarkusTestExtensionState ensureStarted(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass);

        QuarkusTestExtensionState state = getState(extensionContext);
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
                setState(extensionContext, state);

            } catch (Throwable e) {
                failedBoot = true;
                firstException = e;
            }
        }
        return state;
    }

    private QuarkusTestExtensionState doNativeStart(ExtensionContext context, Class<? extends QuarkusTestProfile> profile)
            throws Throwable {
        Map<String, String> devServicesProps = handleDevServices(context, false).properties();
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
                    getAdditionalTestResources(testProfileAndProperties.testProfile, currentJUnitTestClass.getClassLoader()),
                    testProfileAndProperties.testProfile != null
                            && testProfileAndProperties.testProfile.disableGlobalTestResources());
            testResourceManager.init(
                    testProfileAndProperties.testProfile != null ? testProfileAndProperties.testProfile.getClass().getName()
                            : null);
            hasPerTestResources = testResourceManager.hasPerTestResources();

            Map<String, String> additionalProperties = new HashMap<>(testProfileAndProperties.properties);
            Map<String, String> resourceManagerProps = new HashMap<>(devServicesProps);
            // Allow override of dev services props by integration test extensions
            resourceManagerProps.putAll(testResourceManager.start());
            Map<String, String> old = new HashMap<>();
            //we also make the dev services config accessible from the test itself
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
            //this includes dev services props
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
                ConfigUtil.waitTimeValue(config),
                config.getOptionalValue("quarkus.test.native-image-profile", String.class).orElse(null),
                ConfigUtil.argLineValue(config),
                ConfigUtil.env(config),
                new ArtifactLauncher.InitContext.DevServicesLaunchResult() {
                    @Override
                    public Map<String, String> properties() {
                        return Collections.emptyMap();
                    }

                    @Override
                    public String networkId() {
                        return null;
                    }

                    @Override
                    public boolean manageNetwork() {
                        return false;
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public CuratedApplication getCuratedApplication() {
                        return null;
                    }
                },
                System.getProperty("native.image.path"),
                config.getOptionalValue("quarkus.package.output-directory", String.class).orElse(null),
                requiredTestClass));
        return launcher;
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
        ensureStarted(context);
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
