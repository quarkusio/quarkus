package io.quarkus.hibernate.orm.deployment;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import io.quarkus.builder.item.MultiBuildItem;

public final class PersistenceXmlDescriptorBuildItem extends MultiBuildItem {

    private final ParsedPersistenceXmlDescriptor descriptor;

    public PersistenceXmlDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    protected ParsedPersistenceXmlDescriptor getDescriptor() {
        return descriptor;
    }
}
