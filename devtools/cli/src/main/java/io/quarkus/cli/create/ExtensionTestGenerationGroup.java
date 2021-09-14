package io.quarkus.cli.create;

import picocli.CommandLine;

public class ExtensionTestGenerationGroup {
    @CommandLine.Option(names = { "--unit-test" }, description = "Generate unit tests", negatable = true)
    boolean unitTest;

    @CommandLine.Option(names = { "--it-tests" }, description = "Generate integration tests", negatable = true)
    boolean integrationTests;

    @CommandLine.Option(names = { "--devmode-test" }, description = "Generate dev mode tests", negatable = true)
    boolean devModeTest;

    @CommandLine.Option(names = { "--without-tests" }, description = "Do not generate any tests (disable all)")
    boolean withoutTests;

    @Override
    public String toString() {
        return "ExtensionTestGenerationGroup [devModeTest=" + devModeTest + ", integrationTests=" + integrationTests
                + ", unitTest=" + unitTest + ", withoutTests=" + withoutTests + "]";
    }
}
