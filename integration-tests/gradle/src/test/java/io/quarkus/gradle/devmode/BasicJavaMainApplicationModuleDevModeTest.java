package io.quarkus.gradle.devmode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.BufferedReader;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BasicJavaMainApplicationModuleDevModeTest extends QuarkusDevGradleTestBase {
    private static final String ARGS_LOG_LINE_PREFIX = "basic-java-main-application-project: args";

    @Override
    protected String projectDirectoryName() {
        return "basic-java-main-application-project";
    }

    @Override
    protected String[] buildArguments() {
        return new String[] { "clean", "quarkusDev", "-s", "-Dquarkus.args=param1=1 param2=2" };
    }

    protected void testDevMode() throws Exception {
        File logOutput = new File(projectDir, "command-output.log");
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            try {
                String logLine;
                RandomAccessFile commandOutputLogFile = new RandomAccessFile(logOutput, "r");
                while ((logLine = commandOutputLogFile.readLine()) != null) {
                    if (logLine.startsWith(ARGS_LOG_LINE_PREFIX)) {
                        commandOutputLogFile.close();
                        return true;
                    }
                }
                commandOutputLogFile.close();
                return false;
            } catch (Exception e) {
                System.out.println(String.format("e: <message, %s>, <cause, %s>", e.getMessage(), e.getCause()));
                System.out.println("e: " + Arrays.toString(e.getStackTrace()));
                return false;
            }
        });
        String argsLengthLogLine = null;
        String argsLogLine = null;
        if (logOutput.exists()) {
            try (BufferedReader reader = Files.newBufferedReader(logOutput.toPath())) {
                String line = reader.readLine();
                while (line != null) {
                    if (line.startsWith(ARGS_LOG_LINE_PREFIX)) {
                        if (line.contains("length")) {
                            argsLengthLogLine = line;
                        } else {
                            argsLogLine = line;
                        }
                    }
                    line = reader.readLine();
                }
            }
        }
        assertThat(argsLengthLogLine).isNotNull();
        assertThat(argsLengthLogLine).isNotEmpty();
        assertThat(argsLogLine).isNotNull();
        assertThat(argsLogLine).isNotEmpty();
        assertThat(argsLengthLogLine).contains("basic-java-main-application-project: args.length: 2");
        assertThat(argsLogLine).contains("basic-java-main-application-project: args: [param1=1, param2=2]");
    }
}
