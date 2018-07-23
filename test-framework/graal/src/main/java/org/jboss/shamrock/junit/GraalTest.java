package org.jboss.shamrock.junit;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

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
        boolean reportAtRuntime = Boolean.getBoolean("graal.reportAtRuntime");
        if (first) {
            first = false;
            String graal = System.getenv("GRAALVM_HOME");
            if (graal == null) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), new RuntimeException("GRAALVM_HOME was not set")));
                return;
            }
            String nativeImage = graal + File.separator + "bin" + File.separator + "native-image";

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

                String[] command;
                if (reportAtRuntime) {
                    command = new String[]{nativeImage, "-jar", path, "-H:+ReportUnsupportedElementsAtRuntime", "-H:IncludeResources=META-INF/.*"};
                } else {
                    command = new String[]{nativeImage, "-jar", path, "-H:IncludeResources=META-INF/.*"};
                }

                Process process = Runtime.getRuntime().exec(command, new String[]{}, new File(path.substring(0, path.lastIndexOf(File.separator))));
                CompletableFuture<String> output = new CompletableFuture<>();
                CompletableFuture<String> errorOutput = new CompletableFuture<>();
                new Thread(new ProcessReader(process.getInputStream(), output)).start();
                if (process.waitFor() != 0) {
                    notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), new RuntimeException("Image generation failed: " + output.get())));
                    return;
                }

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
                new Thread(new ProcessReader(testProcess.getInputStream(), output)).start();
                new Thread(new ProcessReader(testProcess.getErrorStream(), errorOutput)).start();
                output.whenComplete(new BiConsumer<String, Throwable>() {
                    @Override
                    public void accept(String s, Throwable throwable) {
                        if (throwable != null) {
                            throwable.printStackTrace();
                        } else {
                            System.out.println(s);
                        }
                    }
                });
                errorOutput.whenComplete(new BiConsumer<String, Throwable>() {
                    @Override
                    public void accept(String s, Throwable throwable) {
                        if (throwable != null) {
                            throwable.printStackTrace();
                        } else {
                            System.err.println(s);
                        }
                    }
                });
                Thread.sleep(1000); //wait for the image to be up, should check the port

            } catch (Exception e) {
                notifier.fireTestFailure(new Failure(Description.createSuiteDescription(GraalTest.class), e));
            }

        }
    }

    private static final class ProcessReader implements Runnable {

        private final InputStream inputStream;
        private final CompletableFuture<String> result;

        private ProcessReader(InputStream inputStream, CompletableFuture<String> result) {
            this.inputStream = inputStream;
            this.result = result;
        }

        @Override
        public void run() {

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[100];
            int i;
            try {
                while ((i = inputStream.read(b)) > 0) {
                    out.write(b, 0, i);
                }
                result.complete(new String(out.toByteArray()));
            } catch (IOException e) {
                result.completeExceptionally(e);
            }
        }
    }
}
