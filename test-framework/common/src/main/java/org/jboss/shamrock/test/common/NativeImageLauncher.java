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

package org.jboss.shamrock.test.common;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;

public class NativeImageLauncher implements Closeable {

    private static final long IMAGE_WAIT_TIME = 60000;

    private final Class<?> testClass;
    private Process shamrockProcess;

    public NativeImageLauncher(Class<?> testClass) {
        this.testClass = testClass;
    }

    public void start() throws Exception {

        String path = System.getProperty("native.image.path");
        if (path == null) {
            path = guessPath(testClass);
        }

        System.out.println("Executing " + path);

        shamrockProcess = Runtime.getRuntime().exec(path);
        new Thread(new ProcessReader(shamrockProcess.getInputStream())).start();
        new Thread(new ProcessReader(shamrockProcess.getErrorStream())).start();

        waitForShamrock();
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
        String errorString = "\n=native.image.path was not set, making a guess that  " + guessedPath + " is the correct native image=";
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println(errorString);
        for (int i = 0; i < errorString.length(); ++i) {
            System.err.print("=");
        }
        System.err.println();
    }

    private static void waitForShamrock() {
        int port = Integer.getInteger("http.port", 8080);
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
                    System.out.print(new String(b, 0, i));
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    @Override
    public void close() {
        shamrockProcess.destroy();
    }
}
