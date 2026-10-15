package io.quarkus.micrometer.deployment;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.micrometer.runtime.MicrometerRecorder;

@SuppressWarnings("unchecked")
public final class MicrometerRegistryProviderBuildItem extends MultiBuildItem
        implements Comparable<MicrometerRegistryProviderBuildItem> {

    final Class<? extends MeterRegistry> clazz;

    public MicrometerRegistryProviderBuildItem(Class<?> providedRegistryClass) {
        this.clazz = (Class<? extends MeterRegistry>) providedRegistryClass;
    }

    public MicrometerRegistryProviderBuildItem(String registryClassName) {
        this.clazz = (Class<? extends MeterRegistry>) MicrometerRecorder.getClassForName(registryClassName);
    }

    public Class<? extends MeterRegistry> getRegistryClass() {
        return clazz;
    }

    @Override
    public String toString() {
        return "MicrometerRegistryProviderBuildItem{"
                + clazz
                + '}';
    }

    @Override
    public int compareTo(MicrometerRegistryProviderBuildItem o) {
        return this.clazz.getName().compareTo(o.clazz.getName());
    }
}
