package io.quarkus.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class QuarkusGradleWrapperTestBase extends QuarkusGradleTestBase {

    private static final String GRADLE_WRAPPER_WINDOWS = "gradlew.bat";
    private static final String GRADLE_WRAPPER_UNIX = "./gradlew";
    private static final String GRADLE_NO_DAEMON = "--no-daemon";

    public BuildResult runGradleWrapper(File projectDir, String... args) throws IOException, InterruptedException {
        List<String> command = new LinkedList<>();
        command.add(getGradleWrapperCommand());
        command.add(GRADLE_NO_DAEMON);
        command.add("--stacktrace");
        command.addAll(Arrays.asList(args));

        File logOutput = new File(projectDir, "command-output.log");

        Process p = new ProcessBuilder()
                .directory(projectDir)
                .command(command)
                .redirectInput(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(logOutput)
                .redirectError(logOutput)
                .start();

        p.waitFor(5, TimeUnit.MINUTES);

        try (InputStream is = new FileInputStream(logOutput)) {
            return BuildResult.of(is);
        }
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
}
