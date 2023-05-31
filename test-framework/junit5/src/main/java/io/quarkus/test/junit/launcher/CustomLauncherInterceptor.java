package io.quarkus.test.junit.launcher;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.platform.launcher.LauncherDiscoveryListener;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.LauncherInterceptor;
import org.junit.platform.launcher.LauncherSession;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.StartupActionHolder;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
        System.out.println("HOLLY interceipt support is " + TestSupport.instance().isPresent());
        System.out.println("HOLLY interceipt holder is " + StartupActionHolder.getStored());
        // System.out.println("HOLLY classpaht is " + System.getProperty("java.class.path"));

        // Bypass all this in continuous testing mode; the startup action holder is our best way of detecting it
        if (StartupActionHolder.getStored() == null) {
            if (invocation instanceof LauncherSession) {
                LauncherSession sess = (LauncherSession) invocation;
                sess.getLauncher().registerLauncherDiscoveryListeners(new LauncherDiscoveryListener() {
                    @Override
                    public void launcherDiscoveryStarted(LauncherDiscoveryRequest request) {
                        System.out.println("YOYO discovery started " + request);
                        LauncherDiscoveryListener.super.launcherDiscoveryStarted(request);
                    }
                });
            }

            Thread currentThread = Thread.currentThread();
            ClassLoader originalClassLoader = currentThread.getContextClassLoader();

            //       infinite loop, this is what triggers this method LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
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
            System.out.println("HOLLY other way us " + ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getProfiles());
            try {
                System.out.println("HOLLY interceipt original" + originalClassLoader);
                CoreQuarkusTestExtension coreQuarkusTestExtension = new CoreQuarkusTestExtension();

                // TODO we normally start with a test class and work out the runtime classpath from that;
                // here we need to go in the other direction. There's probably existing logic we can re-use, because this
                // is seriously brittle
                // TODO massive assumptions, completely ignoring multi module projects
                // Assume the test class lives in the first element on the classpath
                String s = System.getProperty("java.class.path")
                        .split(":")[0];
                System.out.println("HOLLY taking classath as " + s);
                Path applicationRoot;
                if (s.endsWith("jar")) {
                    applicationRoot = Path.of(s);
                } else {
                    // Even if it's a test-classes dir, no need to go up two levels to the main project
                    // The PathTestHelper will find the path we need
                    applicationRoot = Path.of(s);
                }
                System.out.println("made app root" + applicationRoot);

                CuratedApplication curatedApplication;
                // TODO this makes no sense here because we're on the wrong classloader unless a TCCL is already around, and we reset it
                if (CurrentTestApplication.curatedApplication != null) {
                    System.out.println("Re-using curated application");
                    curatedApplication = CurrentTestApplication.curatedApplication;
                } else {

                    System.out.println("MAKING Curated application");
                    curatedApplication = QuarkusBootstrap.builder()
                            //.setExistingModel(gradleAppModel)
                            // unfortunately this model is not re-usable
                            // due
                            // to PathTree serialization by Gradle
                            .setIsolateDeployment(true)
                            .setMode(
                                    QuarkusBootstrap.Mode.TEST) // Even in continuous testing, we set the mode to test - here, if we go down this path we know it's normal mode
                            // is this always right?
                            .setTest(true)

                            .setApplicationRoot(applicationRoot)
                            .setProjectRoot(applicationRoot)

                            //                    .setTargetDirectory(
                            //                            PathTestHelper
                            //                            .getProjectBuildDir(
                            //                                    projectRoot, testClassLocation))
                            //                        .setProjectRoot
                            //                        (projectRoot)
                            //                        .setApplicationRoot(rootBuilder.build())
                            .build()
                            .bootstrap();

                    QuarkusClassLoader tcl = curatedApplication.createDeploymentClassLoader();

                    // TODO should we set the context classloader to the deployment classloader?
                    // If not, how will anyone retrieve it?
                    Consumer currentTestAppConsumer = (Consumer) tcl.loadClass(CurrentTestApplication.class.getName())
                            .getDeclaredConstructor().newInstance();
                    currentTestAppConsumer.accept(curatedApplication);

                    // TODO   move this to close     shutdownTasks.add(curatedApplication::close);
                }

                System.out.println("HOLLY after launch mode is " + LaunchMode.current());
                final QuarkusBootstrap.Mode currentMode = curatedApplication.getQuarkusBootstrap().getMode();
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
            Thread.currentThread().setContextClassLoader(StartupActionHolder.getStored().getClassLoader());
            System.out.println("HOLLY NOW TCCL IS " + Thread.currentThread().getContextClassLoader());
            return invocation.proceed();
        }
    }

    @Override
    public void close() {

        //        //        try {
        //        //            // TODO     customClassLoader.close();
        //        //        } catch (Exception e) {
        //        //            throw new UncheckedIOException("Failed to close custom class loader", e);
        //        //        }
    }
}
