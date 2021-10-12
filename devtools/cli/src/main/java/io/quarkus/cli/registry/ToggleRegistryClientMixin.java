package io.quarkus.cli.registry;

import picocli.CommandLine;

public class ToggleRegistryClientMixin extends RegistryClientMixin {

    boolean useRegistryClient = true;

    @CommandLine.Option(names = { "--registry-client" }, description = "Use the Quarkus extension catalog", negatable = true)
    void setRegistryClient(boolean enabled) {
        System.setProperty("quarkusRegistryClient", Boolean.toString(enabled));
        useRegistryClient = enabled;
    }

    @Override
    public boolean useRegistryClient() {
        return useRegistryClient;
    }
}
