package org.jboss.shamrock.junit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * A test runner for Graal native images
 * basically just a big pile of hacks
 */
public class GraalTest extends BlockJUnit4ClassRunner {

    private static boolean first = true;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public GraalTest(Class<?> klass) throws InitializationError {
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
            URL mainClassUri = getClass().getClassLoader().getResource("org/jboss/shamrock/runner/Main.class");
            if (mainClassUri == null) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), new RuntimeException("Unable to find shamrock main class")));
                return;
            }
            String externalForm = mainClassUri.getPath();
            int jar = externalForm.lastIndexOf('!');
            if (jar == -1) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), new RuntimeException("Cannot find jar to image " + mainClassUri + " is not in a jar archive")));
                return;
            }
            String path = externalForm.substring(5, jar);

            try {
                String outputFile = path.substring(0, path.lastIndexOf('.'));
                System.out.println("Executing " + outputFile);
                final Process testProcess = Runtime.getRuntime().exec(outputFile);
                notifier.addListener(new RunListener() {
                    @Override
                    public void testRunFinished(Result result) throws Exception {
                        super.testRunFinished(result);
                        testProcess.destroy();
                    }
                });
                new Thread(new ProcessReader(testProcess.getInputStream())).start();
                new Thread(new ProcessReader(testProcess.getErrorStream())).start();

                Thread.sleep(1000); //wait for the image to be up, should check the port

            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), e));
            }

        }
    }

    private static final class ProcessReader implements Runnable {

        private final InputStream inputStream;

        private ProcessReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    System.out.print(new String(b, 0, i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
