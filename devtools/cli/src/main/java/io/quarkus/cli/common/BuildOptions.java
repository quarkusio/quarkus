package io.quarkus.cli.common;

import java.util.HashMap;
import java.util.Map;

import picocli.CommandLine;

public class BuildOptions {
    public Map<String, String> properties = new HashMap<>();

    @CommandLine.Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    @CommandLine.Option(order = 4, names = { "--native" }, description = "Build native executable.", defaultValue = "false")
    public boolean buildNative = false;

    @CommandLine.Option(order = 5, names = { "--offline" }, description = "Work offline.", defaultValue = "false")
    public boolean offline = false;

    @CommandLine.Option(order = 6, names = { "--tests" }, description = "Run tests.", negatable = true)
    public boolean runTests = true;

    @CommandLine.Option(order = 7, names = "-D", mapFallbackValue = "", description = "Additional Java properties.")
    void setProperty(Map<String, String> props) {
        this.properties = props;
    }

    public boolean skipTests() {
        return !runTests;
    }
}
