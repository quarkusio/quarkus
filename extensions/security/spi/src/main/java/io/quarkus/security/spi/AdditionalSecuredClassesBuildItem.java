package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Collections;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains classes that need to have @DenyAll on all methods that don't have security annotations
 */
public final class AdditionalSecuredClassesBuildItem extends MultiBuildItem {
    public final Collection<ClassInfo> additionalSecuredClasses;

    public AdditionalSecuredClassesBuildItem(Collection<ClassInfo> additionalSecuredClasses) {
        this.additionalSecuredClasses = Collections.unmodifiableCollection(additionalSecuredClasses);
    }
}
