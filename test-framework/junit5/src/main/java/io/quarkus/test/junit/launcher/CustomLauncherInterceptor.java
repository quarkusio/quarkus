package io.quarkus.test.junit.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.LauncherSession;

import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupActionHolder;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import io.quarkus.deployment.dev.testing.CoreQuarkusTestExtension;
import io.quarkus.deployment.dev.testing.CurrentTestApplication;
import io.quarkus.deployment.dev.testing.TestSupport;
import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;

public class CustomLauncherInterceptor implements LauncherInterceptor {

    protected static final String TEST_LOCATION = "test-location";
    protected static final String TEST_CLASS = "test-class";

    private final ClassLoader customClassLoader;

    public CustomLauncherInterceptor() throws Exception {
        System.out.println("HOLLY interceipt construct" + getClass().getClassLoader());
        ClassLoader parent = Thread.currentThread()
                .getContextClassLoader();
        System.out.println("HOLLY CCL is " + parent);

        customClassLoader = parent;
        System.out.println("HOLLY stored variable loader" + customClassLoader);
    }

    @Override
    public <T> T intercept(Invocation<T> invocation) {
        // We visit this several times
        System.out.println("HOLLY interceipt doing" + invocation);
        System.out.println("HOLLY interceipt support is " + TestSupport.instance()
                .isPresent());
        System.out.println("HOLLY interceipt holder is " + StartupActionHolder.getStored());

        // This class sets a Thead Context Classloader, which JUnit uses to load classes.
        // However, in continuous testing mode, setting a TCCL here isn't sufficient for the
        // tests to come in with our desired classloader;
        // downstream code sets the classloader to the deployment classloader, so we then need
        // to come in *after* that code.

        // TODO sometimes this is called in dev mode and sometimes it isn't? Ah, it's only not
        //  called if we die early, before we get to this

        // In continuous testing mode, the runner code will have executed before this
        // interceptor, so
        // this interceptor doesn't need to do anything.
        // TODO what if we removed the changes in the runner code?

        // Bypass all this in continuous testing mode; the startup action holder is our best way
        // of detecting it
        if (StartupActionHolder.getStored() == null) {
            if (invocation instanceof LauncherSession) {
                LauncherSession sess = (LauncherSession) invocation;
                sess.getLauncher()
                        .registerLauncherDiscoveryListeners(new LauncherDiscoveryListener() {
                            @Override
                            public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
                                System.out.println("YOYO discovery started " + request);
                                LauncherDiscoveryListener.super.launcherDiscoveryStarted(request);
                            }
                        });
            }

            Thread currentThread = Thread.currentThread();
            ClassLoader originalClassLoader = currentThread.getContextClassLoader();

            //       infinite loop, this is what triggers this method LauncherDiscoveryRequest
            //       request = LauncherDiscoveryRequestBuilder.request()
            //                .filters(includeClassNamePatterns(".*"))
            //                .build();
            //
            //        TestPlan plan = LauncherFactory.create().discover(request);
            //
            //        for (TestIdentifier root : plan.getRoots()) {
            //            System.out.println("Root: " + root.toString());
            //
            //            for (TestIdentifier test : plan.getChildren(root)) {
            //                System.out.println("Found test: " + test.toString());
            //            }
            //        }

            System.out.println("HOLLY before launch mode is " + LaunchMode.current());
            System.out.println("HOLLY other way us " + ConfigProvider.getConfig()
                    .unwrap(SmallRyeConfig.class)
                    .getProfiles());
            try {
                System.out.println("HOLLY interceipt original" + originalClassLoader);
                CoreQuarkusTestExtension coreQuarkusTestExtension = new CoreQuarkusTestExtension();

                // TODO we normally start with a test class and work out the runtime classpath
                //  from that;
                // here we need to go in the other direction. There's probably existing logic we
                // can re-use, because this
                // is seriously brittle
                // TODO massi   ve assumptions, completely ignoring multi module projects
                // Assume the test class lives in the first element on the classpath
                Path projectRoot = Paths.get("")
                        .normalize()
                        .toAbsolutePath();

                // TODO can we be more elegant about this? What about multi-module?
                // TODO should we cross-reference against the classpath?

                Path targetDir = projectRoot.resolve("target/test-classes");

                // The gradle case; can we know in advance which it is?
                Path buildDir = projectRoot.resolve("build");

                System.out.println(
                        "HOLLY whole class path is " + System.getProperty("java.class.path"));
                Path applicationRoot;

                // TODO probably pointless, to make sure it's shown as set
                if (Files.exists(targetDir)) {
                    applicationRoot = targetDir;
                } else {
                    applicationRoot = buildDir;
                }

                String[] ses = System.getProperty("java.class.path")
                        .split(":");
                for (String s : ses) {
                    Path path = Paths.get(s);
                    if (path.normalize()
                            .toAbsolutePath()
                            .startsWith(projectRoot)) {
                        System.out.println("CANDIDATE CLASSPATH " + s);
                        // TODO ugly; set it if we didn't set it to something on the classpath
                        // TODO fragile, we miss other modules in multi-module
                        //  things like us to take the first element on the classpath that fits
                        if (applicationRoot == targetDir || applicationRoot == buildDir) {
                            applicationRoot = path;
                            System.out.println("made app root" + applicationRoot);
                        }
                    }
                }

                CuratedApplication curatedApplication;
                // TODO this makes no sense here because we're on the wrong classloader unless a
                //  TCCL is already around, and we reset it
                if (CurrentTestApplication.curatedApplication != null) {
                    System.out.println("Re-using curated application");
                    curatedApplication = CurrentTestApplication.curatedApplication;
                } else {

                    System.out.println("MAKING Curated application with root " + applicationRoot);

                    System.out.println("An alternate root we couuld do is " + projectRoot);
                    System.out.println(
                            "That gives gradlle " + getGradleAppModelForIDE(Paths.get("")
                                    .normalize()
                                    .toAbsolutePath()));

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

                    //                    QuarkusClassLoader tcl = curatedApplication
                    //                    .createDeploymentClassLoader();
                    //                    System.out.println("HOLLY interceptor just made a " +
                    //                    tcl);

                    // TODO should we set the context classloader to the deployment classloader?
                    // If not, how will anyone retrieve it?
                    // TODO commenting this out doesn't change much?
                    //                    Consumer currentTestAppConsumer = (Consumer) tcl
                    //                    .loadClass(CurrentTestApplication.class.getName())
                    //                            .getDeclaredConstructor().newInstance();
                    //                    currentTestAppConsumer.accept(curatedApplication);

                    // TODO   move this to close     shutdownTasks.add(curatedApplication::close);
                }

                System.out.println("HOLLY after launch mode is " + LaunchMode.current());
                final QuarkusBootstrap.Mode currentMode = curatedApplication.getQuarkusBootstrap()
                        .getMode();
                ClassLoader loader = coreQuarkusTestExtension.doJavaStart(applicationRoot,
                        curatedApplication);
                currentThread.setContextClassLoader(loader);

                System.out.println("HOLLY did set to " + currentThread.getContextClassLoader());

                return invocation.proceed();

            } catch (Exception e) {
                e.printStackTrace();
                return invocation.proceed();
            } finally {
                currentThread.setContextClassLoader(originalClassLoader);
            }
        } else {
            // TODO should we be unsetting the classloader somewhere?
            Thread.currentThread()
                    .setContextClassLoader(StartupActionHolder.getStored()
                            .getClassLoader());
            System.out.println("HOLLY NOW TCCL IS " + Thread.currentThread()
                    .getContextClassLoader());
            return invocation.proceed();
        }
    }

    private ApplicationModel getGradleAppModelForIDE(Path projectRoot) throws IOException,
            AppModelResolverException {
        return System.getProperty(BootstrapConstants.SERIALIZED_TEST_APP_MODEL) == null
                ? BuildToolHelper.enableGradleAppModelForTest(projectRoot)
                : null;
    }

    @Override
    public void close() {

        //        //        try {
        //        //            // TODO     customClassLoader.close();
        //        //        } catch (Exception e) {
        //        //            throw new UncheckedIOException("Failed to close custom class
        //        loader", e);
        //        //        }
    }
}
