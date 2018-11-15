package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.SimpleBuildItem;
import org.jboss.shamrock.runtime.InjectionFactory;

/**
 * Symbolic build item that is produced when injection provider registration is complete
 */
public final class InjectionFactoryBuildItem extends SimpleBuildItem {

    private final InjectionFactory factory;

    public InjectionFactoryBuildItem(InjectionFactory factory) {
        this.factory = factory;
    }

    public InjectionFactory getFactory() {
        return factory;
    }
}
