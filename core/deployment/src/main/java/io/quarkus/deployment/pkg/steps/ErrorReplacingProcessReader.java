package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substrate prints incomprehensible and useless 'potential call paths' that look like stack traces
 * <p>
 * This class intercepts them and prints meaningful output instead, so users don't waste hours going on wild goose
 * chases
 */
public final class ErrorReplacingProcessReader {

    private static final String LINE_START = "Call path from entry point to ";

    /**
     * Printed by native-image when the C compiler it needs to link the executable is not installed, for example
     * {@code Error: Default native-compiler executable 'gcc' not found via environment variable PATH}. The exit code
     * does not tell this apart from other failures, but the output does.
     */
    private static final String MISSING_C_COMPILER = "native-compiler executable";
    private static final String MISSING_C_COMPILER_HINT = "the native-image build could not find a C compiler. Install the "
            + "platform C toolchain: the Microsoft Visual C++ Build Tools on Windows, gcc with the glibc and zlib headers "
            + "on Linux, or the Xcode Command Line Tools on macOS.";

    private final BufferedReader reader;
    private final File reportdir;

    private ReportAnalyzer reportAnalyzer;

    public ErrorReplacingProcessReader(final BufferedReader reader, final File reportdir) {
        this.reader = reader;
        this.reportdir = reportdir;
    }

    public void run() throws IOException {
        Deque<String> fullBuffer = new ArrayDeque<>();
        Set<String> hints = new LinkedHashSet<>();
        boolean buffering = false;
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            String hint = hintForLine(line);
            if (hint != null) {
                hints.add(hint);
            }
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
        for (String hint : hints) {
            System.err.println("Hint: " + hint);
        }
    }

    /**
     * Returns the hint for a native-image output line matching a known signature, or {@code null} if none matches.
     */
    static String hintForLine(String line) {
        return line.contains(MISSING_C_COMPILER) ? MISSING_C_COMPILER_HINT : null;
    }

    private void handleErrorState(File report, String firstLine, Deque<String> queue) {
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
        System.err.println("-- Quarkus has attempted to generate a more meaningful call flow analysis below          ---");
        System.err.println("---------------------------------------------------------------------------------------------\n");
        try {
            String fullName = m.group(1);

            int index = fullName.lastIndexOf('.');
            String clazz = fullName.substring(0, index);
            String method = fullName.substring(index + 1);
            if (reportAnalyzer == null) {
                reportAnalyzer = new ReportAnalyzer(report.getAbsolutePath());
            }
            System.err.println(reportAnalyzer.analyse(clazz, method));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println(line);
    }
}
