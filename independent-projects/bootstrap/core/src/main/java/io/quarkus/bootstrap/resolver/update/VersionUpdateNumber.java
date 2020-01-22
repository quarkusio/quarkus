package io.quarkus.bootstrap.resolver.update;

/**
 * Indicates which version number is allowed to be updated.
 *
 * @author Alexey Loubyansky
 */
public enum VersionUpdateNumber {

    MAJOR("major"),
    MINOR("minor"),
    MICRO("micro"),
    UNKNOWN(null);

    private final String name;

    static VersionUpdateNumber of(String name) {
        if (MAJOR.name.equals(name)) {
            return MAJOR;
        }
        if (MINOR.name.equals(name)) {
            return MINOR;
        }
        if (MICRO.name.equals(name)) {
            return MICRO;
        }
        return UNKNOWN;
    }

    VersionUpdateNumber(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }
}