package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class Capabilities {

    private List<Capability> capabilities = new ArrayList<>(0);

    public Capability capability(String name) {
        Capability capability = new Capability(name);
        capabilities.add(capability);
        return capability;
    }

    public List<Capability> getCapabilities() {
        return capabilities;
    }
}
