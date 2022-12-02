package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class LinuxIDUtil {

    private LinuxIDUtil() {
    }

    static String getLinuxID(String option) {
        Process process;

        try {
            StringBuilder responseBuilder = new StringBuilder();
            String line;

            ProcessBuilder idPB = new ProcessBuilder().command("id", option);
            idPB.redirectError(new File("/dev/null"));
            idPB.redirectInput(new File("/dev/null"));

            process = idPB.start();
            try (InputStream inputStream = process.getInputStream()) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    safeWaitFor(process);
                    return responseBuilder.toString();
                }
            } catch (Throwable t) {
                safeWaitFor(process);
                throw t;
            }
        } catch (IOException e) { //from process.start()
            //swallow and return null id
            return null;
        }
    }

    private static void safeWaitFor(Process process) {
        boolean intr = false;
        try {
            for (;;)
                try {
                    process.waitFor();
                    return;
                } catch (InterruptedException ex) {
                    intr = true;
                }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
