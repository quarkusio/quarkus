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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.Index;

import io.quarkus.bootstrap.BootstrapAppModelFactory;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
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

    private static Class<?> quarkusTestMethodContextClass;
    private static boolean hasPerTestResources;

    private static List<Object> testMethodInvokers;
    private Runnable configCleanup;

    public static class PrepareResult {
        protected final AugmentAction augmentAction;
        public final QuarkusTestProfile profileInstance;
        protected final CuratedApplication curatedApplication;

        public PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
                CuratedApplication curatedApplication) {

            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
        }
    }

    public static ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException, AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    private PrepareResult createAugmentor(final Class<?> requiredTestClass, String displayName, boolean isContinuousTesting,
            CuratedApplication curatedApplication,
            Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws AppModelResolverException, BootstrapException, IOException,
            NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {

        if (curatedApplication == null) {
            curatedApplication = makeCuratedApplication(requiredTestClass, displayName, isContinuousTesting, shutdownTasks);
        }
        Path testClassLocation = getTestClassesLocation(requiredTestClass, curatedApplication);

        // clear the test.url system property as the value leaks into the run when using different profiles
        System.clearProperty("test.url");
        Map<String, String> additional = new HashMap<>();

        QuarkusTestProfile profileInstance = null;
        if (profile != null) {

            profileInstance = new ClassCoercingTestProfile(profile.getConstructor()
                    .newInstance());
            // TODO we make this twice, also in abstractjvmextension can we streamline that?
            // TODO We can't get rid of the one here because config needs to be set before augmentation, but maybe we can get rid of it on the test side?
            additional.putAll(profileInstance.getConfigOverrides());
            if (!profileInstance.getEnabledAlternatives()
                    .isEmpty()) {
                additional.put("quarkus.arc.selected-alternatives", profileInstance.getEnabledAlternatives()
                        .stream()
                        .peek((c) -> {
                            try {
                                // TODO is string comparison more efficient?
                                if (!c.isAnnotationPresent((Class<? extends Annotation>) profile.getClassLoader()
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
            // Sadly, I don't think #42715 helps, because it kicks in after this code
            configCleanup = RestorableSystemProperties.setProperties(additional)::close;
        }

        if (curatedApplication
                .getApplicationModel().getRuntimeDependencies().isEmpty()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus project. Please ensure that the test is configured to use the proper working directory.");
        }

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
                .createAugmentor(TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication);
    }

    public CuratedApplication makeCuratedApplication(Class<?> requiredTestClass, String displayName,
            boolean isContinuousTesting,
            Collection<Runnable> shutdownTasks) throws IOException, AppModelResolverException, BootstrapException {
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
        shutdownTasks.add(curatedApplication::close);

        // TODO can we consolidate some of this with TestSupport? The code over there is
        //        final QuarkusBootstrap.Builder bootstrapConfig = curatedApplication.getQuarkusBootstrap().clonedBuilder()
        //                                                                           .setMode(QuarkusBootstrap.Mode.TEST)
        //                                                                           .setAssertionsEnabled(true)
        //                                                                           .setDisableClasspathCache(false)
        //                                                                           .setIsolateDeployment(true)
        //                                                                           .setExistingModel(null)
        //                                                                           .setBaseClassLoader(getClass().getClassLoader().getParent())
        //                                                                           .setTest(true)
        //                                                                           .setAuxiliaryApplication(true)
        //                                                                           .setHostApplicationIsTestOnly(devModeType == DevModeType.TEST_ONLY)
        //                                                                           .setProjectRoot(projectDir)
        //                                                                           .setApplicationRoot(getRootPaths(module, mainModule))
        //                                                                           .clearLocalArtifacts();

        return curatedApplication;
    }

    // Note that curated application cannot be re-used between restarts, so this application
    // should have been freshly created
    // TODO maybe don't even accept one? is that comment right?
    public StartupAction getStartupAction(Class testClass, CuratedApplication curatedApplication,
            boolean isContinuousTesting, Class profile) throws AppModelResolverException, BootstrapException, IOException,
            InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Collection<Runnable> shutdownTasks = new HashSet();
        PrepareResult result = createAugmentor(testClass, "(QuarkusTest)", isContinuousTesting, curatedApplication, profile,
                shutdownTasks);
        AugmentAction augmentAction = result.augmentAction;

        try {
            StartupAction startupAction = augmentAction.createInitialRuntimeApplication();

            // To check changes here run integration-tests/elytron-resteasy-reactive and SharedProfileTestCase in integration-tests/main

            return startupAction;
        } catch (Throwable e) {
            // Errors at this point just get reported as org.junit.platform.commons.JUnitException: TestEngine with ID 'junit-jupiter' failed to discover tests
            // Give a little help to debuggers
            // TODO how best to handle?
            e.printStackTrace();
            throw e;

        } finally {
            if (configCleanup != null) {
                configCleanup.run();
            }
        }

    }

}
