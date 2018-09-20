package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class SetupContextImpl implements SetupContext {

    final List<ResourceProcessor> resourceProcessors = new ArrayList<>();
    final List<InjectionProvider> injectionProviders = new ArrayList<>();
    final List<String> applicationArchiveMarkers = new ArrayList<>();
    final Set<String> capabilities = new HashSet<>();

    @Override
    public void addResourceProcessor(ResourceProcessor resourceProcessor) {
        resourceProcessors.add(resourceProcessor);
    }

    @Override
    public void addInjectionProvider(InjectionProvider injectionProvider) {
        injectionProviders.add(injectionProvider);
    }

    @Override
    public void addApplicationArchiveMarker(String file) {
        applicationArchiveMarkers.add(file);
    }

    public void addCapability(String name) {
        capabilities.add(name);
    }
}
