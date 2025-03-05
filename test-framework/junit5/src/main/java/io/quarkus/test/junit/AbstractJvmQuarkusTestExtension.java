package io.quarkus.test.junit;

import static io.quarkus.commons.classloading.ClassLoaderHelper.fromClassNameToResourceName;
import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.Index;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.deployment.dev.testing.CurrentTestApplication;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.common.TestClassIndexer;
import io.smallrye.config.SmallRyeConfig;

public class AbstractJvmQuarkusTestExtension extends AbstractQuarkusTestWithContextExtension
        implements ExecutionCondition {

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    protected static final String TEST_PROFILE = "test-profile";

    protected ClassLoader originalCl;

    protected static Class<? extends QuarkusTestProfile> quarkusTestProfile;

    //needed for @Nested
    protected static final Deque<Class<?>> currentTestClassStack = new ArrayDeque<>();
    protected static Class<?> currentJUnitTestClass;

    protected PrepareResult createAugmentor(ExtensionContext context, Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        final PathList.Builder rootBuilder = PathList.builder();
        Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && Files.exists(path) && !rootBuilder.contains(path)) {
                rootBuilder.add(path);
            }
        };

        final Class<?> requiredTestClass = context.getRequiredTestClass();
        currentJUnitTestClass = requiredTestClass;

        final Path testClassLocation;
        final Path appClassLocation;
        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();

        final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
        // If gradle project running directly with IDE
        if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
            final WorkspaceModule module = gradleAppModel.getApplicationModule();
            final String testClassFileName = fromClassNameToResourceName(requiredTestClass.getName());
            Path testClassesDir = null;
            for (String classifier : module.getSourceClassifiers()) {
                final ArtifactSources sources = module.getSources(classifier);
                if (sources.isOutputAvailable() && sources.getOutputTree().contains(testClassFileName)) {
                    for (SourceDir src : sources.getSourceDirs()) {
                        addToBuilderIfConditionMet.accept(src.getOutputDir());
                        if (Files.exists(src.getOutputDir().resolve(testClassFileName))) {
                            testClassesDir = src.getOutputDir();
                        }
                    }
                    for (SourceDir src : sources.getResourceDirs()) {
                        addToBuilderIfConditionMet.accept(src.getOutputDir());
                    }
                    for (SourceDir src : module.getMainSources().getSourceDirs()) {
                        addToBuilderIfConditionMet.accept(src.getOutputDir());
                    }
                    for (SourceDir src : module.getMainSources().getResourceDirs()) {
                        addToBuilderIfConditionMet.accept(src.getOutputDir());
                    }
                    break;
                }
            }
            if (testClassesDir == null) {
                final StringBuilder sb = new StringBuilder();
                sb.append("Failed to locate ").append(requiredTestClass.getName()).append(" in ");
                for (String classifier : module.getSourceClassifiers()) {
                    final ArtifactSources sources = module.getSources(classifier);
                    if (sources.isOutputAvailable()) {
                        for (SourceDir d : sources.getSourceDirs()) {
                            if (Files.exists(d.getOutputDir())) {
                                sb.append(System.lineSeparator()).append(d.getOutputDir());
                            }
                        }
                    }
                }
                throw new RuntimeException(sb.toString());
            }
            testClassLocation = testClassesDir;

        } else {
            if (System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR) != null) {
                final String[] sourceDirectories = System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR).split(",");
                for (String sourceDirectory : sourceDirectories) {
                    final Path directory = Paths.get(sourceDirectory);
                    addToBuilderIfConditionMet.accept(directory);
                }
            }

            testClassLocation = getTestClassesLocation(requiredTestClass);
            appClassLocation = getAppClassLocationForTestLocation(testClassLocation);
            if (!appClassLocation.equals(testClassLocation)) {
                addToBuilderIfConditionMet.accept(testClassLocation);
                // if test classes is a dir, we should also check whether test resources dir exists as a separate dir (gradle)
                // TODO: this whole app/test path resolution logic is pretty dumb, it needs be re-worked using proper workspace discovery
                final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(testClassLocation, "test");
                addToBuilderIfConditionMet.accept(testResourcesLocation);
            }

            addToBuilderIfConditionMet.accept(appClassLocation);
            final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
            addToBuilderIfConditionMet.accept(appResourcesLocation);
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
                additional.put(LaunchMode.TEST.getProfileKey(), profileInstance.getConfigProfile());
            }
            //we just use system properties for now
            //it's a lot simpler
            shutdownTasks.add(RestorableSystemProperties.setProperties(additional)::close);
        }

        CuratedApplication curatedApplication;
        if (CurrentTestApplication.curatedApplication != null) {
            curatedApplication = CurrentTestApplication.curatedApplication;
        } else {
            curatedApplication = QuarkusBootstrap.builder()
                    //.setExistingModel(gradleAppModel) unfortunately this model is not re-usable due to PathTree serialization by Gradle
                    .setBaseName(context.getDisplayName() + " (QuarkusTest)")
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.TEST)
                    .setTest(true)
                    .setTargetDirectory(PathTestHelper.getProjectBuildDir(projectRoot, testClassLocation))
                    .setProjectRoot(projectRoot)
                    .setApplicationRoot(rootBuilder.build())
                    .build()
                    .bootstrap();
            shutdownTasks.add(curatedApplication::close);
        }

        if (curatedApplication.getApplicationModel().getRuntimeDependencies().isEmpty()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus project. Please ensure that the test is configured to use the proper working directory.");
        }

        Index testClassesIndex = TestClassIndexer.indexTestClasses(testClassLocation);
        // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
        TestClassIndexer.writeIndex(testClassesIndex, testClassLocation, requiredTestClass);

        Timing.staticInitStarted(curatedApplication.getOrCreateBaseRuntimeClassLoader(),
                curatedApplication.getQuarkusBootstrap().isAuxiliaryApplication());
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        if (profile != null) {
            props.put(TEST_PROFILE, profile.getName());
        }
        quarkusTestProfile = profile;
        return new PrepareResult(curatedApplication
                .createAugmentor(TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication, testClassLocation);
    }

    private ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException, AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    protected Class<? extends QuarkusTestProfile> getQuarkusTestProfile(ExtensionContext extensionContext) {
        // If the current class or any enclosing class in its hierarchy is annotated with `@TestProfile`.
        Class<? extends QuarkusTestProfile> testProfile = findTestProfileAnnotation(extensionContext.getRequiredTestClass());
        if (testProfile != null) {
            return testProfile;
        }

        // Otherwise, if the current class is annotated with `@Nested`:
        if (extensionContext.getRequiredTestClass().isAnnotationPresent(Nested.class)) {
            // let's try to find the `@TestProfile` from the enclosing classes:
            testProfile = findTestProfileAnnotation(extensionContext.getRequiredTestClass().getEnclosingClass());
            if (testProfile != null) {
                return testProfile;
            }

            // if not found, let's try the parents
            Optional<ExtensionContext> parentContext = extensionContext.getParent();
            while (parentContext.isPresent()) {
                ExtensionContext currentExtensionContext = parentContext.get();
                if (currentExtensionContext.getTestClass().isEmpty()) {
                    break;
                }

                testProfile = findTestProfileAnnotation(currentExtensionContext.getTestClass().get());
                if (testProfile != null) {
                    return testProfile;
                }

                parentContext = currentExtensionContext.getParent();
            }
        }

        return null;
    }

    private Class<? extends QuarkusTestProfile> findTestProfileAnnotation(Class<?> clazz) {
        Class<?> testClass = clazz;
        while (testClass != null) {
            TestProfile annotation = testClass.getAnnotation(TestProfile.class);
            if (annotation != null) {
                return annotation.value();
            }

            testClass = testClass.getEnclosingClass();
        }

        return null;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (!context.getTestClass().isPresent()) {
            return ConditionEvaluationResult.enabled("No test class specified");
        }
        if (context.getTestInstance().isPresent()) {
            return ConditionEvaluationResult.enabled("Quarkus Test Profile tags only affect classes");
        }
        SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);
        TestConfig testConfig = config.getConfigMapping(TestConfig.class);
        Optional<List<String>> tags = testConfig.profile().tags();
        if (tags.isEmpty() || tags.get().isEmpty()) {
            return ConditionEvaluationResult.enabled("No Quarkus Test Profile tags");
        }
        Class<? extends QuarkusTestProfile> testProfile = getQuarkusTestProfile(context);
        if (testProfile == null) {
            return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                    + "' is not annotated with '@QuarkusTestProfile' but 'quarkus.profile.test.tags' was set");
        }
        QuarkusTestProfile profileInstance;
        try {
            profileInstance = testProfile.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Set<String> testProfileTags = profileInstance.tags();
        for (String tag : tags.get()) {
            String trimmedTag = tag.trim();
            if (testProfileTags.contains(trimmedTag)) {
                return ConditionEvaluationResult.enabled("Tag '" + trimmedTag + "' is present on '" + testProfile
                        + "' which is used on test '" + context.getRequiredTestClass());
            }
        }
        return ConditionEvaluationResult.disabled("Test '" + context.getRequiredTestClass()
                + "' disabled because 'quarkus.profile.test.tags' don't match the tags of '" + testProfile + "'");
    }

    protected static class PrepareResult {
        protected final AugmentAction augmentAction;
        protected final QuarkusTestProfile profileInstance;
        protected final CuratedApplication curatedApplication;
        protected final Path testClassLocation;

        public PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
                CuratedApplication curatedApplication, Path testClassLocation) {
            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
            this.testClassLocation = testClassLocation;
        }
    }
}
