package org.jboss.shamrock.jpa;

import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.jboss.builder.item.MultiBuildItem;

public final class PersistenceUnitDescriptorBuildItem extends MultiBuildItem {

    private final ParsedPersistenceXmlDescriptor descriptor;

    public PersistenceUnitDescriptorBuildItem(ParsedPersistenceXmlDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ParsedPersistenceXmlDescriptor getDescriptor() {
        return descriptor;
    }
}
