package org.jboss.shamrock.deployment;

import java.util.ArrayList;
import java.util.List;

class SetupContextImpl implements SetupContext {

    final List<ResourceProcessor> resourceProcessors = new ArrayList<>();
    final List<InjectionProvider> injectionProviders = new ArrayList<>();
    final List<String> applicationArchiveMarkers = new ArrayList<>();

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
}
