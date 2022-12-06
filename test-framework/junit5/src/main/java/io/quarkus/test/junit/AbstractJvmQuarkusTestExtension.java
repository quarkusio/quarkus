package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.Index;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.deployment.dev.testing.CurrentTestApplication;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.common.TestClassIndexer;

public class AbstractJvmQuarkusTestExtension extends AbstractQuarkusTestWithContextExtension {

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";

    protected ClassLoader originalCl;

    protected static Class<? extends QuarkusTestProfile> quarkusTestProfile;

    //needed for @Nested
    protected static final Deque<Class<?>> currentTestClassStack = new ArrayDeque<>();
    protected static Class<?> currentJUnitTestClass;

    protected PrepareResult createAugmentor(ExtensionContext context, Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        Class<?> requiredTestClass = context.getRequiredTestClass();
        currentJUnitTestClass = requiredTestClass;
        Path testClassLocation = getTestClassesLocation(requiredTestClass);
        final Path appClassLocation = getAppClassLocationForTestLocation(testClassLocation.toString());

        final PathList.Builder rootBuilder = PathList.builder();

        Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && Files.exists(path) && !rootBuilder.contains(path)) {
                rootBuilder.add(path);
            }
        };

        if (!appClassLocation.equals(testClassLocation)) {
            addToBuilderIfConditionMet.accept(testClassLocation);
            // if test classes is a dir, we should also check whether test resources dir exists as a separate dir (gradle)
            // TODO: this whole app/test path resolution logic is pretty dumb, it needs be re-worked using proper workspace discovery
            final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(testClassLocation, "test");
            addToBuilderIfConditionMet.accept(testResourcesLocation);
        }

        originalCl = Thread.currentThread().getContextClassLoader();

        // clear the test.url system property as the value leaks into the run when using different profiles
        System.clearProperty("test.url");
        Map<String, String> additional = new HashMap<>();

        QuarkusTestProfile profileInstance = null;
        if (profile != null) {
            profileInstance = profile.getConstructor().newInstance();
            additional.putAll(profileInstance.getConfigOverrides());
            if (!profileInstance.getEnabledAlternatives().isEmpty()) {
                additional.put("quarkus.arc.selected-alternatives", profileInstance.getEnabledAlternatives().stream()
                        .peek((c) -> {
                            if (!c.isAnnotationPresent(Alternative.class)) {
                                throw new RuntimeException(
                                        "Enabled alternative " + c + " is not annotated with @Alternative");
                            }
                        })
                        .map(Class::getName).collect(Collectors.joining(",")));
            }
            if (profileInstance.disableApplicationLifecycleObservers()) {
                additional.put("quarkus.arc.test.disable-application-lifecycle-observers", "true");
            }
            if (profileInstance.getConfigProfile() != null) {
                additional.put(ProfileManager.QUARKUS_TEST_PROFILE_PROP, profileInstance.getConfigProfile());
            }
            //we just use system properties for now
            //it's a lot simpler
            shutdownTasks.add(RestorableSystemProperties.setProperties(additional)::close);
        }

        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();

        addToBuilderIfConditionMet.accept(appClassLocation);
        final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
        addToBuilderIfConditionMet.accept(appResourcesLocation);

        // If gradle project running directly with IDE
        if (System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null) {
            ApplicationModel model = BuildToolHelper.enableGradleAppModelForTest(projectRoot);
            if (model != null && model.getApplicationModule() != null) {
                final ArtifactSources artifactSrc = model.getApplicationModule().getTestSources();
                if (artifactSrc != null) {
                    for (SourceDir src : artifactSrc.getSourceDirs()) {
                        if (Files.exists(src.getOutputDir())) {
                            final Path classesDir = src.getOutputDir();
                            addToBuilderIfConditionMet.accept(classesDir);
                        }
                    }
                }
                for (SourceDir src : model.getApplicationModule().getMainSources().getSourceDirs()) {
                    if (Files.exists(src.getOutputDir())) {
                        final Path classesDir = src.getOutputDir();
                        addToBuilderIfConditionMet.accept(classesDir);
                    }
                }
            }
        } else if (System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR) != null) {
            final String[] sourceDirectories = System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR).split(",");
            for (String sourceDirectory : sourceDirectories) {
                final Path directory = Paths.get(sourceDirectory);
                addToBuilderIfConditionMet.accept(directory);
            }
        }
        CuratedApplication curatedApplication;
        if (CurrentTestApplication.curatedApplication != null) {
            curatedApplication = CurrentTestApplication.curatedApplication;
        } else {
            final QuarkusBootstrap.Builder runnerBuilder = QuarkusBootstrap.builder()
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.TEST);
            runnerBuilder.setTargetDirectory(PathTestHelper.getProjectBuildDir(projectRoot, testClassLocation));
            runnerBuilder.setProjectRoot(projectRoot);
            runnerBuilder.setApplicationRoot(rootBuilder.build());

            curatedApplication = runnerBuilder
                    .setTest(true)
                    .build()
                    .bootstrap();
            shutdownTasks.add(curatedApplication::close);
        }

        if (curatedApplication.getApplicationModel().getRuntimeDependencies().isEmpty()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus project. Please ensure that the test is configured to use the proper working directory.");
        }

        Index testClassesIndex = TestClassIndexer.indexTestClasses(requiredTestClass);
        // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
        TestClassIndexer.writeIndex(testClassesIndex, requiredTestClass);

        Timing.staticInitStarted(curatedApplication.getBaseRuntimeClassLoader(),
                curatedApplication.getQuarkusBootstrap().isAuxiliaryApplication());
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        quarkusTestProfile = profile;
        return new PrepareResult(curatedApplication
                .createAugmentor(QuarkusTestExtension.TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication);
    }

    protected Class<? extends QuarkusTestProfile> getQuarkusTestProfile(ExtensionContext extensionContext) {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        while (testClass != null) {
            TestProfile annotation = testClass.getAnnotation(TestProfile.class);
            if (annotation != null) {
                return annotation.value();
            }

            testClass = testClass.getEnclosingClass();
        }

        return null;
    }

    protected static boolean hasPerTestResources(ExtensionContext extensionContext) {
        return hasPerTestResources(extensionContext.getRequiredTestClass());
    }

    public static boolean hasPerTestResources(Class<?> requiredTestClass) {
        while (requiredTestClass != Object.class) {
            for (QuarkusTestResource testResource : requiredTestClass.getAnnotationsByType(QuarkusTestResource.class)) {
                if (testResource.restrictToAnnotatedClass()) {
                    return true;
                }
            }
            // scan for meta-annotations
            for (Annotation annotation : requiredTestClass.getAnnotations()) {
                // skip TestResource annotations
                if (annotation.annotationType() != QuarkusTestResource.class) {
                    // look for a TestResource on the annotation itself
                    if (annotation.annotationType().getAnnotationsByType(QuarkusTestResource.class).length > 0) {
                        // meta-annotations are per-test scoped for now
                        return true;
                    }
                }
            }
            // look up
            requiredTestClass = requiredTestClass.getSuperclass();
        }
        return false;
    }

    protected static class PrepareResult {
        protected final AugmentAction augmentAction;
        protected final QuarkusTestProfile profileInstance;
        protected final CuratedApplication curatedApplication;

        public PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
                CuratedApplication curatedApplication) {
            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
        }
    }
}
