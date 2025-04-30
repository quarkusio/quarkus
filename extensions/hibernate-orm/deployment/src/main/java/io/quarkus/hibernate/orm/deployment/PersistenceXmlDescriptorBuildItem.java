package io.quarkus.hibernate.orm.deployment;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Provides instances of {@see ParsedPersistenceXmlDescriptor}, the raw representation
 * of a persistence.xml file as it is after being located and parsed.
 * Exposed as a possible integration API: other extensions can produce additional
 * configuration instances.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class PersistenceXmlDescriptorBuildItem extends MultiBuildItem {

    private final PersistenceUnitDescriptor descriptor;

    public PersistenceXmlDescriptorBuildItem(PersistenceUnitDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public PersistenceUnitDescriptor getDescriptor() {
        return descriptor;
    }
}
