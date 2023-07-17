package io.quarkus.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;

public class QuarkusGradleWrapperTestBase extends QuarkusGradleTestBase {

    private static final String GRADLE_WRAPPER_WINDOWS = "gradlew.bat";
    private static final String GRADLE_WRAPPER_UNIX = "./gradlew";
    private static final String MAVEN_REPO_LOCAL = "maven.repo.local";

    private Map<String, String> systemProps;

    private boolean configurationCacheEnable = true;
    private boolean noWatchFs = true;

    protected void setupTestCommand() {

    }

    /**
     * Gradle's configuration cache is enabled by default for all tests. This option can be used to disable the
     * configuration test.
     */
    protected void gradleConfigurationCache(boolean configurationCacheEnable) {
        this.configurationCacheEnable = configurationCacheEnable;
    }

    /**
     * Gradle is run by default with {@code --no-watch-fs} to reduce I/O load during tests. Some tests might run into issues
     * with this option.
     */
    protected void gradleNoWatchFs(boolean noWatchFs) {
        this.noWatchFs = noWatchFs;
    }

    public BuildResult runGradleWrapper(File projectDir, String... args) throws IOException, InterruptedException {
        return runGradleWrapper(false, projectDir, args);
    }

    public BuildResult runGradleWrapper(boolean expectError, File projectDir, String... args)
            throws IOException, InterruptedException {
        return runGradleWrapper(expectError, projectDir, true, args);
    }

    public BuildResult runGradleWrapper(boolean expectError, File projectDir, boolean skipAnalytics, String... args)
            throws IOException, InterruptedException {
        setupTestCommand();
        List<String> command = new ArrayList<>();
        command.add(getGradleWrapperCommand());
        addSystemProperties(command);
        command.add("-Dorg.gradle.console=plain");
        if (skipAnalytics) {
            command.add("-Dquarkus.analytics.disabled=true");
        }
        if (configurationCacheEnable) {
            command.add("--configuration-cache");
        }
        command.add("--stacktrace");
        if (noWatchFs) {
            command.add("--no-watch-fs");
        }
        command.add("--info");
        command.add("--daemon");
        command.addAll(Arrays.asList(args));

        File logOutput = new File(projectDir, "command-output.log");

        System.out.println("$ " + String.join(" ", command));
        ProcessBuilder pb = new ProcessBuilder()
                .directory(projectDir)
                .command(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(logOutput)
                // Should prevent "fragmented" output (parts of stdout and stderr interleaved)
                .redirectErrorStream(true);
        if (System.getenv("JAVA_HOME") == null) {
            // This helps running the tests in IntelliJ w/o configuring an explicit JAVA_HOME env var.
            pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
        }
        Process p = pb.start();

        //long timeout for native tests
        //that may also need to download docker
        boolean done = p.waitFor(10, TimeUnit.MINUTES);
        if (!done) {
            destroyProcess(p);
        }
        final BuildResult commandResult = BuildResult.of(logOutput);
        int exitCode = p.exitValue();

        // The test failed, if the Gradle build exits with != 0 and the tests expects no failure, or if the test
        // expects a failure and the exit code is 0.
        if (expectError == (exitCode == 0)) {
            // Only print the output, if the test does not expect a failure.
            printCommandOutput(projectDir, command, commandResult, exitCode);
            // Fail hard, if the test does not expect a failure.
            Assertions.fail("Gradle build failed with exit code %d", exitCode);
        }
        return commandResult;
    }

    protected void setSystemProperty(String name, String value) {
        if (systemProps == null) {
            systemProps = new HashMap<>();
        }
        systemProps.put(name, value);
    }

    private String getGradleWrapperCommand() {
        return Paths.get(getGradleWrapperName()).toAbsolutePath().toString();
    }

    private String getGradleWrapperName() {
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows")) {
            return GRADLE_WRAPPER_WINDOWS;
        }
        return GRADLE_WRAPPER_UNIX;
    }

    private void addSystemProperties(List<String> args) {
        if (systemProps != null) {
            systemProps.entrySet().stream().map(e -> toPropertyArg(e.getKey(), e.getValue())).forEach(args::add);
        }

        final String mavenRepoLocal = System.getProperty(MAVEN_REPO_LOCAL);
        if (mavenRepoLocal != null) {
            args.add(toPropertyArg(MAVEN_REPO_LOCAL, mavenRepoLocal));
        }
    }

    private static String toPropertyArg(String name, String value) {
        return "-D" + name + "=" + value;
    }

    private void printCommandOutput(File projectDir, List<String> command, BuildResult commandResult, int exitCode) {
        System.err.println(
                "Command: " + String.join(" ", command) + " ran from: " + projectDir.getAbsolutePath()
                        + " failed with exit code: " + exitCode + " and the following output:");
        System.err.println(commandResult.getOutput());
    }

    /**
     * Try to destroy the process normally a few times
     * and resort to forceful destruction if necessary
     */
    private static void destroyProcess(Process wrapperProcess) {
        wrapperProcess.destroy();
        int i = 0;
        while (i++ < 10) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {

            }
            if (!wrapperProcess.isAlive()) {
                break;
            }
        }

        if (wrapperProcess.isAlive()) {
            wrapperProcess.destroyForcibly();
        }
    }
}
