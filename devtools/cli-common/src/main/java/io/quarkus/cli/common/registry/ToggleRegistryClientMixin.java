package io.quarkus.cli.common.registry;

import io.quarkus.quickcli.annotations.Option;

public class ToggleRegistryClientMixin extends RegistryClientMixin {

    boolean useRegistryClient = true;

    @Option(names = { "--registry-client" }, description = "Use the Quarkus extension catalog", negatable = true)
    public void setRegistryClient(boolean enabled) {
        System.setProperty("quarkusRegistryClient", Boolean.toString(enabled));
        useRegistryClient = enabled;
    }

    @Override
    public boolean enabled() {
        return useRegistryClient;
    }
}
