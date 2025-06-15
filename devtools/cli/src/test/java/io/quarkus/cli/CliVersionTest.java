package io.quarkus.cli;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CliVersionTest {
    static Path workspaceRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            .resolve("target/test-project");

    @Test
    public void testCommandVersion() throws Exception {

        CliDriver.Result result = CliDriver.execute(workspaceRoot, "-v");
        result.echoSystemOut();
        Assertions.assertEquals(result.stdout.trim(), System.getProperty("project.version"),
                "Version output for command aliases should be the same.");

        CliDriver.Result result2 = CliDriver.execute(workspaceRoot, "--version");
        Assertions.assertEquals(result.stdout, result2.stdout,
                "Version output for command aliases should be the same.");
    }
}
