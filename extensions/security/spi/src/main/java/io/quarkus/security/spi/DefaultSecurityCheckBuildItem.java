package io.quarkus.security.spi;

import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.SimpleBuildItem;

public final class DefaultSecurityCheckBuildItem extends SimpleBuildItem {

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
