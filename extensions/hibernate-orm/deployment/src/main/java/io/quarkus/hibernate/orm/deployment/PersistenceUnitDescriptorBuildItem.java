package io.quarkus.hibernate.orm.deployment;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Not to be confused with PersistenceXmlDescriptorBuildItem, which holds
 * items of the same type.
 * This build item represents a later phase, and might include the implicit
 * configuration definitions that are automatically defined by Quarkus.
 */
public final class PersistenceUnitDescriptorBuildItem extends MultiBuildItem {

    private final ParsedPersistenceXmlDescriptor descriptor;

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ParsedPersistenceXmlDescriptor getDescriptor() {
        return descriptor;
    }
}
