package io.quarkus.test.junit;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;
import static io.quarkus.test.common.PathTestHelper.validateTestDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.jboss.jandex.Index;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapConstants;
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
import io.quarkus.test.common.PathTestHelper;
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

    static PrepareResult prepare(
            Class<?> requiredTestClass,
            CuratedApplication curatedApplication,
            Optional<Class<?>> profileClass) throws Exception {

        Path testClassLocation = getTestClassesLocation(requiredTestClass, curatedApplication);

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
        Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        profileClass.map(Class::getName).ifPresent(name -> props.put(TEST_PROFILE, name));
        return new PrepareResult(curatedApplication.createAugmentor(TestBuildChainFunction.class.getName(), props),
                curatedApplication);
    }

    public static CuratedApplication makeCuratedApplication(Class<?> requiredTestClass, List<Path> additionalPaths,
            String displayName, boolean isContinuousTesting) throws Exception {
        final PathList.Builder rootBuilder = PathList.builder();
        final Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && Files.exists(path)) {
                path = path.normalize().toAbsolutePath();
                if (!rootBuilder.contains(path)) {
                    rootBuilder.add(path);
                }
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

        for (Path additionalPath : additionalPaths) {
            addToBuilderIfConditionMet.accept(additionalPath);
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
    public static StartupAction getStartupAction(Class<?> testClass, CuratedApplication curatedApplication,
            Optional<Class<?>> profileClass) throws Exception {
        PrepareResult prepareResult = prepare(testClass, curatedApplication, profileClass);

        try {
            // To check changes here run integration-tests/elytron-resteasy-reactive and SharedProfileTestCase in integration-tests/main
            return prepareResult.augmentAction().createInitialRuntimeApplication();
        } catch (Exception e) {
            // Errors at this point just get reported as org.junit.platform.commons.JUnitException: TestEngine with ID 'junit-jupiter' failed to discover tests
            // Even though a stack trace isn't ideal handling, we want to make sure people have something to try and debug if problems happen
            e.printStackTrace();
            throw e;
        }
    }
}
