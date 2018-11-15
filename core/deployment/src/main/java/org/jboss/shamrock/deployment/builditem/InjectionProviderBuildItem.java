package org.jboss.shamrock.deployment.builditem;

import org.jboss.builder.item.MultiBuildItem;
import org.jboss.shamrock.runtime.InjectionFactory;

/**
 * Symbolic build item that represents the registration of an injection provider
 */
public final class InjectionProviderBuildItem extends MultiBuildItem {

    private final InjectionFactory factory;

    public InjectionProviderBuildItem(InjectionFactory factory) {
        this.factory = factory;
    }

    public InjectionFactory getFactory() {
        return factory;
    }

    @Override
    public String toString() {
        return "InjectionProviderBuildItem{" +
                "factory=" + factory +
                '}';
    }
}
