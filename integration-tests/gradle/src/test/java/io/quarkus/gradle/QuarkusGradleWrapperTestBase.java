package io.quarkus.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class QuarkusGradleWrapperTestBase extends QuarkusGradleTestBase {

    private static final String GRADLE_WRAPPER_WINDOWS = "gradlew.bat";
    private static final String GRADLE_WRAPPER_UNIX = "./gradlew";
    private static final String GRADLE_NO_DAEMON = "--no-daemon";
    private static final String MAVEN_REPO_LOCAL = "maven.repo.local";

    private Map<String, String> systemProps;

    protected void setupTestCommand() {

    }

    public BuildResult runGradleWrapper(File projectDir, String... args) throws IOException, InterruptedException {
        setupTestCommand();
        List<String> command = new LinkedList<>();
        command.add(getGradleWrapperCommand());
        command.add(GRADLE_NO_DAEMON);
        command.addAll(getSytemProperties());
        command.add("--stacktrace");
        command.addAll(Arrays.asList(args));

        File logOutput = new File(projectDir, "command-output.log");

        Process p = new ProcessBuilder()
                .directory(projectDir)
                .command(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(logOutput)
                .redirectOutput(logOutput)
                .redirectError(logOutput)
                .start();

        p.waitFor(5, TimeUnit.MINUTES);
        try (InputStream is = new FileInputStream(logOutput)) {
            final BuildResult commandResult = BuildResult.of(is);
            if (p.exitValue() != 0) {
                printCommandOutput(command, commandResult);
            }
            return commandResult;
        }
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
        if (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).indexOf("windows") >= 0) {
            return GRADLE_WRAPPER_WINDOWS;
        }
        return GRADLE_WRAPPER_UNIX;
    }

    private List<String> getSytemProperties() {
        List<String> args = null;
        if (systemProps != null) {
            args = new ArrayList<>(systemProps.size() + 1);
            for (Map.Entry<String, String> prop : systemProps.entrySet()) {
                args.add(toPropertyArg(prop.getKey(), prop.getValue()));
            }
        }
        final String mavenRepoLocal = System.getProperty(MAVEN_REPO_LOCAL);
        if (mavenRepoLocal != null) {
            final String arg = toPropertyArg(MAVEN_REPO_LOCAL, mavenRepoLocal);
            if (args == null) {
                args = Collections.singletonList(arg);
            } else {
                args.add(arg);
            }
        }
        return args == null ? Collections.emptyList() : args;
    }

    private static String toPropertyArg(String name, String value) {
        return new StringBuilder().append("-D=").append(name).append("=").append(value).toString();
    }

    private void printCommandOutput(List<String> command, BuildResult commandResult) {
        System.err.println("Command: " + String.join(" ", command) + " failed with the following output:");
        System.err.println(commandResult.getOutput());
    }
}
