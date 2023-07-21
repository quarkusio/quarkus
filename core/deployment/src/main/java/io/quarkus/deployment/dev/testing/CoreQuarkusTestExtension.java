package io.quarkus.deployment.dev.testing;

import static io.quarkus.deployment.dev.testing.PathTestHelper.getAppClassLocationForTestLocation;
import static io.quarkus.deployment.dev.testing.PathTestHelper.getTestClassesLocation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Alternative;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Index;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.classloading.ClassPathElement;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.runner.Timing;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestClassBeanBuildItem;
import io.quarkus.deployment.builditem.TestClassPredicateBuildItem;
import io.quarkus.paths.PathList;
import io.quarkus.runtime.configuration.ProfileManager;
import io.quarkus.runtime.test.TestHttpEndpointProvider;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;

public class CoreQuarkusTestExtension {

    // Copied from superclass of thing we copied
    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";

    protected static Class currentJUnitTestClass;

    /// end copied

    private static final Logger log = Logger.getLogger(CoreQuarkusTestExtension.class);

    private static boolean failedBoot;

    private static Class<?> actualTestClass;
    private static Object actualTestInstance;
    // needed for @Nested
    private static Deque<Object> outerInstances = new ArrayDeque<>(1);
    private static Pattern clonePattern;
    private static Throwable firstException; //if this is set then it will be thrown from the
    // very first test that is run, the rest are aborted

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
            this.augmentAction = augmentAction;
            this.profileInstance = profileInstance;
            this.curatedApplication = curatedApplication;
            this.testClassLocation = testClassLocation;
        }
    }

    // Re-used from AbstractJvmQuarkusTestExtension
    protected PrepareResult createAugmentor(Class requiredTestClass, Class<? extends QuarkusTestProfile> profile,
            Collection<Runnable> shutdownTasks) throws Exception {

        // I think the required test class is just an example, since the augmentor is only
        // created once per test profile
        final PathList.Builder rootBuilder = PathList.builder();
        Consumer<Path> addToBuilderIfConditionMet = path -> {
            if (path != null && Files.exists(path) && !rootBuilder.contains(path)) {
                System.out.println("HOLLY adding path to builder " + path);
                rootBuilder.add(path);
            }
        };

        currentJUnitTestClass = requiredTestClass;

        final Path testClassLocation;
        final Path appClassLocation;
        final Path projectRoot = Paths.get("")
                .normalize()
                .toAbsolutePath();

        //        final ApplicationModel gradleAppModel = getGradleAppModelForIDE(projectRoot);
        // If gradle project running directly with IDE
        //        if (gradleAppModel != null && gradleAppModel.getApplicationModule() != null) {
        //            final WorkspaceModule module = gradleAppModel.getApplicationModule();
        //            final String testClassFileName = requiredTestClass.getName().replace('.',
        //            '/') + ".class";
        //            Path testClassesDir = null;
        //            for (String classifier : module.getSourceClassifiers()) {
        //                final ArtifactSources sources = module.getSources(classifier);
        //                if (sources.isOutputAvailable() && sources.getOutputTree().contains
        //                (testClassFileName)) {
        //                    for (SourceDir src : sources.getSourceDirs()) {
        //                        addToBuilderIfConditionMet.accept(src.getOutputDir());
        //                        if (Files.exists(src.getOutputDir().resolve(testClassFileName))) {
        //                            testClassesDir = src.getOutputDir();
        //                        }
        //                    }
        //                    for (SourceDir src : sources.getResourceDirs()) {
        //                        addToBuilderIfConditionMet.accept(src.getOutputDir());
        //                    }
        //                    for (SourceDir src : module.getMainSources().getSourceDirs()) {
        //                        addToBuilderIfConditionMet.accept(src.getOutputDir());
        //                    }
        //                    for (SourceDir src : module.getMainSources().getResourceDirs()) {
        //                        addToBuilderIfConditionMet.accept(src.getOutputDir());
        //                    }
        //                    break;
        //                }
        //            }
        //            if (testClassesDir == null) {
        //                final StringBuilder sb = new StringBuilder();
        //                sb.append("Failed to locate ").append(requiredTestClass.getName())
        //                .append(" in ");
        //                for (String classifier : module.getSourceClassifiers()) {
        //                    final ArtifactSources sources = module.getSources(classifier);
        //                    if (sources.isOutputAvailable()) {
        //                        for (SourceDir d : sources.getSourceDirs()) {
        //                            if (Files.exists(d.getOutputDir())) {
        //                                sb.append(System.lineSeparator()).append(d.getOutputDir
        //                                ());
        //                            }
        //                        }
        //                    }
        //                }
        //                throw new RuntimeException(sb.toString());
        //            }
        //            testClassLocation = testClassesDir;
        //
        //        } else {
        if (System.getProperty(BootstrapConstants.OUTPUT_SOURCES_DIR) != null) {
            final String[] sourceDirectories = System.getProperty(
                    BootstrapConstants.OUTPUT_SOURCES_DIR)
                    .split(",");
            for (String sourceDirectory : sourceDirectories) {
                final Path directory = Paths.get(sourceDirectory);
                addToBuilderIfConditionMet.accept(directory);
            }
        }

        testClassLocation = getTestClassesLocation(requiredTestClass);
        System.out.println("test class location is " + testClassLocation);
        appClassLocation = getAppClassLocationForTestLocation(testClassLocation.toString());
        System.out.println("app class location is " + appClassLocation);
        if (!appClassLocation.equals(testClassLocation)) {
            addToBuilderIfConditionMet.accept(testClassLocation);
            // if test classes is a dir, we should also check whether test resources dir exists
            // as a separate dir (gradle)
            // TODO: this whole app/test path resolution logic is pretty dumb, it needs be
            //  re-worked using proper workspace discovery
            final Path testResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(
                    testClassLocation, "test");
            addToBuilderIfConditionMet.accept(testResourcesLocation);
        }

        addToBuilderIfConditionMet.accept(appClassLocation);
        final Path appResourcesLocation = PathTestHelper.getResourcesForClassesDirOrNull(
                appClassLocation, "main");

        addToBuilderIfConditionMet.accept(appResourcesLocation);
        //  }

        ClassLoader originalCl = Thread.currentThread()
                .getContextClassLoader();

        // clear the test.url system property as the value leaks into the run when using
        // different profiles
        System.clearProperty("test.url");
        Map<String, String> additional = new HashMap<>();

        QuarkusTestProfile profileInstance = null;
        if (profile != null) {
            profileInstance = profile.getConstructor()
                    .newInstance();
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
                additional.put(ProfileManager.QUARKUS_TEST_PROFILE_PROP,
                        profileInstance.getConfigProfile());
            }
            //we just use system properties for now
            //it's a lot simpler
            // TODO do we need this?  shutdownTasks.add(RestorableSystemProperties.setProperties(additional)::close);
        }

        CuratedApplication curatedApplication;
        if (CurrentTestApplication.curatedApplication != null) {
            curatedApplication = CurrentTestApplication.curatedApplication;
            System.out.println("HOLLY read it from CurrentTestApp" + curatedApplication);
        } else {
            curatedApplication = QuarkusBootstrap.builder()
                    //.setExistingModel(gradleAppModel)
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
                    .build()
                    .bootstrap();
            shutdownTasks.add(curatedApplication::close);
        }

        if (curatedApplication.getApplicationModel()
                .getRuntimeDependencies()
                .isEmpty()) {
            throw new RuntimeException(
                    "The tests were run against a directory that does not contain a Quarkus " +
                            "project. Please ensure that the test is configured to use the proper" +
                            " working directory.");
        }

        //TODO is this needed?
        //        Index testClassesIndex = TestClassIndexer.indexTestClasses(testClassLocation);
        //        // we need to write the Index to make it reusable from other parts of the testing
        //        // infrastructure that run in different ClassLoaders
        //        TestClassIndexer.writeIndex(testClassesIndex, testClassLocation, requiredTestClass);

        Timing.staticInitStarted(curatedApplication.getBaseRuntimeClassLoader(),
                curatedApplication.getQuarkusBootstrap()
                        .isAuxiliaryApplication());
        final Map<String, Object> props = new HashMap<>();
        props.put(TEST_LOCATION, testClassLocation);
        props.put(TEST_CLASS, requiredTestClass);
        // TODO what's going on here with the profile?
        Class<? extends QuarkusTestProfile> quarkusTestProfile = profile;
        PrepareResult result = new PrepareResult(curatedApplication
                .createAugmentor(CoreQuarkusTestExtension.TestBuildChainFunction.class.getName(),
                        props),
                profileInstance,
                curatedApplication, testClassLocation);
        // TODO this could be cleaner
        StartupAction.storeTestClassLocation(result.testClassLocation);
        return result;
    }

    public ClassLoader doJavaStart(Class testClass, CuratedApplication curatedApplication) throws Exception {
        Class<? extends QuarkusTestProfile> profile = null;
        // TODO do we want any of these?
        Collection shutdownTasks = new HashSet();
        PrepareResult result = createAugmentor(testClass, profile, shutdownTasks);
        AugmentAction augmentAction = result.augmentAction;
        QuarkusTestProfile profileInstance = result.profileInstance;

        testHttpEndpointProviders = TestHttpEndpointProvider.load();
        System.out.println("CORE MAKER SEES CLASS OF STARTUP " + StartupAction.class.getClassLoader());

        System.out.println("HOLLY about to make app for " + testClass);
        StartupAction startupAction = augmentAction.createInitialRuntimeApplication();
        // TODO this seems to be safe to do because the classloaders are the same
        startupAction.store();
        System.out.println("HOLLY did store " + startupAction);
        return startupAction.getClassLoader();

    }

    // TODO can we defer this and move it back to the junit5 module?
    public static class TestBuildChainFunction implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(Map<String, Object> stringObjectMap) {
            Path testLocation = (Path) stringObjectMap.get(TEST_LOCATION);
            // the index was written by the extension
            Index testClassesIndex = TestClassIndexer.readIndex(testLocation, (Class<?>) stringObjectMap.get(TEST_CLASS));

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

            // TODO will this work if we copied the class?
            // give other extensions the ability to customize the build chain
            for (TestBuildChainCustomizerProducer testBuildChainCustomizerProducer : ServiceLoader
                    .load(TestBuildChainCustomizerProducer.class, this.getClass().getClassLoader())) {
                allCustomizers.add(testBuildChainCustomizerProducer.produce(testClassesIndex));
            }

            return allCustomizers;
        }
    }

}
