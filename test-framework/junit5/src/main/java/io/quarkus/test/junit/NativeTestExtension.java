package io.quarkus.test.junit;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.inject.Inject;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.JUnitException;

import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.NativeImageLauncher;
import io.quarkus.test.common.PropertyTestUtil;
import io.quarkus.test.common.RestAssuredURLManager;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.common.TestScopeManager;
import io.quarkus.test.common.http.TestHTTPResourceManager;

public class NativeTestExtension
        implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, TestInstancePostProcessor {

    private static boolean failedBoot;

    private static List<Function<Class<?>, String>> testHttpEndpointProviders;
    private static boolean ssl;

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            RestAssuredURLManager.clearURL();
            TestScopeManager.tearDown(true);
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (!failedBoot) {
            RestAssuredURLManager.setURL(ssl, QuarkusTestExtension.getEndpointPath(context, testHttpEndpointProviders));
            TestScopeManager.setup(true);
        }
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        ensureNoInjectAnnotationIsUsed(testClass);
        ExtensionContext root = extensionContext.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        PropertyTestUtil.setLogFileProperty();
        if (state == null) {
            ensureNoTestProfile(testClass);

            TestResourceManager testResourceManager = new TestResourceManager(testClass);
            try {
                testResourceManager.init();
                Map<String, String> systemProps = testResourceManager.start();
                NativeImageLauncher launcher = new NativeImageLauncher(testClass);
                launcher.addSystemProperties(systemProps);
                try {
                    launcher.start();
                } catch (IOException e) {
                    try {
                        launcher.close();
                    } catch (Throwable t) {
                    }
                    throw e;
                }
                if (launcher.isDefaultSsl()) {
                    ssl = true;
                }
                state = new ExtensionState(testResourceManager, launcher, true);
                store.put(ExtensionState.class.getName(), state);

                testHttpEndpointProviders = TestHttpEndpointProvider.load();
            } catch (Exception e) {

                failedBoot = true;
                throw new JUnitException("Quarkus native image start failed, original cause: " + e, e);
            }
        }
    }

    private void ensureNoInjectAnnotationIsUsed(Class<?> testClass) {
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                Inject injectAnnotation = field.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                    throw new JUnitException(
                            "@Inject is not supported in NativeImageTest tests. Offending field is "
                                    + field.getDeclaringClass().getTypeName() + "."
                                    + field.getName());
                }
            }
            current = current.getSuperclass();
        }

    }

    /**
     * We don't support {@link TestProfile} in native tests because we don't want to incur the native binary rebuild cost
     * which is very high.
     *
     * This method looks up the annotations via Jandex in order to try and prevent the image generation if there are
     * any cases of {@link NativeImageTest} being used with {@link TestProfile}
     */
    private void ensureNoTestProfile(Class<?> testClass) {
        Index index = TestClassIndexer.readIndex(testClass);
        List<AnnotationInstance> instances = index.getAnnotations(DotName.createSimple(NativeImageTest.class.getName()));
        for (AnnotationInstance instance : instances) {
            if (instance.target().kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            ClassInfo testClassInfo = instance.target().asClass();
            if (testClassInfo.classAnnotation(DotName.createSimple(TestProfile.class.getName())) != null) {
                throw new JUnitException(
                        "@TestProfile is not supported in NativeImageTest tests. Offending class is " + testClassInfo.name());
            }
        }
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) throws Exception {
        TestHTTPResourceManager.inject(testInstance);
        ExtensionContext root = context.getRoot();
        ExtensionContext.Store store = root.getStore(ExtensionContext.Namespace.GLOBAL);
        ExtensionState state = store.get(ExtensionState.class.getName(), ExtensionState.class);
        state.testResourceManager.inject(testInstance);
    }

    public class ExtensionState implements ExtensionContext.Store.CloseableResource {

        private final TestResourceManager testResourceManager;
        private final Closeable resource;

        ExtensionState(TestResourceManager testResourceManager, Closeable resource, boolean nativeImage) {
            this.testResourceManager = testResourceManager;
            this.resource = resource;
        }

        @Override
        public void close() throws Throwable {
            testResourceManager.close();
            resource.close();
        }
    }
}
