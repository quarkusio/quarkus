package io.quarkus.builder.location;

import java.util.Objects;

public abstract class Location {
    private final Location parent;

    Location(final Location parent) {
        this.parent = parent;
    }

    public Location getParent() {
        return parent;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Location l && Objects.equals(parent, l.getParent());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parent);
    }
}
