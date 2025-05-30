package io.quarkus.maven.capabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CapabilitiesConfig {

private List<CapabilityConfig> provides = new ArrayList<>(0);
private List<CapabilityConfig> requires = new ArrayList<>(0);

public void addProvides(CapabilityConfig capability) {
provides.add(capability);
}

public void addProvidesIf(CapabilityConfig capability) {
provides.add(capability);
}

public Collection<CapabilityConfig> getProvides() {
return provides;
}

public void addRequires(CapabilityConfig capability) {
requires.add(capability);
}

public void addRequiresIf(CapabilityConfig capability) {
requires.add(capability);
}

public Collection<CapabilityConfig> getRequires() {
return requires;
}
}
