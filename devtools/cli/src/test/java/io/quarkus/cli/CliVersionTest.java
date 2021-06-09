package io.quarkus.cli;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CliVersionTest {
    @Test
    public void testCommandVersion() throws Exception {
        CliDriver.Result result = CliDriver.execute("version");
        result.echoSystemOut();

        CliDriver.Result result2 = CliDriver.execute("-v");
        Assertions.assertEquals(result.stdout, result2.stdout, "Version output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");

        CliDriver.Result result3 = CliDriver.execute("--version");
        Assertions.assertEquals(result.stdout, result3.stdout, "Version output for command aliases should be the same.");
        CliDriver.println("-- same as above\n\n");
    }
}
