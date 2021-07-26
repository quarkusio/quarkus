package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains classes that need to have @DenyAll on all methods that don't have security annotations
 */
public final class AdditionalSecuredClassesBuildItem extends MultiBuildItem {
    public final Collection<ClassInfo> additionalSecuredClasses;
    /**
     * The roles alloe
     */
    public final Optional<List<String>> rolesAllowed;

    public AdditionalSecuredClassesBuildItem(Collection<ClassInfo> additionalSecuredClasses) {
        this.additionalSecuredClasses = Collections.unmodifiableCollection(additionalSecuredClasses);
        rolesAllowed = Optional.empty();
    }

    public AdditionalSecuredClassesBuildItem(Collection<ClassInfo> additionalSecuredClasses,
            Optional<List<String>> rolesAllowed) {
        this.additionalSecuredClasses = Collections.unmodifiableCollection(additionalSecuredClasses);
        this.rolesAllowed = rolesAllowed;
    }
}
