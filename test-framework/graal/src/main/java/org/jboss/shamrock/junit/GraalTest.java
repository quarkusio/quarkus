package org.jboss.shamrock.junit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

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
            String path = System.getProperty("native.image.path");
            if (path == null) {
                //ok, lets make a guess
                //this is a horrible hack, but it is intended to make this work in IDE's

                ClassLoader cl = getClass().getClassLoader();
                String guessedPath = null;
                if (cl instanceof URLClassLoader) {
                    URL[] urls = ((URLClassLoader) cl).getURLs();
                    for (URL url : urls) {
                        if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
                            //we have the test classes dir
                            File testClasses = new File(url.getPath());
                            for (File file : testClasses.getParentFile().listFiles()) {
                                if (file.getName().endsWith("-runner")) {
                                    guessedPath = file.getAbsolutePath();
                                    break;
                                }
                            }
                        }
                        if (guessedPath != null) {
                            break;
                        }
                    }
                }

                if(guessedPath == null) {
                    notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), new RuntimeException("Unable to find native image, make sure native.image.path is set")));
                    return;
                } else {
                    String errorString = "=native.image.path was not set, making a guess that  " + guessedPath + " is the correct native image=";
                    for(int i= 0; i < errorString.length(); ++i) {
                        System.err.print("=");
                    }
                    System.err.println(errorString);
                    for(int i= 0; i < errorString.length(); ++i) {
                        System.err.print("=");
                    }
                    path = guessedPath;
                }
            }
            try {
                System.out.println("Executing " + path);
                final Process testProcess = Runtime.getRuntime().exec(path);
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
