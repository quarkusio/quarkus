package io.quarkus.devtools.project.update.rewrite;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class QuarkusUpdateCommandTest {

    /**
     * The local repository configured for the current build must reach the child Maven process, not only the log.
     */
    @Test
    public void localRepositoryIsPropagatedToTheChildProcess(@TempDir Path tempDir) throws Exception {
        Path localRepo = tempDir.resolve("repo");
        Path logFile = tempDir.resolve("update.log");
        String java = ProcessHandle.current().info().command().orElseThrow();
        List<String> command = List.of(java, "-cp", System.getProperty("java.class.path"), EchoArgs.class.getName());
        String previous = System.getProperty("maven.repo.local");
        System.setProperty("maven.repo.local", localRepo.toString());
        try {
            QuarkusUpdateCommand.execute("echo", command, MessageWriter.info(), logFile, "");
        } finally {
            if (previous == null) {
                System.clearProperty("maven.repo.local");
            } else {
                System.setProperty("maven.repo.local", previous);
            }
        }
        List<String> lines = Files.readAllLines(logFile);
        assertThat(lines).anyMatch(line -> line.startsWith("Running: ") && line.contains("-Dmaven.repo.local=" + localRepo));
        assertThat(lines).describedAs("the arguments received by the child process")
                .contains("args: -Dmaven.repo.local=" + localRepo);
    }

    public static class EchoArgs {
        public static void main(String[] args) {
            System.out.println("args: " + String.join(" ", args));
        }
    }
}
