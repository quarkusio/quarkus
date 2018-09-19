package org.jboss.shamrock.junit;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.shamrock.deployment.ArchiveContextBuilder;
import org.jboss.shamrock.runner.RuntimeRunner;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class ShamrockTest extends BlockJUnit4ClassRunner {

    private static boolean first = true;
    private static boolean started = false;
    private static boolean failed = false;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public ShamrockTest(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    public void run(final RunNotifier notifier) {

        runInternal(notifier);
        super.run(notifier);
    }

    private void runInternal(RunNotifier notifier) {
        if (first) {
            first = false;
            //now we need to bootstrap shamrock
            try {
                notifier.addListener(new RunListener() {


                    @Override
                    public void testStarted(Description description) {
                        if (failed) {
                            notifier.fireTestFailure(new Failure(description, new AssertionError("Startup failed")));
                            return;
                        }
                        if (!started) {
                            started = true;
                            //TODO: so much hacks...
                            try {
                                Class<?> theClass = description.getTestClass();
                                String classFileName = theClass.getName().replace(".", "/") + ".class";
                                URL resource = theClass.getClassLoader().getResource(classFileName);
                                String testClassLocation = resource.getPath().substring(0, resource.getPath().length() - classFileName.length());
                                String appClassLocation = testClassLocation.replace("test-classes", "classes");
                                Path appRoot = Paths.get(appClassLocation);
                                RuntimeRunner runtimeRunner = new RuntimeRunner(getClass().getClassLoader(), appRoot, Paths.get(testClassLocation), new ArchiveContextBuilder());
                                runtimeRunner.run();
                            } catch (RuntimeException e) {
                                failed = true;
                                throw e;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(ShamrockTest.class), e));
            }
        }
    }
}
