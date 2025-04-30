package io.quarkus.security.spi;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Registers default SecurityCheck with the SecurityCheckStorage.
 * Please make sure this build item is produced exactly once or validation will fail and exception will be thrown.
 */
public final class DefaultSecurityCheckBuildItem
        // we make this Multi to run CapabilityAggregationStep#aggregateCapabilities first
        // so that user-friendly error message is logged when Quarkus REST and RESTEasy are used together
        extends MultiBuildItem {

    public final List<String> rolesAllowed;

    private DefaultSecurityCheckBuildItem(List<String> rolesAllowed) {
        this.rolesAllowed = rolesAllowed;
    }

    public static DefaultSecurityCheckBuildItem denyAll() {
        return new DefaultSecurityCheckBuildItem(null);
    }

    public static DefaultSecurityCheckBuildItem rolesAllowed(List<String> rolesAllowed) {
        Objects.requireNonNull(rolesAllowed);
        return new DefaultSecurityCheckBuildItem(List.copyOf(rolesAllowed));
    }

    public List<String> getRolesAllowed() {
        return rolesAllowed;
    }
}
