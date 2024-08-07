package io.quarkus.micrometer.deployment;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.builder.item.MultiBuildItem;

@SuppressWarnings("unchecked")
public final class MicrometerRegistryProviderBuildItem extends MultiBuildItem {

    final String registryClassName;

    @Deprecated(forRemoval = true)
    public MicrometerRegistryProviderBuildItem(Class<?> providedRegistryClass) {
        this.registryClassName = providedRegistryClass.getName();
    }

    public MicrometerRegistryProviderBuildItem(String registryClassName) {
        this.registryClassName = registryClassName;
    }

    public Class<? extends MeterRegistry> getRegistryClass() {
        try {
            return (Class<? extends MeterRegistry>) Class.forName(registryClassName, false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "MicrometerRegistryProviderBuildItem{"
                + registryClassName
                + '}';
    }
}
