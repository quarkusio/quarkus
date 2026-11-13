package io.quarkus.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import picocli.CommandLine;

@EnabledIfSystemProperty(named = "native.image.path", matches = ".+")
public class CliNativeIT {

    @Test
    public void version() throws Exception {
        Result result = run("--version");
        assertEquals(CommandLine.ExitCode.OK, result.exitCode, result.toString());
        assertTrue(result.stdout.trim().matches("[\\w.-]+"), result.toString());
    }

    @Test
    public void help() throws Exception {
        Result result = run("--help");
        assertEquals(CommandLine.ExitCode.OK, result.exitCode, result.toString());
        assertTrue(result.stdout.contains("Usage: quarkus"), result.toString());
        assertTrue(result.stdout.contains("create"), result.toString());
    }

    @Test
    public void listPlugins() throws Exception {
        Result result = run("plugin", "list");
        assertEquals(CommandLine.ExitCode.OK, result.exitCode, result.toString());
    }

    private static Result run(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(System.getProperty("native.image.path"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output = process.getInputStream().readAllBytes();
        assertTrue(process.waitFor(1, TimeUnit.MINUTES), "The native CLI did not exit within a minute");
        return new Result(process.exitValue(), new String(output, StandardCharsets.UTF_8));
    }

    record Result(int exitCode, String stdout) {
    }
}
