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
package io.quarkus.gradle.it;

import ch.vorburger.exec.ManagedProcess;
import ch.vorburger.exec.ManagedProcessBuilder;

/**
 * Launch Quarkus JARs as external processes for integration testing.
 *
 * @author <a href="mailto:mike@vorburger.ch">Michael Vorburger.ch</a>
 */
public class QuarkusJavaLauncher implements AutoCloseable {

    // TODO Move this test utility class somewhere common so that it can be used by other ITs

    public static final int MAX_CONSOLE_LINES = 1000;
    private static final String EXPECTED_CONSOLE_MESSAGE = "Listening on: http";
    private static final int MAX_WAIT_IN_MS = 13000;

    private final ManagedProcess process;

    // Use throws Exception instead of ManagedProcessException so that users don't depend on types from the internally used library
    public QuarkusJavaLauncher(String pathToQuarkusJAR) throws Exception {
        process = new ManagedProcessBuilder(getJavaExecutable())
                .addArgument("-jar").addArgument(pathToQuarkusJAR)
                .setConsoleBufferMaxLines(MAX_CONSOLE_LINES).build();

        if (!process.startAndWaitForConsoleMessageMaxMs(EXPECTED_CONSOLE_MESSAGE, MAX_WAIT_IN_MS)) {
            process.destroy();
            throw new IllegalStateException(
                    EXPECTED_CONSOLE_MESSAGE + " did not appear (after waiting " + MAX_WAIT_IN_MS / 1000 + "s)");
        }
    }

    protected String getJavaExecutable() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            return "java";
        } else {
            // required due to https://github.com/quarkusio/quarkus/issues/2012
            return javaHome + "/bin/java";
        }
    }

    /**
     * Obtain the output which the process printed to (both) STDOUT and STDERR.
     *
     * It includes up to maximum {@link #MAX_CONSOLE_LINES} lines; earlier output is lost.
     *
     * This is useful for <a href="https://github.com/quarkusio/quarkus/issues/2005">tests who want to assert on the (non)
     * appearance of particular log messages.
     *
     * @return new line separated multi-line output text
     */
    public String getOutput() {
        return process.getConsole();
    }

    /**
     * Stop the external JVM.
     */
    @Override
    public void close() throws Exception {
        if (process.isAlive()) {
            process.destroy();
        }
    }
}
