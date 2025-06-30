package io.quarkus.devtools.testing;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.resolver.maven.options.BootstrapMavenOptions;
import io.smallrye.common.os.OS;
import io.smallrye.common.process.ProcessBuilder;

public final class WrapperRunner {

    public enum Wrapper {
        GRADLE("gradlew", "gradlew.bat", List.of("--no-daemon", "build", "--info", "--stacktrace")),
        MAVEN("mvnw", "mvnw.cmd", List.of("-B", "package"));

        private final String execUnix;
        private final String execWindows;
        private final List<String> cmdArgs;

        Wrapper(String execUnix, String execWindows, List<String> cmdArgs) {
            this.execUnix = execUnix;
            this.execWindows = execWindows;
            this.cmdArgs = cmdArgs;
        }

        public String getExec() {
            return switch (OS.current()) {
                case WINDOWS -> execWindows;
                default -> execUnix;
            };
        }

        public List<String> getCmdArgs() {
            return cmdArgs;
        }

        public static Wrapper fromBuildtool(String buildtool) {
            return switch (buildtool) {
                case "maven" -> MAVEN;
                case "gradle", "gradle-kotlin-dsl" -> GRADLE;
                default -> throw new IllegalStateException("No wrapper linked to build tool: " + buildtool);
            };
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
        Path command = projectDir.resolve(wrapper.getExec()).toAbsolutePath();
        List<String> args = new ArrayList<>(wrapper.getCmdArgs().size() + 2);
        args.addAll(wrapper.getCmdArgs());

        propagateSystemPropertyIfSet("maven.repo.local", args);

        if (wrapper == Wrapper.MAVEN) {
            final String mavenSettings = getMavenSettingsArg();
            if (mavenSettings != null) {
                args.add("-s");
                args.add(mavenSettings);
            }
        }

        var holder = new Object() {
            int exitCode;
        };
        System.out.printf("Running command: %s %s%n", command, args);
        ProcessBuilder.newBuilder(command)
                .arguments(args)
                .directory(projectDir)
                .exitCodeChecker(ec -> {
                    holder.exitCode = ec;
                    return true;
                })
                .output().inherited()
                .error().inherited()
                .run();
        return holder.exitCode;
    }

    private static String getMavenSettingsArg() {
        final String mavenSettings = System.getProperty("maven.settings");
        if (mavenSettings != null) {
            return Files.exists(Paths.get(mavenSettings)) ? mavenSettings : null;
        }
        return BootstrapMavenOptions.newInstance().getOptionValue(BootstrapMavenOptions.ALTERNATE_USER_SETTINGS);
    }

    private static void propagateSystemPropertyIfSet(String name, List<String> command) {
        if (System.getProperties().containsKey(name)) {
            final StringBuilder buf = new StringBuilder();
            buf.append("-D").append(name);
            final String value = System.getProperty(name);
            if (value != null && !value.isEmpty()) {
                buf.append("=").append(value);
            }
            command.add(buf.toString());
        }
    }
}
