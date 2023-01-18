package io.quarkus.security.spi;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.jandex.MethodInfo;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Contains methods that need to have {@link jakarta.annotation.security.DenyAll} or
 * {@link jakarta.annotation.security.RolesAllowed}.
 */
public final class AdditionalSecuredMethodsBuildItem extends MultiBuildItem {

    public final Collection<MethodInfo> additionalSecuredMethods;
    public final Optional<List<String>> rolesAllowed;

    public AdditionalSecuredMethodsBuildItem(Collection<MethodInfo> additionalSecuredMethods) {
        this.additionalSecuredMethods = Collections.unmodifiableCollection(additionalSecuredMethods);
        rolesAllowed = Optional.empty();
    }

    public AdditionalSecuredMethodsBuildItem(Collection<MethodInfo> additionalSecuredMethods,
            Optional<List<String>> rolesAllowed) {
        this.additionalSecuredMethods = Collections.unmodifiableCollection(additionalSecuredMethods);
        this.rolesAllowed = rolesAllowed;
    }
}
