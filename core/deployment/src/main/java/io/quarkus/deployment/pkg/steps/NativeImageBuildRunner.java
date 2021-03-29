package io.quarkus.deployment.pkg.steps;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.logging.Logger;

import io.quarkus.deployment.pkg.steps.NativeImageBuildStep.GraalVM;
import io.quarkus.deployment.util.ProcessUtil;

public abstract class NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildRunner.class);

    public GraalVM.Version getGraalVMVersion() {
        final GraalVM.Version graalVMVersion;
        try {
            String[] versionCommand = getGraalVMVersionCommand(Collections.singletonList("--version"));
            log.debugf(String.join(" ", versionCommand).replace("$", "\\$"));
            Process versionProcess = new ProcessBuilder(versionCommand)
                    .redirectErrorStream(true)
                    .start();
            versionProcess.waitFor();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(versionProcess.getInputStream(), StandardCharsets.UTF_8))) {
                graalVMVersion = GraalVM.Version.of(reader.lines());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get GraalVM version", e);
        }
        return graalVMVersion;
    }

    public void setup(boolean processInheritIODisabled) {
    }

    public void cleanupServer(File outputDir) throws InterruptedException, IOException {
    }

    public int build(List<String> args, Path outputDir, boolean processInheritIODisabled)
            throws InterruptedException, IOException {
        preBuild(args);
        try {
            CountDownLatch errorReportLatch = new CountDownLatch(1);
            final String[] buildCommand = getBuildCommand(args);
            final ProcessBuilder processBuilder = new ProcessBuilder(buildCommand)
                    .directory(outputDir.toFile());
            log.info(String.join(" ", buildCommand).replace("$", "\\$"));
            final Process process = ProcessUtil.launchProcessStreamStdOut(processBuilder, processInheritIODisabled);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new ErrorReplacingProcessReader(process.getErrorStream(), outputDir.resolve("reports").toFile(),
                    errorReportLatch));
            executor.shutdown();
            errorReportLatch.await();
            return process.waitFor();
        } finally {
            postBuild();
        }
    }

    protected abstract String[] getGraalVMVersionCommand(List<String> args);

    protected abstract String[] getBuildCommand(List<String> args);

    protected void preBuild(List<String> buildArgs) throws IOException, InterruptedException {
    }

    protected void postBuild() throws InterruptedException, IOException {
    }

    /**
     * Run {@code command} in {@code workingDirectory} and log error if {@code errorMsg} is not null.
     *
     * @param command The command to run
     * @param errorMsg The error message to be printed in case of failure.
     *        If {@code null} the failure is ignored, but logged.
     * @param workingDirectory The directory in which to run the command
     */
    void runCommand(String[] command, String errorMsg, File workingDirectory) {
        log.info(String.join(" ", command).replace("$", "\\$"));
        Process process = null;
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            if (workingDirectory != null) {
                processBuilder.directory(workingDirectory);
            }
            process = processBuilder.start();
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                if (errorMsg != null) {
                    log.error(errorMsg);
                } else {
                    log.debugf("Command: " + String.join(" ", command) + " failed with exit code " + exitCode);
                }
            }
        } catch (IOException | InterruptedException e) {
            if (errorMsg != null) {
                log.error(errorMsg);
            } else {
                log.debugf(e, "Command: " + String.join(" ", command) + " failed.");
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
