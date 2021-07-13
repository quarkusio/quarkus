package io.quarkus.cli.common;

import picocli.CommandLine;

public class ToggleRegistryClientMixin extends RegistryClientMixin {

    @CommandLine.Option(names = { "--registry-client" }, description = "Use the Quarkus extension catalog", negatable = true)
    boolean useRegistryClient = false;

    @Override
    public boolean enabled() {
        return useRegistryClient;
    }
}
