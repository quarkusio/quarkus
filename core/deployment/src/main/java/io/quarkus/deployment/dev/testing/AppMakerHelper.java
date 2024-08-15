package io.quarkus.deployment.dev.testing;

import static io.quarkus.test.common.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.test.common.PathTestHelper.getTestClassesLocation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.BootstrapException;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.bootstrap.workspace.ArtifactSources;
import io.quarkus.bootstrap.workspace.SourceDir;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.commons.classloading.ClassloadHelper;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.common.PathTestHelper;
import io.quarkus.test.common.RestorableSystemProperties;
import io.quarkus.test.junit.QuarkusTestProfile;

public class AppMakerHelper {

    // Copied from superclass of thing we copied
    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";
    protected static final String TEST_PROFILE = "test-profile";
    /// end copied

    private static boolean failedBoot;

    private static Class<?> quarkusTestMethodContextClass;
    private static boolean hasPerTestResources;
    private static List<Function<Class<?>, String>> testHttpEndpointProviders;

    private static List<Object> testMethodInvokers;

    protected static class PrepareResult {
        protected final AugmentAction augmentAction;
        protected final QuarkusTestProfile profileInstance;
        protected final CuratedApplication curatedApplication;
        protected final Path testClassLocation;

        public PrepareResult(AugmentAction augmentAction, QuarkusTestProfile profileInstance,
                CuratedApplication curatedApplication, Path testClassLocation) {
            System.out.println("PrepareResult" + augmentAction + ": " + profileInstance + " test class " + testClassLocation);

            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
            this.testClassLocation = testClassLocation;
        }
    }

    // TODO is this used?
    protected PrepareResult v2createAugmentor(CuratedApplication curatedApplication, Class requiredTestClass,
            Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks, boolean isContinuousTesting) throws Exception {

        Path testClassLocation = getTestClassesLocation(requiredTestClass);
        // TODO this is probably wrong since we find the gradle path in the method but the other path elsewhere
        return v2createAugmentor(curatedApplication, PathList.of(testClassLocation), requiredTestClass, profile, shutdownTasks,
                isContinuousTesting);
    }

    // TODO Re-used from AbstractJvmQuarkusTestExtension
    protected ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException, AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    // TODO Re-used from AbstractJvmQuarkusTestExtension, delete it there
    private PrepareResult createAugmentor(ExtensionContext context, CuratedApplication curatedApplication,
            Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {
        return createAugmentor(context.getRequiredTestClass(), context.getDisplayName(), curatedApplication, profile,
                shutdownTasks);
    }

    private PrepareResult createAugmentor(final Class<?> requiredTestClass, String displayName,
            CuratedApplication curatedApplication,
            Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        if (curatedApplication == null) {
            curatedApplication = makeCuratedApplication(requiredTestClass, displayName, shutdownTasks);
        }
        Path testClassLocation = getTestClassLocationIncludingPossibilityOfGradleModel(requiredTestClass);

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
                        .getQuarkusBootstrap().isAuxiliaryApplication());
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        if (profile != null) {
            props.put(TEST_PROFILE, profile.getName());
        }
        //TODO copied, not needed - but do need to pass it out quarkusTestProfile = profile;
        return new PrepareResult(curatedApplication
                .createAugmentor(TestBuildChainFunction.class.getName(), props), profileInstance,
                curatedApplication, testClassLocation);
    }

    CuratedApplication makeCuratedApplication(Class<?> requiredTestClass, String displayName,
            Collection<Runnable> shutdownTasks) throws IOException, AppModelResolverException, BootstrapException {
        final PathList.Builder rootBuilder = PathList.builder();
        Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && Files.exists(path) && !rootBuilder.contains(path)) {
                rootBuilder.add(path);
            }
        };

        final Path testClassLocation;
        final Path appClassLocation;
        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();

        final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
        // If gradle project running directly with IDE
        if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
            final WorkspaceModule module = gradleAppModel.getApplicationModule();
            final String testClassFileName = ClassloadHelper
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
            final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(appClassLocation, "main");
            addToBuilderIfConditionMet.accept(appResourcesLocation);
        }

        CuratedApplication curatedApplication = QuarkusBootstrap.builder()
                //.setExistingModel(gradleAppModel) unfortunately this model is not re-usable due to PathTree serialization by Gradle
                .setBaseName(displayName + " (QuarkusTest)")
                .setIsolateDeployment(true)
                .setMode(QuarkusBootstrap.Mode.TEST)
                .setTest(true)
                .setTargetDirectory(PathTestHelper.getProjectBuildDir(projectRoot, testClassLocation))
                .setProjectRoot(projectRoot)
                .setApplicationRoot(rootBuilder.build())
                .build()
                .bootstrap();
        shutdownTasks.add(curatedApplication::close);

        return curatedApplication;
    }

    private Path getTestClassLocationIncludingPossibilityOfGradleModel(Class<?> requiredTestClass)
            throws IOException, AppModelResolverException, BootstrapException {

        final Path projectRoot = Paths.get("").normalize().toAbsolutePath();

        final Path testClassLocation;

        final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
        // If gradle project running directly with IDE
        if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
            final WorkspaceModule module = gradleAppModel.getApplicationModule();
            final String testClassFileName = ClassloadHelper
                    .fromClassNameToResourceName(requiredTestClass.getName());
            Path testClassesDir = null;
            for (String classifier : module.getSourceClassifiers()) {
                final ArtifactSources sources = module.getSources(classifier);
                if (sources.isOutputAvailable() && sources.getOutputTree().contains(testClassFileName)) {
                    for (SourceDir src : sources.getSourceDirs()) {
                        if (Files.exists(src.getOutputDir().resolve(testClassFileName))) {
                            testClassesDir = src.getOutputDir();
                        }
                    }

                    break;
                }
            }
            validateTestDir(requiredTestClass, testClassesDir, module);
            testClassLocation = testClassesDir;

        } else {
            testClassLocation = getTestClassesLocation(requiredTestClass);
        }

        return testClassLocation;
    }

    private static void validateTestDir(Class<?> requiredTestClass, Path testClassesDir, WorkspaceModule module) {
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
    }

    protected PrepareResult v2createAugmentor(CuratedApplication curatedApplication, PathList bothLocations,
            Class<?> requiredTestClass,
            Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks, boolean isContinuousTesting) throws Exception {
        System.out.println("HOLLY creating augmentor with prarams" + bothLocations + " " + requiredTestClass + " and app"
                + curatedApplication);
        Path testClassLocation = bothLocations.stream().findFirst().get();

        // I think the required test class is just an example, since the augmentor is only
        // created once per test profile

        QuarkusTestProfile profileInstance = null;
        if (profile != null) {
            profileInstance = profile.getConstructor()
                    .newInstance();
        }

        if (curatedApplication == null) {

            // TODO this is only needed if curatedApplication is null? Extract common code with the interceptor
            // TODO we bypass all this in the interceptor, terrible!!
            final PathList.Builder rootBuilder = PathList.builder();
            Consumer<Path> addToBuilderIfConditionMet = path -> {
                if (path != null && Files.exists(path) && !rootBuilder.contains(path)) {
                    System.out.println("HOLLY adding path to builder " + path);
                    rootBuilder.add(path);
                }
            };

            final Path appClassLocation;
            Path appClassLocation1;
            final Path projectRoot = Paths.get("")
                    .normalize()
                    .toAbsolutePath();

            final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
            //If gradle project running directly with IDE
            if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
                System.out.println("HOLLY going down IDE gradle path");
                final WorkspaceModule module = gradleAppModel.getApplicationModule();
                final String testClassFileName = requiredTestClass.getName().replace('.',
                        '/') + ".class";
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
                System.out.println("HOLLY going down not-gradle path " +
                        System.getProperty(BootstrapConstants.ALL_OUTPUT_SOURCES_DIR) + " " +
                        System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR));
                // TODO sort out duplication
                if (System.getProperty(BootstrapConstants.ALL_OUTPUT_SOURCES_DIR) != null) {
                    System.out
                            .println("HOLLY ALLS processing " + System.getProperty(BootstrapConstants.ALL_OUTPUT_SOURCES_DIR));
                    final String[] sourceDirectories = System.getProperty(
                            BootstrapConstants.ALL_OUTPUT_SOURCES_DIR)
                            .split(",");
                    for (String sourceDirectory : sourceDirectories) {
                        final Path directory = Paths.get(sourceDirectory);
                        addToBuilderIfConditionMet.accept(directory);
                    }
                } else if (System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR) != null) {
                    System.out.println("HOLLY processing " + System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR));
                    final String[] sourceDirectories = System.getProperty(
                            BootstrapConstants.OUTPUT_SOURCES_DIR)
                            .split(",");
                    for (String sourceDirectory : sourceDirectories) {
                        final Path directory = Paths.get(sourceDirectory);
                        addToBuilderIfConditionMet.accept(directory);
                    }
                }

            }

            // testClassLocation = getTestClassesLocation(requiredTestClass);
            System.out.println("test class location is " + testClassLocation);

            appClassLocation = getAppClassLocationForTestLocation(testClassLocation);

            System.out.println("app class location is " + appClassLocation);
            if (!appClassLocation.equals(testClassLocation)) {
                System.out.println("Adding test class location explicitly");
                addToBuilderIfConditionMet.accept(testClassLocation);
                // if test classes is a dir, we should also check whether test resources dir exists
                // as a separate dir (gradle)
                // TODO: this whole app/test path resolution logic is pretty dumb, it needs be
                //  re-worked using proper workspace discovery
                final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(
                        testClassLocation, "test");
                addToBuilderIfConditionMet.accept(testResourcesLocation);
            }

            System.out.println("Adding app class location explicitly");
            addToBuilderIfConditionMet.accept(appClassLocation);
            final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(
                    appClassLocation, "main");

            addToBuilderIfConditionMet.accept(appResourcesLocation);

            ClassLoader originalCl = Thread.currentThread()
                    .getContextClassLoader();

            // clear the test.url system property as the value leaks into the run when using
            // different profiles
            System.clearProperty("test.url");
            Map<String, String> additional = new HashMap<>();

            if (profile != null) {

                additional.putAll(profileInstance.getConfigOverrides());
                if (!profileInstance.getEnabledAlternatives()
                        .isEmpty()) {
                    additional.put("quarkus.arc.selected-alternatives",
                            profileInstance.getEnabledAlternatives()
                                    .stream()
                                    .peek((c) -> {
                                        if (!c.isAnnotationPresent(Alternative.class)) {
                                            throw new RuntimeException(
                                                    "Enabled alternative " + c + " is not " +
                                                            "annotated with @Alternative");
                                        }
                                    })
                                    .map(Class::getName)
                                    .collect(Collectors.joining(",")));
                }
                if (profileInstance.disableApplicationLifecycleObservers()) {
                    additional.put("quarkus.arc.test.disable-application-lifecycle-observers", "true");
                }
                if (profileInstance.getConfigProfile() != null) {
                    additional.put(LaunchMode.TEST.getProfileKey(),
                            profileInstance.getConfigProfile());
                }
                //we just use system properties for now
                //it's a lot simpler
                // TODO do we need this?  shutdownTasks.add(RestorableSystemProperties.setProperties(additional)::close);
            }

            curatedApplication = QuarkusBootstrap.builder()
                    //.setExistingModel(gradleAppModel) TODO is this needed?
                    // unfortunately this model is not re-usable due
                    // to PathTree serialization by Gradle
                    .setIsolateDeployment(true)
                    .setMode(QuarkusBootstrap.Mode.TEST)
                    .setTest(true)
                    .setTargetDirectory(
                            PathTestHelper.getProjectBuildDir(
                                    projectRoot, testClassLocation))
                    .setProjectRoot(projectRoot)
                    .setApplicationRoot(rootBuilder.build())
                    .setAuxiliaryApplication(isContinuousTesting) // TODO should be conditional on the launch mode? do not set to true for normal holly addition are we sure this is safe to do here? it's not done in what we copied from? will this work with mvn verify? this guards instrumenting the classes with tracing to decide what to hot reload

                    .build()
                    .bootstrap();
            shutdownTasks.add(curatedApplication::close);
        }

        System.out.println("HOLLY made it to the other side of curated application creation ");
        System.out.println("HOLLY app model is  " + curatedApplication.getApplicationModel());

        if (curatedApplication.getApplicationModel()
                .getRuntimeDependencies()
                .isEmpty()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus " +
                            "project. Please ensure that the test is configured to use the proper" +
                            " working directory.");
        }

        //TODO is this needed? Or could we take advantage of this more?
        //        Index testClassesIndex = TestClassIndexer.indexTestClasses(testClassLocation);
        //        // we need to write the Index to make it reusable from other parts of the testing
        //        // infrastructure that run in different ClassLoaders
        //        TestClassIndexer.writeIndex(testClassesIndex, testClassLocation, requiredTestClass);

        // TODO get used to be ok, now it needs to be get or create, what *isn't* happening earlier on the lifecycle?
        Timing.staticInitStarted(curatedApplication.getOrCreateBaseRuntimeClassLoader(),
                curatedApplication.getQuarkusBootstrap()
                        .isAuxiliaryApplication());
        System.out.println("HOLLY did timing init started");
        System.out.println("HOLLY test class location is " + testClassLocation);
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        // TODO surely someone reads this? props.put(TEST_CLASS, requiredTestClass);
        // TODO what's going on here with the profile?
        Class<? extends QuarkusTestProfile> quarkusTestProfile = profile;
        PrepareResult result = new PrepareResult(curatedApplication
                .createAugmentor(TestBuildChainFunction.class.getName(),
                        props),
                profileInstance,
                curatedApplication, testClassLocation);
        // TODO this could be cleaner
        // TODO do we need this? StartupAction.storeTestClassLocation(result.testClassLocation);
        return result;
    }

    // TODO surely there's a cleaner way to see if it's continuous testing?
    // TODO should we be doing something with these unused arguments?
    // Note that curated application cannot be re-used between restarts, so this application
    // should have been freshly created
    // TODO maybe don't even accept one?
    public DumbHolder getStartupAction(Class testClass, CuratedApplication curatedApplication,
            boolean isContinuousTesting, Class ignoredProfile)
            throws Exception {

        Class<? extends QuarkusTestProfile> profile = ignoredProfile;
        // TODO do we want any of these?
        Collection shutdownTasks = new HashSet();
        // TODO work out a good display name
        PrepareResult result = createAugmentor(testClass, "(QuarkusTest)", curatedApplication, profile, shutdownTasks);
        AugmentAction augmentAction = result.augmentAction;
        QuarkusTestProfile profileInstance = result.profileInstance;

        testHttpEndpointProviders = TestHttpEndpointProvider.load();

        try {
            System.out.println("HOLLY about to make app for " + testClass);
            StartupAction startupAction = augmentAction.createInitialRuntimeApplication();

            // TODO this seems to be safe to do because the classloaders are the same
            // TODO not doing it startupAction.store();
            System.out.println("HOLLY did store " + startupAction);
            return new DumbHolder(startupAction, result);
        } catch (RuntimeException e) {
            // Errors at this point just get reported as org.junit.platform.commons.JUnitException: TestEngine with ID 'junit-jupiter' failed to discover tests
            // Give a little help to debuggers
            System.out.println("HOLLY IT ALL WENT WRONG + + e" + e);
            e.printStackTrace();
            throw e;

        }

    }

    record DumbHolder(StartupAction startupAction, PrepareResult prepareResult) {
    }

    //    public QuarkusClassLoader doJavaStart(PathList location, CuratedApplication curatedApplication, boolean isContinuousTesting)
    //            throws Exception {
    //        Class<? extends QuarkusTestProfile> profile = null;
    //        // TODO do we want any of these?
    //        Collection shutdownTasks = new HashSet();
    //        // TODO clearly passing null is not really ideal
    //        PrepareResult result = createAugmentor(curatedApplication, location, null, profile, shutdownTasks,
    //                isContinuousTesting);
    //        AugmentAction augmentAction = result.augmentAction;
    //        QuarkusTestProfile profileInstance = result.profileInstance;
    //
    //        testHttpEndpointProviders = TestHttpEndpointProvider.load();
    //        System.out.println(
    //                "CORE MAKER SEES CLASS OF STARTUP " + StartupAction.class.getClassLoader());
    //
    //        System.out.println("HOLLY about to make app for " + location);
    //        StartupAction startupAction = augmentAction.createInitialRuntimeApplication();
    //        // TODO this seems to be safe to do because the classloaders are the same
    //        // TODO not doing it startupAction.store();
    //        System.out.println("HOLLY did store " + startupAction);
    //        return (QuarkusClassLoader) startupAction.getClassLoader();
    //
    //    }

    // TODO can we defer this and move it back to the junit5 module?
    public static class TestBuildChainFunction implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> stringObjectMap) {
            System.out.println("HOLLY in apply");
            Path testLocation = (Path) stringObjectMap.get(TEST_LOCATION);
            System.out.println("HOLLY path is " + testLocation);
            // the index was written by the extension
            Class<?> testClass = (Class<?>) stringObjectMap.get(TEST_CLASS);
            // TODO is this at all safe?

            Index testClassesIndex;
            if (testClass != null) {
                testClassesIndex = TestClassIndexer.readIndex(testLocation, testClass);
            } else {
                testClassesIndex = TestClassIndexer.readIndex(testLocation);
            }
            List<Consumer<BuildChainBuilder>> allCustomizers = new ArrayList<>(1);
            Consumer<BuildChainBuilder> defaultCustomizer = new Consumer<BuildChainBuilder>() {

                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new TestClassPredicateBuildItem(new Predicate<String>() {
                                @Override
                                public boolean test(String className) {
                                    return PathTestHelper.isTestClass(className,
                                            Thread.currentThread().getContextClassLoader(), testLocation);
                                }
                            }));
                        }
                    }).produces(TestClassPredicateBuildItem.class)
                            .build();
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            //we need to make sure all hot reloadable classes are application classes
                            context.produce(new ApplicationClassPredicateBuildItem(new Predicate<String>() {
                                @Override
                                public boolean test(String s) {
                                    QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread()
                                            .getContextClassLoader();
                                    //if the class file is present in this (and not the parent) CL then it is an application class
                                    List<ClassPathElement> res = cl
                                            .getElementsWithResource(s.replace(".", "/") + ".class", true);
                                    return !res.isEmpty();
                                }
                            }));
                        }
                    }).produces(ApplicationClassPredicateBuildItem.class).build();
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            // TODO leaking of knowledge from junit5 to core
                            context.produce(new TestAnnotationBuildItem("io.quarkus.test.junit.QuarkusTest"));
                        }
                    }).produces(TestAnnotationBuildItem.class)
                            .build();

                    List<String> testClassBeans = new ArrayList<>();

                    List<AnnotationInstance> extendWith = testClassesIndex
                            .getAnnotations(DotNames.EXTEND_WITH);
                    for (AnnotationInstance annotationInstance : extendWith) {
                        if (annotationInstance.target().kind() != AnnotationTarget.Kind.CLASS) {
                            continue;
                        }
                        ClassInfo classInfo = annotationInstance.target().asClass();
                        if (classInfo.isAnnotation()) {
                            continue;
                        }
                        Type[] extendsWithTypes = annotationInstance.value().asClassArray();
                        for (Type type : extendsWithTypes) {
                            if (DotNames.QUARKUS_TEST_EXTENSION.equals(type.name())) {
                                testClassBeans.add(classInfo.name().toString());
                            }
                        }
                    }

                    List<AnnotationInstance> registerExtension = testClassesIndex.getAnnotations(DotNames.REGISTER_EXTENSION);
                    for (AnnotationInstance annotationInstance : registerExtension) {
                        if (annotationInstance.target().kind() != AnnotationTarget.Kind.FIELD) {
                            continue;
                        }
                        FieldInfo fieldInfo = annotationInstance.target().asField();
                        if (DotNames.QUARKUS_TEST_EXTENSION.equals(fieldInfo.type().name())) {
                            testClassBeans.add(fieldInfo.declaringClass().name().toString());
                        }
                    }

                    if (!testClassBeans.isEmpty()) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                for (String quarkusExtendWithTestClass : testClassBeans) {
                                    context.produce(new TestClassBeanBuildItem(quarkusExtendWithTestClass));
                                }
                            }
                        }).produces(TestClassBeanBuildItem.class)
                                .build();
                    }

                }
            };
            allCustomizers.add(defaultCustomizer);

            // TODO disabled, to avoid dependency issues
            // give other extensions the ability to customize the build chain
            //            for (TestBuildChainCustomizerProducer testBuildChainCustomizerProducer : ServiceLoader
            //                    .load(TestBuildChainCustomizerProducer.class, this.getClass().getClassLoader())) {
            //                allCustomizers.add(testBuildChainCustomizerProducer.produce(testClassesIndex));
            //            }

            System.out.println("HOLLY done apply");
            return allCustomizers;
        }
    }

}
