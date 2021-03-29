package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.JUnitException;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.PathsCollection;
import io.quarkus.bootstrap.resolver.model.QuarkusModel;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.common.ArtifactLauncher;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.TestClassIndexer;
import io.quarkus.test.common.http.TestHTTPResourceManager;

final class IntegrationTestUtil {

    private IntegrationTestUtil() {
    }

    static void ensureNoInjectAnnotationIsUsed(Class<?> testClass) {
        Class<?> current = testClass;
        while (current.getSuperclass() != null) {
            for (Field field : current.getDeclaredFields()) {
                Inject injectAnnotation = field.getAnnotation(Inject.class);
                if (injectAnnotation != null) {
                    throw new JUnitException(
                            "@Inject is not supported in @NativeImageTest and @QuarkusIntegrationTest tests. Offending field is "
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
        IntegrationTestExtensionState state = store.get(IntegrationTestExtensionState.class.getName(),
                IntegrationTestExtensionState.class);
        state.getTestResourceManager().inject(testInstance);
    }

    static Map<String, String> getSysPropsToRestore() {
        Map<String, String> sysPropRestore = new HashMap<>();
        sysPropRestore.put(ProfileManager.QUARKUS_TEST_PROFILE_PROP,
                System.getProperty(ProfileManager.QUARKUS_TEST_PROFILE_PROP));
        return sysPropRestore;
    }

    static TestProfileAndProperties determineTestProfileAndProperties(Class<? extends QuarkusTestProfile> profile,
            Map<String, String> sysPropRestore) throws InstantiationException, IllegalAccessException {
        final Map<String, String> properties = new HashMap<>();
        QuarkusTestProfile testProfile = null;
        if (profile != null) {
            testProfile = profile.newInstance();
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
                properties.put(ProfileManager.QUARKUS_PROFILE_PROP, configProfile);
            }
            properties.put("quarkus.configuration.build-time-mismatch-at-runtime", "fail");
            for (Map.Entry<String, String> i : properties.entrySet()) {
                sysPropRestore.put(i.getKey(), System.getProperty(i.getKey()));
            }
            for (Map.Entry<String, String> i : properties.entrySet()) {
                System.setProperty(i.getKey(), i.getValue());
            }
        }
        return new TestProfileAndProperties(testProfile, properties);
    }

    static void startLauncher(ArtifactLauncher launcher, Map<String, String> additionalProperties, Runnable sslSetter)
            throws IOException {
        launcher.addSystemProperties(additionalProperties);
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

    static Map<String, String> handleDevDb(ExtensionContext context) throws Exception {
        Class<?> requiredTestClass = context.getRequiredTestClass();
        Path testClassLocation = getTestClassesLocation(requiredTestClass);
        final Path appClassLocation = getAppClassLocationForTestLocation(testClassLocation.toString());

        PathsCollection.Builder rootBuilder = PathsCollection.builder();

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
        QuarkusTestProfile profileInstance = null;

        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();
        runnerBuilder.setProjectRoot(projectRoot);
        Path outputDir;
        try {
            // this should work for both maven and gradle
            outputDir = projectRoot.resolve(projectRoot.relativize(testClassLocation).getName(0));
        } catch (Exception e) {
            // this shouldn't happen since testClassLocation is usually found under the project dir
            outputDir = projectRoot;
        }
        runnerBuilder.setTargetDirectory(outputDir);

        rootBuilder.add(appClassLocation);
        final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
        if (appResourcesLocation != null) {
            rootBuilder.add(appResourcesLocation);
        }

        // If gradle project running directly with IDE
        if (System.getProperty(BootstrapConstants.SERIALIZED_APP_MODEL) == null) {
            QuarkusModel model = BuildToolHelper.enableGradleAppModelForTest(projectRoot);
            if (model != null) {
                final Set<File> classDirectories = model.getWorkspace().getMainModule().getSourceSet()
                        .getSourceDirectories();
                for (File classes : classDirectories) {
                    if (classes.exists() && !rootBuilder.contains(classes.toPath())) {
                        rootBuilder.add(classes.toPath());
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
        curatedApplication
                .createAugmentor()
                .performCustomBuild(NativeDevServicesDatasourceHandler.class.getName(), new BiConsumer<String, String>() {
                    @Override
                    public void accept(String s, String s2) {
                        propertyMap.put(s, s2);
                    }
                }, DevServicesDatasourceResultBuildItem.class.getName());
        return propertyMap;
    }
}
