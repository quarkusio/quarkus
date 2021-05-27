package io.quarkus.cli.common;

import java.util.HashMap;
import java.util.Map;

import picocli.CommandLine;

public class DevOptions {
    public Map<String, String> properties = new HashMap<>();

    @CommandLine.Option(order = 2, names = { "--dryrun" }, description = "Show actions that would be taken.")
    public boolean dryRun = false;

    @CommandLine.Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    @CommandLine.Option(order = 4, names = {
            "--no-tests" }, description = "Toggle continuous testing mode. Enabled by default.", negatable = true, hidden = true)
    public boolean runTests = true; // TODO: does this make sense re: continuous test?

    @CommandLine.Option(order = 5, names = "-D", mapFallbackValue = "", description = "Java properties")
    void setProperty(Map<String, String> props) {
        this.properties = props;
    }

    public boolean skipTests() {
        return !runTests;
    }

    @Override
    public String toString() {
        return "DevOptions [clean=" + clean + ", properties=" + properties + ", tests=" + runTests
                + "]";
    }
}
