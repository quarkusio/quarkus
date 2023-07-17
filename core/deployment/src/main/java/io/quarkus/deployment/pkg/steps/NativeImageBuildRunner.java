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

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.util.ProcessUtil;

public abstract class NativeImageBuildRunner {

    private static final Logger log = Logger.getLogger(NativeImageBuildRunner.class);

    private static GraalVM.Version graalVMVersion = null;

    public GraalVM.Version getGraalVMVersion() {
        if (graalVMVersion == null) {
            try {
                final String[] versionCommand = getGraalVMVersionCommand(Collections.singletonList("--version"));
                log.debugf(String.join(" ", versionCommand).replace("$", "\\$"));
                final Process versionProcess = new ProcessBuilder(versionCommand)
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
        }
        return graalVMVersion;
    }

    public abstract boolean isContainer();

    public void setup(boolean processInheritIODisabled) {
    }

    public void addShutdownHook(Process buildNativeProcess) {
    }

    public Result build(List<String> args, String nativeImageName, String resultingExecutableName, Path outputDir,
            GraalVM.Version graalVMVersion, boolean debugSymbolsEnabled, boolean processInheritIODisabled)
            throws InterruptedException, IOException {
        preBuild(outputDir, args);
        try {
            CountDownLatch errorReportLatch = new CountDownLatch(1);
            final String[] buildCommand = getBuildCommand(outputDir, args);
            final ProcessBuilder processBuilder = new ProcessBuilder(buildCommand)
                    .directory(outputDir.toFile());
            log.info(String.join(" ", buildCommand).replace("$", "\\$"));
            final Process process = ProcessUtil.launchProcessStreamStdOut(processBuilder, processInheritIODisabled);
            addShutdownHook(process);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(new ErrorReplacingProcessReader(process.getErrorStream(), outputDir.resolve("reports").toFile(),
                    errorReportLatch));
            executor.shutdown();
            errorReportLatch.await();
            int exitCode = process.waitFor();
            boolean objcopyExists = objcopyExists();
            if (exitCode != 0) {
                return new Result(exitCode);
            }

            if (debugSymbolsEnabled && graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) < 0 && objcopyExists) {
                // Need to explicitly split debug symbols prior to GraalVM/Mandrel 23.0
                splitDebugSymbols(outputDir, nativeImageName, resultingExecutableName);
            }
            if (!(debugSymbolsEnabled && graalVMVersion.compareTo(GraalVM.Version.VERSION_23_0_0) >= 0)) {
                // Strip debug symbols even if not generated by GraalVM/Mandrel, because the underlying JDK might
                // contain them. Note, however, that starting with GraalVM/Mandrel 23.0 this is done by default when
                // generating debug info, so we don't want to do it twice and print twice a warning if objcopy is not
                // available.
                if (objcopyExists) {
                    objcopy(outputDir, "--strip-debug", resultingExecutableName);
                } else if (SystemUtils.IS_OS_LINUX) {
                    log.warn(
                            "objcopy executable not found in PATH. Debug symbols will therefore not be separated from the executable.");
                    log.warn("That also means that resulting native executable is larger as it embeds the debug symbols.");
                }
            }
            return new Result(0);
        } finally {
            postBuild(outputDir, nativeImageName, resultingExecutableName);
        }
    }

    private void splitDebugSymbols(Path outputDir, String nativeImageName, String resultingExecutableName) {
        String symbols = String.format("%s.debug", nativeImageName);
        objcopy(outputDir, "--only-keep-debug", resultingExecutableName, symbols);
        objcopy(outputDir, String.format("--add-gnu-debuglink=%s", symbols), resultingExecutableName);
    }

    protected abstract String[] getGraalVMVersionCommand(List<String> args);

    protected abstract String[] getBuildCommand(Path outputDir, List<String> args);

    protected boolean objcopyExists() {
        return true;
    }

    protected abstract void objcopy(Path outputDir, String... args);

    protected void preBuild(Path outputDir, List<String> buildArgs) throws IOException, InterruptedException {
    }

    protected void postBuild(Path outputDir, String nativeImageName, String resultingExecutableName)
            throws InterruptedException, IOException {
    }

    /**
     * Run {@code command} in {@code workingDirectory} and log error if {@code errorMsg} is not null.
     *
     * @param command The command to run
     * @param errorMsg The error message to be printed in case of failure.
     *        If {@code null} the failure is ignored, but logged.
     * @param workingDirectory The directory in which to run the command
     */
    static void runCommand(String[] command, String errorMsg, File workingDirectory) {
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

    static class Result {
        private final int exitCode;

        public Result(int exitCode) {
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }

    }
}
