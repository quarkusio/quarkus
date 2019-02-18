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

package org.jboss.shamrock.creator.phase.nativeimage;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substrate prints incomprehensible and useless 'potential call paths' that look like stack traces
 * <p>
 * This class intercepts them and prints meaningful output instead, so users don't waste hours going on wild goose
 * chases
 */
public final class ErrorReplacingProcessReader implements Runnable {

    private static final String LINE_START = "Call path from entry point to ";
    private final InputStream inputStream;
    private final File reportdir;
    private final CountDownLatch doneLatch;

    public ErrorReplacingProcessReader(InputStream inputStream, File reportdir, CountDownLatch doneLatch) {
        this.inputStream = inputStream;
        this.reportdir = reportdir;
        this.doneLatch = doneLatch;
    }

    @Override
    public void run() {
        try {
            Deque<String> fullBuffer = new ArrayDeque<>();
            boolean buffering = false;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    if (line.startsWith(LINE_START)) {
                        buffering = true;
                    }
                    if (buffering) {
                        fullBuffer.add(line);
                    } else {
                        System.err.println(line);
                    }
                }
                File reportFile = null;
                if (reportdir.exists()) {
                    File[] files = reportdir.listFiles();
                    if (files != null) {
                        for (File j : files) {
                            if (j.getName().startsWith("call_tree")) {
                                reportFile = j;
                                break;
                            }
                        }

                    }
                }
                if (reportFile == null) {
                    for (String j : fullBuffer) {
                        System.err.println(j);
                    }
                } else {
                    while (!fullBuffer.isEmpty()) {
                        String line = fullBuffer.pop();
                        if (line.startsWith(LINE_START)) {
                            handleErrorState(reportFile, line, fullBuffer);
                        } else {
                            System.err.println(line);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } finally {
            doneLatch.countDown();
        }
    }

    private static void handleErrorState(File report, String firstLine, Deque<String> queue) {
        System.err.println(firstLine);
        String remainder = firstLine.substring(LINE_START.length());
        Matcher m = Pattern.compile("([^(]*).*").matcher(remainder);
        if (!m.find()) {
            return;
        }
        String line = "";
        while (!queue.isEmpty()) {
            line = queue.pop();
            if (line.trim().startsWith("at")) {
                System.err.println(line);
            } else {
                break;
            }
        }

        System.err.println("--------------------------------------------------------------------------------------------");
        System.err.println("-- WARNING: The above stack trace is not a real stack trace, it is a theoretical call tree---");
        System.err.println("-- If an interface has multiple implementations SVM will just display one potential call  ---");
        System.err.println("-- path to the interface. This is often meaningless, and what you actually need to know is---");
        System.err.println("-- the path to the constructor of the object that implements this interface.              ---");
        System.err.println("-- Shamrock has attempted to generate a more meaningful call flow analysis below          ---");
        System.err.println("---------------------------------------------------------------------------------------------\n");
        try {
            String fullName = m.group(1);

            int idex = fullName.lastIndexOf('.');
            String clazz = fullName.substring(0, idex);
            String method = fullName.substring(idex + 1);

            System.err.println(ReportAnalyzer.analyse(report.getAbsolutePath(), clazz, method));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println(line);
    }
}
