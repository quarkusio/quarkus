/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
public class SubstrateTest extends BlockJUnit4ClassRunner {

    private static final long IMAGE_WAIT_TIME = 60000;

    private static boolean first = true;

    /**
     * Creates a BlockJUnit4ClassRunner to run {@code klass}
     *
     * @param klass
     * @throws InitializationError if the test class is malformed.
     */
    public SubstrateTest(Class<?> klass) throws InitializationError {
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
            notifier.addListener(new RunListener() {


                @Override
                public void testStarted(Description description) throws Exception {
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

                        if (guessedPath == null) {
                            notifier.fireTestFailure(new Failure(Description.createSuiteDescription(SubstrateTest.class), new RuntimeException("Unable to find native image, make sure native.image.path is set")));
                            return;
                        } else {
                            String errorString = "\n=native.image.path was not set, making a guess that  " + guessedPath + " is the correct native image=";
                            for (int i = 0; i < errorString.length(); ++i) {
                                System.err.print("=");
                            }
                            System.err.println(errorString);
                            for (int i = 0; i < errorString.length(); ++i) {
                                System.err.print("=");
                            }
                            System.err.println();
                            path = guessedPath;
                        }
                    }
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

                    int port = Integer.getInteger("http.port", 8080);
                    long bailout = System.currentTimeMillis() + IMAGE_WAIT_TIME;
                    boolean ok = false;
                    while (System.currentTimeMillis() < bailout) {
                        try {
                            Thread.sleep(100);
                            try (Socket s = new Socket()) {
                                s.connect(new InetSocketAddress("localhost", port));
                                ok = true;
                                break;
                            }
                        } catch (Exception expected) {
                        }
                    }
                    if (!ok) {
                        throw new RuntimeException("Unable to start native image in " + IMAGE_WAIT_TIME + "ms");
                    }
                }
            });
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
                //ignore
            }
        }
    }
}
