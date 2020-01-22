package io.quarkus.bootstrap.resolver.update;

/**
 * Indicates what should be used as the source of application dependencies.
 *
 * @author Alexey Loubyansky
 */
public enum DependenciesOrigin {

    APPLICATION("application"),
    LAST_UPDATE("last-update"),
    UNKNOWN(null);

    private final String name;

    static DependenciesOrigin of(String name) {
        if (APPLICATION.name.equals(name)) {
            return APPLICATION;
        }
        if (LAST_UPDATE.name.equals(name)) {
            return LAST_UPDATE;
        }
        return UNKNOWN;
    }

    DependenciesOrigin(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}