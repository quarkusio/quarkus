package io.quarkus.bootstrap.resolver.update;

/**
 * Indicates which update policy should be applied.
 *
 * @author Alexey Loubyansky
 */
public enum VersionUpdate {

    LATEST("latest"),
    NEXT("next"),
    NONE("none"),
    UNKNOWN(null);

    private final String name;

    static VersionUpdate of(String name) {
        if (LATEST.name.equals(name)) {
            return LATEST;
        }
        if (NEXT.name.equals(name)) {
            return NEXT;
        }
        if (NONE.name.equals(name)) {
            return NONE;
        }
        return UNKNOWN;
    }

    VersionUpdate(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}