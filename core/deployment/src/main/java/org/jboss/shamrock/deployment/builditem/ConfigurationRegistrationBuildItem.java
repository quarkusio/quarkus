package org.jboss.shamrock.deployment.builditem;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import org.jboss.builder.item.MultiBuildItem;

/**
 */
public final class ConfigurationRegistrationBuildItem extends MultiBuildItem {
    private final Type type;
    private final String baseKey;
    private final AnnotatedElement injectionSite;

    public ConfigurationRegistrationBuildItem(final Type type, final String baseKey, final AnnotatedElement injectionSite) {
        this.type = type;
        this.baseKey = baseKey;
        this.injectionSite = injectionSite;
    }

    public Type getType() {
        return type;
    }

    public String getBaseKey() {
        return baseKey;
    }

    public AnnotatedElement getInjectionSite() {
        return injectionSite;
    }
}
