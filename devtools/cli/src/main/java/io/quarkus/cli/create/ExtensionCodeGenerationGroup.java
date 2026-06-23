package io.quarkus.cli.create;

import java.util.Optional;

import io.quarkus.quickcli.annotations.Option;

public class ExtensionCodeGenerationGroup {
    @Option(names = { "-C",
            "--codestart" }, description = "Generate extension codestart", negatable = true)
    boolean codestart = false;

    @Option(names = { "--no-unit-test" }, description = "Generate unit tests", negatable = true)
    boolean unitTest = true;

    @Option(names = {
            "--no-it-test" }, description = "Generate integration test", negatable = true)
    boolean integrationTests = true;

    @Option(names = {
            "--no-devmode-test" }, description = "Generate dev mode tests", negatable = true)
    boolean devModeTest = true;

    @Option(names = {
            "--without-tests" }, description = "Do not generate any tests (disable all)")
    Optional<Boolean> withoutTests;

    public boolean withCodestart() {
        return codestart;
    }

    public boolean skipUnitTest() {
        return withoutTests.orElse(!unitTest);
    }

    public boolean skipIntegrationTests() {
        return withoutTests.orElse(!integrationTests);
    }

    public boolean skipDevModeTest() {
        return withoutTests.orElse(!devModeTest);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExtensionCodeGenerationGroup{");
        sb.append("codestart=").append(codestart);
        sb.append(", unitTest=").append(unitTest);
        sb.append(", integrationTests=").append(integrationTests);
        sb.append(", devModeTest=").append(devModeTest);
        sb.append(", withoutTests=").append(withoutTests);
        sb.append('}');
        return sb.toString();
    }
}
