package io.quarkus.cli.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.cli.CliDriver;

@DisabledOnOs(value = OS.WINDOWS, disabledReason = "Parsing the stdout is not working on Github Windows, maybe because of the console formatting. I did try it in a Windows box and it works fine.")
public class DecryptTest {
    @TempDir
    Path tempDir;

    @Test
    void decryptPlain() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "decrypt",
                "DPZqAC4GZNAXi6_43A4O2SBmaQssGkq6PS7rz8tzHDt1", "somearbitrarycrazystringthatdoesnotmatter", "-f=plain");
        Scanner scanner = new Scanner(result.getStdout());
        String[] split = scanner.nextLine().split(" ");
        String secret = split[split.length - 1];
        assertEquals("1234", secret);
    }

    @Test
    void decryptBase64() throws Exception {
        CliDriver.Result result = CliDriver.execute(tempDir, "config", "decrypt",
                "DJNrZ6LfpupFv6QbXyXhvzD8eVDnDa_kTliQBpuzTobDZxlg", "c29tZWFyYml0cmFyeWNyYXp5c3RyaW5ndGhhdGRvZXNub3RtYXR0ZXI");
        Scanner scanner = new Scanner(result.getStdout());
        String[] split = scanner.nextLine().split(" ");
        String secret = split[split.length - 1];
        assertEquals("decoded", secret);
    }
}
