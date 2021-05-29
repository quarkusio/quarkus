package io.quarkus.micrometer.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class VetoDefaultMeterRegistryBuildItem extends MultiBuildItem {
    final DotName meterRegistryProducerClassName;

    public VetoDefaultMeterRegistryBuildItem(Class<?> meterRegistryProducerClass) {
        this(DotName.createSimple(meterRegistryProducerClass.getName()));
    }

    public VetoDefaultMeterRegistryBuildItem(DotName meterRegistryProducerClass) {
        this.meterRegistryProducerClassName = meterRegistryProducerClass;
    }

    @Override
    public String toString() {
        return "VetoDefaultMeterRegistryBuildItem"
                + "{meterRegistryProducerClassName=" + meterRegistryProducerClassName
                + '}';
    }
}
