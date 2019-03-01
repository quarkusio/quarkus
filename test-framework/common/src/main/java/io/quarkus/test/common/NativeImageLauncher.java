/*
 * Copyright 2019 Red Hat, Inc.
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

package io.quarkus.test.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.test.common.http.TestHTTPResourceManager;

public class NativeImageLauncher implements Closeable {

    private static final long IMAGE_WAIT_TIME = 60000;

    private final Class<?> testClass;
    private Process quarkusProcess;
    private final int port;

    public NativeImageLauncher(Class<?> testClass) {
        this(testClass, ConfigProvider.getConfig().getOptionalValue("quarkus.http.test-port", Integer.class).orElse(8081));
    }

    public NativeImageLauncher(Class<?> testClass, int port) {
        this.testClass = testClass;
        this.port = port;
    }

    public void start() throws Exception {

        String path = System.getProperty("native.image.path");
        if (path == null) {
            path = guessPath(testClass);
        }
        List<String> args = new ArrayList<>();
        args.add(path);
        args.add("-Dquarkus.http.port=" + port);
        args.add("-Dtest.url=" + TestHTTPResourceManager.getUri());
        args.add("-Dquarkus.log.file.path=target/quarkus.log");

        System.out.println("Executing " + args);

        quarkusProcess = Runtime.getRuntime().exec(args.toArray(new String[args.size()]));
        new Thread(new ProcessReader(quarkusProcess.getInputStream())).start();
        new Thread(new ProcessReader(quarkusProcess.getErrorStream())).start();

        waitForQuarkus();
    }

    private static String guessPath(Class<?> testClass) {
        //ok, lets make a guess
        //this is a horrible hack, but it is intended to make this work in IDE's

        ClassLoader cl = testClass.getClassLoader();

        if (cl instanceof URLClassLoader) {
            URL[] urls = ((URLClassLoader) cl).getURLs();
            for (URL url : urls) {
                if (url.getProtocol().equals("file") && url.getPath().endsWith("test-classes/")) {
                    //we have the test classes dir
                    File testClasses = new File(url.getPath());
                    for (File file : testClasses.getParentFile().listFiles()) {
                        if (file.getName().endsWith("-runner")) {
                            logGuessedPath(file.getAbsolutePath());
                            return file.getAbsolutePath();
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Unable to find native image, make sure native.image.path is set");
    }

    private static void logGuessedPath(String guessedPath) {
        String errorString = "\n=native.image.path was not set, making a guess that  " + guessedPath
                + " is the correct native image=";
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println(errorString);
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println();
    }

    private void waitForQuarkus() {
        long bailout = System.currentTimeMillis() + IMAGE_WAIT_TIME;

        while (System.currentTimeMillis() < bailout) {
            try {
                Thread.sleep(100);
                try (Socket s = new Socket()) {
                    s.connect(new InetSocketAddress("localhost", port));
                    return;
                }
            } catch (Exception expected) {
            }
        }

        throw new RuntimeException("Unable to start native image in " + IMAGE_WAIT_TIME + "ms");
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
                    System.out.print(new String(b, 0, i, StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void close() {
        quarkusProcess.destroy();
    }
}
