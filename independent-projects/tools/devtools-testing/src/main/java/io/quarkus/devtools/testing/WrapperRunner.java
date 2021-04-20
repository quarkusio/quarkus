package io.quarkus.devtools.testing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class WrapperRunner {

    public enum Wrapper {
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

        public static Wrapper detect(Path projectDir) {
            for (Wrapper value : Wrapper.values()) {
                final File file = projectDir.resolve(value.getExec()).toFile();
                if (file.isFile() && file.canExecute()) {
                    return value;
                }
            }
            throw new IllegalStateException("No supported wrapper that can be executed found in this directory: " + projectDir);
        }
    }

    public static int run(Path projectDir) {
        return run(projectDir, Wrapper.detect(projectDir));
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
                streamToSysOutSysErr(p);
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

    private static void streamToSysOutSysErr(final Process process) {
        streamOutputToSysOut(process);
        streamErrorToSysErr(process);
    }

    private static void streamOutputToSysOut(final Process process) {
        final InputStream processStdOut = process.getInputStream();
        final Thread t = new Thread(new Streamer(processStdOut, System.out));
        t.setName("Process stdout streamer");
        t.setDaemon(true);
        t.start();
    }

    private static void streamErrorToSysErr(final Process process) {
        streamErrorTo(System.err, process);
    }

    private static void streamErrorTo(final PrintStream printStream, final Process process) {
        final InputStream processStdErr = process.getErrorStream();
        final Thread t = new Thread(new Streamer(processStdErr, printStream));
        t.setName("Process stderr streamer");
        t.setDaemon(true);
        t.start();
    }

    private static final class Streamer implements Runnable {

        private final InputStream processStream;
        private final PrintStream consumer;

        private Streamer(final InputStream processStream, final PrintStream consumer) {
            this.processStream = processStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(processStream, StandardCharsets.UTF_8))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    consumer.println(line);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
