package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;
import static io.quarkus.test.common.PathTestHelper.validateTestDir;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.Index;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.commons.classloading.ClassLoaderHelper;
import io.quarkus.maven.dependency.DependencyFlags;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.common.TestClassIndexer;

public class AppMakerHelper {

    // TODO Copied from superclass of thing we copied
    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    protected static final String TEST_PROFILE = "test-profile";

    /// end copied

    public static ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException, AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    static PrepareResult prepare(final Class<?> requiredTestClass,
            CuratedApplication curatedApplication,
            Class<? extends QuarkusTestProfile> profile)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        Path testClassLocation = getTestClassesLocation(requiredTestClass, curatedApplication);

        // clear the test.url system property as the value leaks into the run when using different profiles
        System.clearProperty("test.url");

        // TODO should we do this here, or when we prepare the curated application?
        // Or is it needed at all?
        Index testClassesIndex = TestClassIndexer.indexTestClasses(testClassLocation);
        // we need to write the Index to make it reusable from other parts of the testing infrastructure that run in different ClassLoaders
        TestClassIndexer.writeIndex(testClassesIndex, testClassLocation, requiredTestClass);

        Timing.staticInitStarted(curatedApplication
                .getOrCreateBaseRuntimeClassLoader(),
                curatedApplication
                        .getQuarkusBootstrap()
                        .isAuxiliaryApplication());
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        if (profile != null) {
            props.put(TEST_PROFILE, profile.getName());
        }
        return new PrepareResult(curatedApplication
                .createAugmentor(TestBuildChainFunction.class.getName(), props), getQuarkusTestProfile(profile),
                curatedApplication);
    }

    static QuarkusTestProfile getQuarkusTestProfile(Class<? extends QuarkusTestProfile> profile)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        return profile == null ? null : new ClassCoercingTestProfile(profile.getConstructor().newInstance());
    }

    /**
     * Reads properties from a profile, sets them as system properties, and returns a Runnable which can be invoked to remove
     * them back off of
     * system properties.
     */
    static Runnable setExtraPropertiesRestorably(Class<?> profileClass, QuarkusTestProfile profileInstance) {
        final Map<String, String> additional = new HashMap<>();
        // We apply the profile config twice, once before augmentation, and once before app start
        // That's a bit awkward, but both augmentation and app start need to have the right config for their profile
        additional.putAll(profileInstance.getConfigOverrides());
        if (!profileInstance.getEnabledAlternatives().isEmpty()) {
            additional.put("quarkus.arc.selected-alternatives", profileInstance.getEnabledAlternatives()
                    .stream()
                    .peek((c) -> {
                        try {
                            // TODO is string comparison more efficient?
                            if (!c.isAnnotationPresent((Class<? extends Annotation>) profileClass.getClassLoader()
                                    .loadClass(Alternative.class.getName()))) {
                                throw new RuntimeException(
                                        "Enabled alternative " + c + " is not annotated with @Alternative");
                            }
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(Class::getName)
                    .collect(Collectors.joining(",")));
        }
        if (profileInstance.disableApplicationLifecycleObservers()) {
            additional.put("quarkus.arc.test.disable-application-lifecycle-observers", "true");
        }
        if (profileInstance.getConfigProfile() != null) {
            additional.put(LaunchMode.TEST.getProfileKey(), profileInstance.getConfigProfile());
        }
        //we just use system properties for now
        //it's a lot simpler
        // TODO this is really ugly, set proper config on the app
        // TODO investigate whether we can use the config from https://github.com/quarkusio/quarkus/pull/42715 to avoid system properties
        // ... but be aware that this is called twice, and on the first pass through, the classloader might be an all-purpose runtime classloader, and would not be the actual test classloader
        // Setting config on the wrong classloader is worse than useless, so we'd need solid test coverage
        return RestorableSystemProperties.setProperties(additional)::close;
    }

    public static CuratedApplication makeCuratedApplication(Class<?> requiredTestClass, String displayName,
            boolean isContinuousTesting) throws IOException, AppModelResolverException, BootstrapException {
        final PathList.Builder rootBuilder = PathList.builder();
        final Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && !rootBuilder.contains(path) && Files.exists(path)) {
                rootBuilder.add(path);
            }
        };

        final Path testClassLocation;
        final Path appClassLocation;
        final Path projectRoot = Path.of("").normalize().toAbsolutePath();

        ApplicationModel testAppModel = null;
        final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
        // If gradle project running directly with IDE
        if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
            final WorkspaceModule module = gradleAppModel.getApplicationModule();
            final String testClassFileName = ClassLoaderHelper
                    .fromClassNameToResourceName(requiredTestClass.getName());
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
            validateTestDir(requiredTestClass, testClassesDir, module);
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

            testAppModel = BootstrapAppModelFactory.newInstance()
                    .setTest(true)
                    .setEnableClasspathCache(true)
                    .setProjectRoot(projectRoot)
                    .resolveAppModel()
                    .getApplicationModel();

            for (var rootDir : testAppModel.getAppArtifact().getContentTree().getRoots()) {
                addToBuilderIfConditionMet.accept(rootDir);
            }
        }

        CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                //.setExistingModel(gradleAppModel) unfortunately this model is not re-usable due to PathTree serialization by Gradle
                .setExistingModel(testAppModel)
                .setBaseName(displayName + " (QuarkusTest)")
                .setIsolateDeployment(true)
                .setMode(QuarkusBootstrap.Mode.TEST)
                .setTest(true)
                .setAuxiliaryApplication(isContinuousTesting)
                .setTargetDirectory(PathTestHelper.getProjectBuildDir(projectRoot, testClassLocation))
                .setProjectRoot(projectRoot)
                .setApplicationRoot(rootBuilder.build())
                .build()
                .bootstrap();

        if (!curatedApplication.getApplicationModel().getDependencies(DependencyFlags.RUNTIME_CP).iterator().hasNext()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus project. Please ensure that the test is configured to use the proper working directory.");

        }

        return curatedApplication;
    }

    // Note that curated application cannot be re-used between restarts, so this application
    // should have been freshly created
    // TODO maybe don't even accept one? is that comment right?
    public static StartupAction getStartupAction(Class<?> testClass, CuratedApplication curatedApplication, Class profile)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        PrepareResult prepareResult = prepare(testClass, curatedApplication, profile);

        // Before doing the augmentation, apply any extra config from the profile
        final Runnable configCleanup = prepareResult.profileInstance() == null ? null
                : setExtraPropertiesRestorably(profile, prepareResult.profileInstance());

        try {
            // To check changes here run integration-tests/elytron-resteasy-reactive and SharedProfileTestCase in integration-tests/main
            return prepareResult.augmentAction().createInitialRuntimeApplication();
        } catch (Exception e) {
            // Errors at this point just get reported as org.junit.platform.commons.JUnitException: TestEngine with ID 'junit-jupiter' failed to discover tests
            // Even though a stack trace isn't ideal handling, we want to make sure people have something to try and debug if problems happen
            e.printStackTrace();
            throw e;

        } finally {
            // We may by doing augmentations for other profiles now, so unset the config
            if (configCleanup != null) {
                configCleanup.run();
            }
        }
    }
}
