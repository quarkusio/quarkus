package io.quarkus.test.junit.launcher;

import static io.quarkus.deployment.dev.testing.PathTestHelper.getTestClassLocationForRootLocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.engine.config.CachingJupiterConfiguration;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.discovery.DiscoverySelectorResolver;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.hierarchical.EngineExecutionContext;
import org.junit.platform.engine.support.hierarchical.HierarchicalTestEngine;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.deployment.dev.testing.CoreQuarkusTestExtension;
import io.quarkus.deployment.dev.testing.CurrentTestApplication;
import io.quarkus.runtime.LaunchMode;

/**
 * The docs say 'HierarchicalTestEngine is a convenient abstract base implementation of the TestEngine SPI (used by the
 * junit-jupiter-engine) that only requires implementors to provide the logic for test discovery. It implements execution of
 * TestDescriptors that implement the Node interface, including support for parallel execution.'
 */
public class QuarkusTestEngine extends HierarchicalTestEngine<EngineExecutionContext> {
    public static final String UNIQUE_ID = "quarkus-jupiter";

    public QuarkusTestEngine() {
    }

    // TODO this is all copy-pasted from the jupiter test engine

    public String getId() {
        return UNIQUE_ID;
    }

    //    public Optional<String> getGroupId() {
    //        return Optional.of("org.junit.jupiter");
    //    }
    //
    //    public Optional<String> getArtifactId() {
    //        return Optional.of("junit-jupiter-engine");
    //    }

    class QCTestDescriptor extends ClassTestDescriptor {

        public QCTestDescriptor(UniqueId uniqueId, Class clazz, JupiterConfiguration configuration) {
            super(uniqueId, clazz, configuration);
        }

    }

    // TODO guard and only do things if they have the tag
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
        JupiterConfiguration configuration = new CachingJupiterConfiguration(
                new DefaultJupiterConfiguration(discoveryRequest.getConfigurationParameters()));
        JupiterEngineDescriptor engineDescriptor = new JupiterEngineDescriptor(uniqueId, configuration);

        Set<Class> quarkusTestClasses = Set
                .of(reCreateClass("io.quarkus.test.junit.util.QuarkusTestProfileAwareClassOrdererTest"));

        EngineDiscoveryRequestBuilder.request()
                .selectors(quarkusTestClasses.stream().map(
                        DiscoverySelectors::selectClass)
                        .collect(Collectors.toList()));

        (new DiscoverySelectorResolver()).resolveSelectors(discoveryRequest, engineDescriptor);

        // TODO Make a tree root for each curatedapplication/classloader
    }

    private Class reCreateClass(Class<?> testClass) throws Exception {
        return reCreateClass(testClass.getName());
    }

    private Class reCreateClass(String className) throws Exception {

        CoreQuarkusTestExtension coreQuarkusTestExtension = new CoreQuarkusTestExtension();

        Path projectRoot = Paths.get("")
                .normalize()
                .toAbsolutePath();

        // Why do we do this rather than just using the project root?
        // BootstrapConstants.OUTPUT_SOURCES_DIR does not have gradle additional source
        // sets, but the classpath does
        //  Path applicationRoot = getTestClassesLocationWithNoContext();
        Path applicationRoot = getTestClassLocationForRootLocation(projectRoot.toString());

        // TODO we need less of this dance of working out a root location because we have a class, exactly like what we started with, arg

        CuratedApplication curatedApplication;

        System.out.println("ENGINE MAKING Curated application with root " + applicationRoot);

        curatedApplication = QuarkusBootstrap.builder()
                //.setExistingModel(gradleAppModel)
                // unfortunately this model is not
                // re-usable
                // due
                // to PathTree serialization by Gradle
                .setIsolateDeployment(true)
                .setMode(
                        QuarkusBootstrap.Mode.TEST) //
                // Even in continuous testing, we set
                // the mode to test - here, if we go
                // down this path we know it's normal mode
                // is this always right?
                .setTest(true)
                .setApplicationRoot(applicationRoot)

                //                    .setTargetDirectory(
                //                            PathTestHelper
                //                            .getProjectBuildDir(
                //                                    projectRoot, testClassLocation))
                .setProjectRoot(projectRoot)
                //                        .setApplicationRoot(rootBuilder.build())
                .build()
                .bootstrap();

        var appModelFactory = curatedApplication.getQuarkusBootstrap()
                .newAppModelFactory();
        appModelFactory.setBootstrapAppModelResolver(null);
        appModelFactory.setTest(true);
        appModelFactory.setLocalArtifacts(Set.of());
        // TODO    if (!mainModule) {
        //      appModelFactory.setAppArtifact(null);
        appModelFactory.setProjectRoot(projectRoot);
        //   }

        // To do this deserialization, we need to have an app root, so we can't use
        // it to find the application model

        final ApplicationModel testModel = appModelFactory.resolveAppModel()
                .getApplicationModel();
        System.out.println("HOLLY test model is " + testModel);
        //                    System.out.println(
        //                            "module dir is " + Arrays.toString(testModel
        //                            .getWorkspaceModules().toArray()));
        //                    System.out.println(
        //                            "module dir is " + ((WorkspaceModule) testModel
        //                            .getWorkspaceModules().toArray()[0])
        //                            .getModuleDir());
        System.out.println(
                "app dir is " + testModel.getApplicationModule()
                        .getModuleDir());

        System.out.println("HOLLY after launch mode is " + LaunchMode.current());
        final QuarkusBootstrap.Mode currentMode = curatedApplication.getQuarkusBootstrap()
                .getMode();

        // TODO can we get a loader without doing a full start?
        ClassLoader loader = coreQuarkusTestExtension.doJavaStart(applicationRoot,
                curatedApplication, false);
        Thread.currentThread().setContextClassLoader(loader);
        Consumer currentTestAppConsumer = (Consumer) loader.loadClass(
                CurrentTestApplication.class.getName())
                .getDeclaredConstructor()
                .newInstance();
        currentTestAppConsumer.accept(curatedApplication);

        Class answer = loader.loadClass(className);
        System.out.println("LOADING CLASS GAVE ABSER" + answer);
        return answer;

        // TODO we ought to close these things

    }

    //    protected HierarchicalTestExecutorService createExecutorService(ExecutionRequest request) {
    //        JupiterConfiguration configuration = this.getJupiterConfiguration(request);
    //        return (HierarchicalTestExecutorService) (configuration.isParallelExecutionEnabled()
    //                ? new ForkJoinPoolHierarchicalTestExecutorService(new PrefixedConfigurationParameters(
    //                        request.getConfigurationParameters(), "junit.jupiter.execution.parallel.config."))
    //                : super.createExecutorService(request));
    //    }
    //
    protected JupiterEngineExecutionContext createExecutionContext(ExecutionRequest request) {
        return new JupiterEngineExecutionContext(request.getEngineExecutionListener(), this.getJupiterConfiguration(request));
    }

    //
    //    protected ThrowableCollector.Factory createThrowableCollectorFactory(ExecutionRequest request) {
    //        return JupiterThrowableCollectorFactory::createThrowableCollector;
    //    }
    //
    private JupiterConfiguration getJupiterConfiguration(ExecutionRequest request) {
        JupiterEngineDescriptor engineDescriptor = (JupiterEngineDescriptor) request.getRootTestDescriptor();
        return engineDescriptor.getConfiguration();
    }
}