package io.quarkus.extension.gradle.dsl;

import java.util.ArrayList;
import java.util.List;

public class Capabilities {

    private List<Capability> provided = new ArrayList<>(0);
    private List<Capability> required = new ArrayList<>(0);

    public Capability provides(String name) {
        Capability capability = new Capability(name);
        provided.add(capability);
        return capability;
    }

    public Capability requires(String name) {
        Capability capability = new Capability(name);
        required.add(capability);
        return capability;
    }

    public List<Capability> getProvidedCapabilities() {
        return provided;
    }

    public List<Capability> getRequiredCapabilities() {
        return required;
    }
}
