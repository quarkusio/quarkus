package io.quarkus.devtools.codestarts.quarkus;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.quarkus.deployment.util.ProcessUtil;

public final class WrapperRunner {

    enum Wrapper {
        GRADLE("gradlew", "gradlew.bat", new String[] { "--no-daemon", "build", "-i" }),
        MAVEN("mvnw", "mvnw.cmd", new String[] { "package" });

        private final String execUnix;
        private final String execWindows;
        private final String[] cmdArgs;

        Wrapper(String execUnix, String execWindows, String[] cmdArgs) {
            this.execUnix = execUnix;
            this.execWindows = execWindows;
            this.cmdArgs = cmdArgs;
        }

        public String getExec() {
            return System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows") ? execWindows : execUnix;
        }

        public String[] getCmdArgs() {
            return cmdArgs;
        }

        public static Wrapper fromBuildtool(String buildtool) {
            switch (buildtool) {
                case "maven":
                    return MAVEN;
                case "gradle":
                case "gradle-kotlin-dsl":
                    return GRADLE;
                default:
                    throw new IllegalStateException("No wrapper linked to buildtool: " + buildtool);
            }
        }
    }

    public static int run(Path projectDir, Wrapper wrapper) {
        List<String> command = new LinkedList<>();
        command.add(projectDir.resolve(wrapper.getExec()).toAbsolutePath().toString());
        command.addAll(Arrays.asList(wrapper.getCmdArgs()));

        if (System.getProperties().containsKey("maven.repo.local")) {
            command.add("-Dmaven.repo.local=" + System.getProperty("maven.repo.local"));
        }

        try {
            System.out.println("Running command: " + command);
            final Process p = new ProcessBuilder()
                    .directory(projectDir.toFile())
                    .command(command)
                    .start();
            try {
                ProcessUtil.streamToSysOutSysErr(p);
                p.waitFor(10, TimeUnit.MINUTES);
                return p.exitValue();
            } catch (InterruptedException e) {
                p.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return -1;
    }

}
