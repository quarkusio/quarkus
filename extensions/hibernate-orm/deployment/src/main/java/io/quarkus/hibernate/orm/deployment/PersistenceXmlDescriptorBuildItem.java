package io.quarkus.hibernate.orm.deployment;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;

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

    private final ParsedPersistenceXmlDescriptor descriptor;

    public PersistenceXmlDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ParsedPersistenceXmlDescriptor getDescriptor() {
        return descriptor;
    }
}
