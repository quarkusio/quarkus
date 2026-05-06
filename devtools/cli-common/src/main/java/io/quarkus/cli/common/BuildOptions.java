package io.quarkus.cli.common;

import io.quarkus.quickcli.annotations.Option;

public class BuildOptions {
    @Option(order = 3, names = {
            "--clean" }, description = "Perform clean as part of build. False by default.", negatable = true)
    public boolean clean = false;

    @Option(order = 4, names = { "--native" }, description = "Build native executable.", defaultValue = "false")
    public boolean buildNative = false;

    @Option(order = 5, names = { "--offline" }, description = "Work offline.", defaultValue = "false")
    public boolean offline = false;

    @Option(order = 6, names = {
            "--no-tests" }, description = "Run tests.", negatable = true, defaultValue = "false")
    public boolean skipTests = false;

    @Option(order = 7, names = {
            "--report" }, description = "Generate build report.", negatable = true, defaultValue = "false")
    public boolean generateBuildReport = false;

    @Option(order = 8, names = {
            "--ext-capture" }, description = "Enable extended capture for build metrics.", negatable = true, defaultValue = "false")
    public boolean buildExtendedCapture = false;

    public boolean skipTests() {
        return skipTests;
    }

    @Override
    public String toString() {
        return "BuildOptions [buildNative=" + buildNative + ", clean=" + clean + ", offline=" + offline + ", skipTests="
                + skipTests + ", generateBuildReport=" + generateBuildReport + ", buildExtendedCapture="
                + buildExtendedCapture + "]";
    }
}
